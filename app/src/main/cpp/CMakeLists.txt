cmake_minimum_required(VERSION 3.22.1)
project("nexus-native-lib")

add_library(nexus-native-lib SHARED native-lib.cpp)

# 【关键修改】：加入隐藏符号表的标志
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -fdata-sections -ffunction-sections -Wl,--gc-sections")

find_library(log-lib log)
target_link_libraries(nexus-native-lib ${log-lib})