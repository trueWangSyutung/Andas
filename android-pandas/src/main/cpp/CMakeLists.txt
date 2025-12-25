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

# 设置编译选项 - 移除OpenMP，使用标准C++17并行算法
target_compile_options(andas_native PRIVATE -O3 -Wall -Wextra)

# 对于Android，使用C++17的并行执行策略（需要Android NDK 21+）
# 如果NDK版本较低，会自动回退到串行执行
set_target_properties(andas_native PROPERTIES
    LINK_FLAGS "-static-libstdc++"
)
