cmake_minimum_required(VERSION 3.22.1)

project("nexus-kernel-lib")

# 编译 C++ 代码为共享库 (.so)
add_library(nexus-kernel-lib SHARED native-lib.cpp)

# 链接 Android 系统的 Log 库，方便在底层调试
find_library(log-lib log)
target_link_libraries(nexus-kernel-lib ${log-lib})