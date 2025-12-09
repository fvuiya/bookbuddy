package com.vuiya.bookbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var btnBack: ImageButton
    private lateinit var spinnerLanguage: Spinner
    private lateinit var fabCapture: FloatingActionButton

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var previewSize: Size? = null
    private var captureSize: Size? = null
    private var imageReader: ImageReader? = null
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val cameraOpenCloseLock = Semaphore(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        initViews()
        setupClickListeners()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startBackgroundThread()
        }
    }

    private fun initViews() {
        textureView = findViewById(R.id.textureView)
        btnBack = findViewById(R.id.btn_back)
        spinnerLanguage = findViewById(R.id.spinner_language)
        fabCapture = findViewById(R.id.fab_capture)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        fabCapture.setOnClickListener { takePicture() }
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "SurfaceTexture available: ${width}x$height")
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "SurfaceTexture size changed: ${width}x$height")
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Log.d(TAG, "SurfaceTexture destroyed")
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while stopping background thread", e)
        }
    }

    private fun openCamera(width: Int, height: Int) {
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            if (map == null) {
                Log.e(TAG, "StreamConfigurationMap is null")
                Toast.makeText(this, "Camera configuration not supported", Toast.LENGTH_SHORT).show()
                cameraOpenCloseLock.release()
                return
            }

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, false)
            captureSize = map.getOutputSizes(ImageFormat.JPEG).maxWithOrNull(CompareSizesByArea())

            Log.d(TAG, "Chosen preview size: ${previewSize?.width}x${previewSize?.height}")
            Log.d(TAG, "Chosen capture size: ${captureSize?.width}x${captureSize?.height}")

            configureTransform(width, height)

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted")
                cameraOpenCloseLock.release()
                return
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception", e)
            Toast.makeText(this, "Cannot access the camera: ${e.message}", Toast.LENGTH_LONG).show()
            cameraOpenCloseLock.release()
        } catch (e: Exception) {
            Log.e(TAG, "General exception in openCamera", e)
            Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
            cameraOpenCloseLock.release()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice opened")
            cameraDevice = camera
            createCameraPreviewSession()
            cameraOpenCloseLock.release()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice disconnected")
            cameraOpenCloseLock.release()
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "CameraDevice error: $error")
            cameraOpenCloseLock.release()
            cameraDevice?.close()
            cameraDevice = null
            Toast.makeText(this@CameraActivity, "Camera error: $error", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture ?: return
            texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

            val surface = Surface(texture)

            imageReader = ImageReader.newInstance(captureSize!!.width, captureSize!!.height, ImageFormat.JPEG, 2)
            imageReader?.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                addTarget(surface)
            }

            cameraDevice?.createCaptureSession(
                listOf(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) {
                            Log.w(TAG, "CameraDevice is null in onConfigured")
                            return
                        }
                        cameraCaptureSession = session
                        try {
                            previewRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            previewRequestBuilder?.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)

                            val previewRequest = previewRequestBuilder!!.build()
                            cameraCaptureSession?.setRepeatingRequest(previewRequest, null, backgroundHandler)
                            Log.d(TAG, "Preview session configured and started")
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Failed to access camera for preview", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Preview session configuration failed")
                        cameraOpenCloseLock.release()
                        Toast.makeText(this@CameraActivity, "Failed to configure camera preview", Toast.LENGTH_SHORT).show()
                    }
                }, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create preview session", e)
        }
    }

    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        Log.d(TAG, "Image available from ImageReader")
        backgroundHandler?.post { 
            val image = reader.acquireNextImage() ?: return@post
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { text ->
                        val recognizedText = StringBuilder()
                        for (block in text.textBlocks) {
                            recognizedText.append(block.text).append("\n")
                        }
                        val intent = Intent(this@CameraActivity, OcrResultActivity::class.java).apply {
                            putExtra("ocr_result", recognizedText.toString())
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "OCR failed", e)
                        runOnUiThread {
                            Toast.makeText(this@CameraActivity, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } finally {
                image.close()
            }
        }
    }

    private fun takePicture() {
        if (cameraDevice == null || cameraCaptureSession == null) {
            Log.w(TAG, "CameraDevice or CameraCaptureSession is null, cannot take picture")
            return
        }

        try {
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader!!.surface)
                set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(windowManager.defaultDisplay.rotation))
            }

            cameraCaptureSession?.stopRepeating()

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: android.hardware.camera2.TotalCaptureResult) {
                    Log.d(TAG, "Capture completed")
                }

                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: android.hardware.camera2.CaptureFailure) {
                    Log.e(TAG, "Capture failed: ${failure.reason}")
                    runOnUiThread { Toast.makeText(this@CameraActivity, "Capture failed", Toast.LENGTH_SHORT).show() }
                    createCameraPreviewSession()
                }
            }

            cameraCaptureSession?.capture(captureBuilder.build(), captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to take picture", e)
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
            createCameraPreviewSession()
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (previewSize == null || textureView == null) {
            return
        }
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = (viewHeight.toFloat() / previewSize!!.height).coerceAtLeast(viewWidth.toFloat() / previewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBackgroundThread()
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while closing camera", e)
            throw RuntimeException("Interrupted while closing camera", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    companion object {
        private const val TAG = "BookBuddy_Camera"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        private val ORIENTATIONS = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }

        private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, forCapture: Boolean): Size {
            if (forCapture) {
                return choices.maxWithOrNull(CompareSizesByArea())!!
            }

            val textureViewRatio = width.toFloat() / height
            Log.d(TAG, "TextureView ratio: $textureViewRatio")

            val goodEnough = choices.filter { 
                val optionRatio = it.width.toFloat() / it.height
                Math.abs(optionRatio - textureViewRatio) < 0.02 && it.width <= 1920 && it.height <= 1080
            }

            if (goodEnough.isNotEmpty()) {
                return goodEnough.minWithOrNull(CompareSizesByArea())!!
            }

            Log.w(TAG, "Preview size: No suitable size found matching aspect ratio, falling back to max 1080p")
            val smallEnough = choices.filter { it.width <= 1920 && it.height <= 1080 }
            if (smallEnough.isNotEmpty()) {
                return smallEnough.maxWithOrNull(CompareSizesByArea())!!
            }

            return choices[0]
        }

        private class CompareSizesByArea : Comparator<Size> {
            override fun compare(lhs: Size, rhs: Size): Int {
                return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
        }
    }
}
