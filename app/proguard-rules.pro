# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ML Kit keeps an optional telemetry transport branch in its bytecode even when
# the CCT backend is deliberately excluded from the zero-collection Play build.
-dontwarn com.google.android.datatransport.cct.CCTDestination
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve annotations and signatures needed by reflection-based libraries.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Preserve RuntimeVisibleAnnotations and RuntimeInvisibleAnnotations
-keepattributes RuntimeVisible*Annotation*,RuntimeInvisible*Annotation*

# Keep generic signature for Retrofit responses
-keepattributes Signature
-keepattributes Exceptions

# Gson models rely on field names that may be accessed via reflection.
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Gson annotations
-keep class com.google.gson.annotations.** { *; }

# Keep all fields with @SerializedName annotation
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit service interfaces need their generic signatures at runtime.
-keep interface com.bookverse.contacts.data.service.** { *; }

# Keep data models and Room entities to avoid aggressive stripping.
-keep class com.bookverse.contacts.data.model.** { *; }
-keep class com.bookverse.contacts.data.db.** { *; }

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit generic type information
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# WorkManager 2.9 keeps InputMerger classes but not the constructor it calls reflectively;
# R8 full mode strips it and every worker, including Glance widget sessions, fails to start.
-keepclassmembers class * extends androidx.work.InputMerger {
    public <init>();
}

# Glance finds widget instances by class; merged GlanceAppWidget subclasses would update each other.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget

# Domain layer contains entry points referenced by WorkManager and other services.
-keep class com.bookverse.contacts.domain.** { *; }

# ML Kit Barcode Scanning (bundled).
# Evita que R8 elimine el BarcodeScannerCreator registrado via ServiceLoader,
# que causa NPE en BarcodeScanning.getClient() en builds de release.
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep interface com.google.mlkit.vision.barcode.internal.BarcodeScannerCreator { *; }
-keep class * implements com.google.mlkit.vision.barcode.internal.BarcodeScannerCreator { *; }

# ML Kit sdkinternal: protege ExecutorSelector, MlKitContext, SharedPrefManager
# y todos los sdkininternal.* que se registran como Firebase Components.
# Sin esto, R8 puede eliminar la factoría de Component<ExecutorSelector> en
# CommonComponentRegistrar.getComponents() porque no detecta la dependencia
# transitiva desde TextRegistrar (registrado en otro AAR).
-keep class com.google.mlkit.common.sdkinternal.** { *; }
-keep class com.google.mlkit.common.internal.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.vision.common.internal.** { *; }

# Paquetes internal de play-services-* que exponen Firebase Components de ML Kit.
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# Firebase Components: garantiza que el ServiceLoader y las factorías sobrevivan
# al strip de R8 (necesario para el discovery de ExecutorSelector, etc.).
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
    public java.util.List getComponents();
}
-keep class com.google.firebase.components.** { *; }
-keep class * implements com.google.firebase.inject.Provider { *; }
