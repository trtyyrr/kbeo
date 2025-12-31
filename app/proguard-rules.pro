# 1. 保护 JNI 方法，否则 C++ 找不到 Java 函数
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. 保护 libsu 核心库，防止混淆导致 Root 执行失败
-keep class com.topjohnwu.superuser.** { *; }

# 3. 移除所有的代码日志（Log），防止通过日志分析你的修补流程
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# 4. 混淆你自己的业务代码，但保留入口 Activity
-keep class com.nexus.kernel.manager.MainActivity { *; }