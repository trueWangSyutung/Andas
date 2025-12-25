//
// Created by wxd2zrx on 2025/12/25.
//

#ifndef ANDAS_DOUBLE_HASH_H
#define ANDAS_DOUBLE_HASH_H

#include <unordered_map>
#include <functional>
#include <vector>

// 自定义哈希函数以处理浮点数精度
struct DoubleHash {
    size_t operator()(const double& k) const;
};

// 自定义相等比较函数
struct DoubleEqual {
    bool operator()(const double& lhs, const double& rhs) const;
};

// 使用自定义比较的哈希表类型定义
using DoubleIndexMap = std::unordered_map<double, std::vector<int>, DoubleHash, DoubleEqual>;

#endif //ANDAS_DOUBLE_HASH_H
