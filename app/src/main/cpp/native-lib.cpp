#include <jni.h>
#include <string>
#include <sys/utsname.h>
#include <unistd.h>

/**
 * 获取内核版本号
 * 通过系统调用 uname 直接读取，不经过 Android API
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_nexus_kernel_manager_MainActivity_getKernelVersion(JNIEnv* env, jobject /* this */) {
    struct utsname buffer;
    if (uname(&buffer) != 0) {
        return env->NewStringUTF("Unknown Environment");
    }
    return env->NewStringUTF(buffer.release);
}

/**
 * 内核级权限验证
 * 核心：直接获取当前进程的真实 UID
 * 只要 UID 为 0，即代表拥有最高内核权限 (Root)
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_nexus_kernel_manager_MainActivity_checkNativePrivilege(JNIEnv* env, jobject /* this */) {
    // getuid() 是 Linux 标准调用，极难被 Java 层 Hook 框架欺骗
    return (getuid() == 0) ? JNI_TRUE : JNI_FALSE;
}