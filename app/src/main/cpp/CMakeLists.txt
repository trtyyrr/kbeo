cmake_minimum_required(VERSION 3.22.1)
project("nexus-native-lib")

# 创建库文件
add_library(nexus-native-lib SHARED native-lib.cpp)

# 链接安卓系统日志库
find_library(log-lib log)
target_link_libraries(nexus-native-lib ${log-lib})