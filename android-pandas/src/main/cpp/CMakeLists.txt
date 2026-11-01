cmake_minimum_required(VERSION 3.18.1)
project(andas_native)

# 设置C++标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加库
add_library(
    andas_native
    SHARED
    native-lib.cpp
    math_operations.cpp
    data_processing.cpp
    double_harsh.cpp
    double_harsh.h
)

# 查找并链接Android日志库
find_library(
    log-lib
    log
)

# 链接库
target_link_libraries(
    andas_native
    ${log-lib}
)

# 设置编译选项
target_compile_options(andas_native PRIVATE -O3 -Wall -Wextra)

# 启用 OpenMP 并行（NDK 21+ 的 libomp）。旧版本 NDK 找不到 OpenMP 时自动回退为串行。
find_package(OpenMP QUIET)
if(OpenMP_CXX_FOUND)
    target_compile_options(andas_native PRIVATE ${OpenMP_CXX_FLAGS})
    target_link_libraries(andas_native ${OpenMP_CXX_LIBRARIES})
    message(STATUS "Andas: OpenMP enabled")
else()
    message(STATUS "Andas: OpenMP not found, building serially")
endif()
