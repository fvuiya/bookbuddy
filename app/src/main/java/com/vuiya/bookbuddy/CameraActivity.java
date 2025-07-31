package com.vuiya.bookbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "BookBuddy_Camera";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    private TextureView textureView;
    private ImageButton btnBack;
    private Spinner spinnerLanguage;
    private FloatingActionButton fabCapture;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private Size previewSize;
    private Size captureSize; // Size for ImageReader
    private ImageReader imageReader;
    private TextRecognizer textRecognizer;

    // A Semaphore to prevent the app from exiting before closing the camera.
    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    // Orientation constants
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initViews();
        setupClickListeners();

        // Initialize ML Kit Text Recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startBackgroundThread();
        }
    }

    private void initViews() {
        textureView = findViewById(R.id.textureView);
        btnBack = findViewById(R.id.btn_back);
        spinnerLanguage = findViewById(R.id.spinner_language);
        fabCapture = findViewById(R.id.fab_capture);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        fabCapture.setOnClickListener(v -> takePicture());
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "SurfaceTexture available: " + width + "x" + height);
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "SurfaceTexture size changed: " + width + "x" + height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Log.d(TAG, "SurfaceTexture destroyed");
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // Invoked every time there's an update on the texture
        }
    };

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping background thread", e);
            }
        }
    }

    private void openCamera(int width, int height) {
        // Acquire the semaphore to ensure the camera is closed before opening a new one.
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }

        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0]; // Use default camera (usually back)

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                Log.e(TAG, "StreamConfigurationMap is null");
                Toast.makeText(this, "Camera configuration not supported", Toast.LENGTH_SHORT).show();
                cameraOpenCloseLock.release();
                return;
            }

            // Choose preview size that matches aspect ratio of TextureView
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, false);
            Log.d(TAG, "Chosen preview size: " + previewSize.getWidth() + "x" + previewSize.getHeight());

            // Choose capture size (largest available JPEG size)
            captureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            Log.d(TAG, "Chosen capture size: " + captureSize.getWidth() + "x" + captureSize.getHeight());

            // Configure the transform for the TextureView
            configureTransform(width, height);

            // Check if we have permission to use the camera
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted");
                cameraOpenCloseLock.release();
                return;
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
            Toast.makeText(this, "Cannot access the camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            cameraOpenCloseLock.release();
        } catch (Exception e) {
            Log.e(TAG, "General exception in openCamera", e);
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            cameraOpenCloseLock.release();
        }
    }

    /**
     * Chooses the optimal size based on the aspect ratio of the TextureView.
     * @param choices The list of available sizes
     * @param width The width of the TextureView
     * @param height The height of the TextureView
     * @param forCapture If true, chooses the largest size; if false, chooses a size for preview.
     * @return The optimal size
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, boolean forCapture) {
        if (forCapture) {
            // For capture, just return the largest size
            return Collections.max(Arrays.asList(choices), new CompareSizesByArea());
        }

        // For preview, find a size that fits the TextureView aspect ratio
        float textureViewRatio = (float) width / height;
        Log.d(TAG, "TextureView ratio: " + textureViewRatio);

        List<Size> goodEnough = new ArrayList<>();
        for (Size option : choices) {
            // Skip sizes larger than 1920x1080 for preview to save resources
            if (option.getWidth() > 1920 || option.getHeight() > 1080) {
                continue;
            }

            float optionRatio = (float) option.getWidth() / option.getHeight();
            // Check if the aspect ratio is close enough (within 2%)
            if (Math.abs(optionRatio - textureViewRatio) < 0.02) {
                goodEnough.add(option);
            }
        }

        if (!goodEnough.isEmpty()) {
            // Pick the smallest of those that fit the aspect ratio
            return Collections.min(goodEnough, new CompareSizesByArea());
        } else {
            Log.w(TAG, "Preview size: No suitable size found matching aspect ratio, falling back to max 1080p");
            // Fallback: find the largest size that's <= 1080p
            List<Size> smallEnough = new ArrayList<>();
            for (Size option : choices) {
                if (option.getWidth() <= 1920 && option.getHeight() <= 1080) {
                    smallEnough.add(option);
                }
            }
            if (!smallEnough.isEmpty()) {
                return Collections.max(smallEnough, new CompareSizesByArea());
            } else {
                // Last resort: first available size
                return choices[0];
            }
        }
    }


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice opened");
            cameraDevice = camera;
            createCameraPreviewSession();
            cameraOpenCloseLock.release(); // Release the semaphore after opening
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice disconnected");
            cameraOpenCloseLock.release(); // Release the semaphore
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice error: " + error);
            cameraOpenCloseLock.release(); // Release the semaphore
            cameraDevice.close();
            cameraDevice = null;
            Toast.makeText(CameraActivity.this, "Camera error: " + error, Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                Log.e(TAG, "Texture is null");
                return;
            }

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // Create an ImageReader for capturing images
            imageReader = ImageReader.newInstance(captureSize.getWidth(), captureSize.getHeight(), ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

            // We set up a CaptureRequest.Builder with the output Surface for preview.
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface); // Add preview surface

            // Here, we create a CameraCaptureSession for camera preview.
            // We only add the preview surface here.
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), // Add both surfaces
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            // The camera is already closed
                            if (cameraDevice == null) {
                                Log.w(TAG, "CameraDevice is null in onConfigured");
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            cameraCaptureSession = session;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                // Flash is automatically enabled when necessary.
                                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);

                                // Finally, we start displaying the camera preview.
                                CaptureRequest previewRequest = previewRequestBuilder.build();
                                cameraCaptureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
                                Log.d(TAG, "Preview session configured and started");
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Failed to access camera for preview", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Preview session configuration failed");
                            cameraOpenCloseLock.release();
                            Toast.makeText(CameraActivity.this, "Failed to configure camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, backgroundHandler
            );
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create preview session", e);
        }
    }

    private final ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "Image available from ImageReader");
            backgroundHandler.post(new ImageProcessor(reader.acquireNextImage()));
        }
    };

    private class ImageProcessor implements Runnable {
        private final Image image;

        public ImageProcessor(Image image) {
            this.image = image;
        }

        @Override
        public void run() {
            if (image == null) {
                Log.w(TAG, "ImageProcessor: Image is null");
                return;
            }

            try {
                // Convert Image to Bitmap
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                // Process with ML Kit
                InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                textRecognizer.process(inputImage)
                        .addOnSuccessListener(text -> {
                            // Extract text from ML Kit result
                            StringBuilder recognizedText = new StringBuilder();
                            for (Text.TextBlock block : text.getTextBlocks()) {
                                recognizedText.append(block.getText()).append("\n");
                            }

                            // Navigate to OCR Result Activity with the recognized text
                            Intent intent = new Intent(CameraActivity.this, OcrResultActivity.class);
                            intent.putExtra("ocr_result", recognizedText.toString());
                            startActivity(intent);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "OCR failed", e);
                            runOnUiThread(() ->
                                    Toast.makeText(CameraActivity.this, "OCR failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                        });
            } finally {
                image.close(); // Important: close the image
            }
        }
    }


    private void takePicture() {
        if (cameraDevice == null || cameraCaptureSession == null) {
            Log.w(TAG, "CameraDevice or CameraCaptureSession is null, cannot take picture");
            return;
        }

        try {
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface()); // Add ImageReader surface for capture

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            // Stop the preview before capturing
            cameraCaptureSession.stopRepeating();

            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.d(TAG, "Capture completed");
                    // Restart preview after capture is handled by ImageAvailableListener
                    // We don't restart preview here directly because processing might take time
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull CaptureFailure failure) {
                    Log.e(TAG, "Capture failed: " + failure.getReason());
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Capture failed", Toast.LENGTH_SHORT).show());
                    // Attempt to restart preview even if capture failed
                    createCameraPreviewSession();
                }
            };

            // Capture the image
            cameraCaptureSession.capture(captureBuilder.build(), captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to take picture", e);
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            // Try to restart preview even if capture failed
            createCameraPreviewSession();
        }
    }


    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * openCamera() or before the preview is started in createCameraPreviewSession().
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (previewSize == null || textureView == null) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth()); // Note: swapped width/height
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        // For ROTATION_0, no additional transform usually needed if aspect ratios match
        textureView.setTransform(matrix);
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBackgroundThread();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        try {
            // Acquire the semaphore to ensure the camera is closed before exiting.
            cameraOpenCloseLock.acquire();
            if (cameraCaptureSession != null) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while closing camera", e);
            throw new RuntimeException("Interrupted while closing camera", e);
        } finally {
            cameraOpenCloseLock.release(); // Always release the semaphore
        }
    }
}