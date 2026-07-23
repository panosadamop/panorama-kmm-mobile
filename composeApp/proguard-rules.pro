# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class gr.panoramapolihnitou.app.**$$serializer { *; }
-keepclassmembers class gr.panoramapolihnitou.app.** {
    *** Companion;
}
-keepclasseswithmembers class gr.panoramapolihnitou.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
