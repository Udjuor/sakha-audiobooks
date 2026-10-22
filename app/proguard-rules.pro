# Правила для уменьшения размера release-APK.
# kotlinx.serialization: сохраняем сгенерированные сериализаторы
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
