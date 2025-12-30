cmake_minimum_required(VERSION 3.22.1)

project("kernel_manager")

add_library(kernel_manager SHARED native-lib.cpp)

find_library(log-lib log)

target_link_libraries(kernel_manager ${log-lib})