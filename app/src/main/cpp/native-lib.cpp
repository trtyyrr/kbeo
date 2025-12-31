#include <jni.h>
#include <string>
#include <sys/utsname.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nexus_kernel_manager_MainActivity_getKernelVersion(JNIEnv* env, jobject /* this */) {
    struct utsname buffer;
    // 使用 Linux 系统调用获取内核 release 信息
    if (uname(&buffer) != 0) {
        return env->NewStringUTF("Unknown Kernel");
    }
    return env->NewStringUTF(buffer.release);
}