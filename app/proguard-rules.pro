# Keep PDFBox classes to prevent them from being obfuscated or removed
-keep class com.tom_roush.pdfbox.** { *; }
-keep interface com.tom_roush.pdfbox.** { *; }

# Prevent warnings about missing optional dependencies or classes
-dontwarn com.tom_roush.pdfbox.**

# If PDFBox uses any reflection that ProGuard might remove, we need to keep those classes/members
# This is a general safeguard, you might need to adjust based on runtime errors
-keepclassmembers class com.tom_roush.pdfbox.** {
    public static <fields>;
    public static <methods>;
    public <init>(...);
}

# Keep native methods if any
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes that are serialized/deserialized
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}