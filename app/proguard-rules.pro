# 保护 libsu 不被混淆
-keep class com.topjohnwu.superuser.** { *; }
# 保护你的 C++ JNI 方法
-keepclasseswithmembernames class * {
    native <methods>;
}