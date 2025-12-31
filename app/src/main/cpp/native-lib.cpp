#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/stat.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nexus_kernel_manager_MainActivity_getKernelStatusFromCpp(
        JNIEnv* env, jobject /* this */) {
    // 通过 C++ 直接读取内核版本
    char buffer[128];
    FILE* pipe = popen("uname -r", "r");
    if (!pipe) return env->NewStringUTF("Error");
    fgets(buffer, 128, pipe);
    pclose(pipe);
    return env->NewStringUTF(buffer);
}

// 模拟授权管理：检查某个包名是否有 Root 记录（需 Root 权限下执行）
extern "C" JNIEXPORT jboolean JNICALL
Java_com_nexus_kernel_manager_MainActivity_checkModuleExists(
        JNIEnv* env, jobject /* this */, jstring moduleName) {
    const char *nativeModuleName = env->GetStringUTFChars(moduleName, 0);
    std::string path = "/data/adb/modules/" + std::string(nativeModuleName);
    
    struct stat buffer;
    bool exists = (stat(path.c_str(), &buffer) == 0);
    
    env->ReleaseStringUTFChars(moduleName, nativeModuleName);
    return exists;
}