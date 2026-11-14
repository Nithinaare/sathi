-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.farmsaathi.core.common.navigation.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
