# LibXposed / Xposed module entry points
-dontwarn io.github.libxposed.annotation.**
-dontwarn io.github.libxposed.api.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# JNI entry point: the native library resolves this exact class and method name.
-keep class io.github.hexstr.UnityFPSUnlocker.MyModule { *; }
