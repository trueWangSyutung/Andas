//
// Created by wxd2zrx on 2025/12/25.
//

#include "double_harsh.h"

size_t DoubleHash::operator()(const double& k) const {
    // 将浮点数四舍五入到指定精度后哈希
    const double precision = 1e-9;
    return std::hash<long long>{}(static_cast<long long>(k / precision));
}

bool DoubleEqual::operator()(const double& lhs, const double& rhs) const {
    const double epsilon = 1e-9;
    return std::abs(lhs - rhs) < epsilon;
}
