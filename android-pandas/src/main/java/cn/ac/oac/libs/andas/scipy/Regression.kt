package cn.ac.oac.libs.andas.scipy

import cn.ac.oac.libs.andas.numpy.Linalg
import cn.ac.oac.libs.andas.numpy.NDArray
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** 回归模型通用预测接口。 */
interface RegressionModel {
    fun predict(features: NDArray): NDArray
    fun summary(): Map<String, Double>
}

data class LinearModel(
    val coefficients: NDArray,
    val intercept: Double?,
    val r2: Double,
    val adjustedR2: Double,
    val rmse: Double,
    val residuals: NDArray,
    val featureNames: List<String> = emptyList()
) : RegressionModel {

    override fun predict(features: NDArray): NDArray {
        val x = if (features.ndim == 1) features.reshape(1, features.size) else features
        require(x.shape[1] == coefficients.size) { "特征维度不匹配" }
        val result = arrayListOf<Double>()
        for (i in 0 until x.shape[0]) {
            var value = intercept ?: 0.0
            for (j in 0 until coefficients.size) value += x.get(i, j) * coefficients.get(j)
            result.add(value)
        }
        return NDArray.fromArray(result.toDoubleArray(), result.size)
    }

    override fun summary(): Map<String, Double> = linkedMapOf(
        "r2" to r2,
        "adjusted_r2" to adjustedR2,
        "rmse" to rmse,
        "intercept" to (intercept ?: Double.NaN)
    ).also { map ->
        featureNames.forEachIndexed { index, name -> map[name] = coefficients.get(index) }
    }
}

/** 多元线性回归（普通最小二乘）。 */
object MultipleLinearRegression {

    fun fit(features: NDArray, target: NDArray, featureNames: List<String> = emptyList()): LinearModel {
        require(features.ndim == 2 && target.ndim == 1 && features.shape[0] == target.size)
        val n = features.shape[0]
        val p = features.shape[1]
        val design = NDArray.zeros(n, p + 1)
        for (i in 0 until n) {
            design.set(1.0, i, 0)
            for (j in 0 until p) design.set(features.get(i, j), i, j + 1)
        }
        val xt = design.transpose()
        val normal = xt.matmul(design)
        val rhs = xt.matmul(NDArray.fromArray(target.toDoubleArray(), n, 1)).flatten()
        val beta = Linalg.solve(normal, rhs)
        val intercept = beta.get(0)
        val coefficients = NDArray.fromArray(DoubleArray(p) { beta.get(it + 1) }, p)
        val predictions = design.matmul(NDArray.fromArray(beta.toDoubleArray(), p + 1, 1)).flatten()
        val residuals = NDArray.fromArray(DoubleArray(n) { target.get(it) - predictions.get(it) }, n)
        val mean = target.mean()
        var sse = 0.0
        var sst = 0.0
        for (i in 0 until n) {
            sse += residuals.get(i) * residuals.get(i)
            sst += (target.get(i) - mean) * (target.get(i) - mean)
        }
        val r2 = if (sst == 0.0) 0.0 else 1.0 - sse / sst
        val adjusted = if (n - p - 1 <= 0) Double.NaN else 1.0 - (1.0 - r2) * (n - 1.0) / (n - p - 1.0)
        return LinearModel(coefficients, intercept, r2, adjusted, sqrt(sse / n), residuals, featureNames)
    }

    /** 便捷重载：以二维数组传入特征。 */
    fun fit(x: Array<DoubleArray>, y: DoubleArray, featureNames: List<String> = emptyList()): LinearModel =
        fit(NDArray.from2D(x), NDArray.fromArray(y, y.size), featureNames)
}

/** 多项式回归：y = b0 + b1*x + ... + bd*x^d。 */
object PolynomialRegression {

    fun fit(x: NDArray, y: NDArray, degree: Int): LinearModel {
        require(x.ndim == 1 && y.ndim == 1 && x.size == y.size && degree >= 1)
        val design = NDArray.zeros(x.size, degree)
        for (i in 0 until x.size) {
            var power = x.get(i)
            for (d in 0 until degree) {
                design.set(power, i, d)
                power *= x.get(i)
            }
        }
        val model = MultipleLinearRegression.fit(design, y, (1..degree).map { "x^$it" })
        // 多项式模型的 intercept 需要并入常数项；保持与 API 一致。
        return model
    }

    fun predict(model: LinearModel, x: NDArray): NDArray {
        require(x.ndim == 1)
        val design = NDArray.zeros(x.size, model.coefficients.size)
        for (i in 0 until x.size) {
            var power = x.get(i)
            for (d in 0 until model.coefficients.size) {
                design.set(power, i, d)
                power *= x.get(i)
            }
        }
        return model.predict(design)
    }
}

/** 岭回归（L2 正则化，截距不惩罚）。 */
object RidgeRegression {

    fun fit(features: NDArray, target: NDArray, lambda: Double = 1.0, fitIntercept: Boolean = true): LinearModel {
        require(features.ndim == 2 && target.ndim == 1 && features.shape[0] == target.size)
        require(lambda >= 0.0)
        val n = features.shape[0]
        val p = features.shape[1]
        val columns = if (fitIntercept) p + 1 else p
        val design = NDArray.zeros(n, columns)
        for (i in 0 until n) {
            if (fitIntercept) design.set(1.0, i, 0)
            for (j in 0 until p) design.set(features.get(i, j), i, j + (if (fitIntercept) 1 else 0))
        }
        val xt = design.transpose()
        val normal = xt.matmul(design)
        for (i in 0 until columns) {
            if (fitIntercept && i == 0) continue
            normal.set(normal.get(i, i) + lambda, i, i)
        }
        val rhs = xt.matmul(NDArray.fromArray(target.toDoubleArray(), n, 1)).flatten()
        val beta = Linalg.solve(normal, rhs)
        val intercept = if (fitIntercept) beta.get(0) else null
        val offset = if (fitIntercept) 1 else 0
        val coefficients = NDArray.fromArray(DoubleArray(p) { beta.get(it + offset) }, p)
        val predictions = design.matmul(NDArray.fromArray(beta.toDoubleArray(), columns, 1)).flatten()
        val residuals = NDArray.fromArray(DoubleArray(n) { target.get(it) - predictions.get(it) }, n)
        val mean = target.mean()
        var sse = 0.0
        var sst = 0.0
        for (i in 0 until n) {
            sse += residuals.get(i) * residuals.get(i)
            sst += (target.get(i) - mean) * (target.get(i) - mean)
        }
        val r2 = if (sst == 0.0) 0.0 else 1.0 - sse / sst
        return LinearModel(coefficients, intercept, r2, Double.NaN, sqrt(sse / n), residuals)
    }
}

/** 二分类逻辑回归（梯度下降）。 */
class LogisticRegression(
    val learningRate: Double = 0.1,
    val iterations: Int = 1000,
    val tolerance: Double = 1e-8
) {
    var coefficients: NDArray? = null
        private set
    var intercept: Double = 0.0
        private set

    fun fit(features: NDArray, labels: NDArray): LogisticRegression {
        require(features.ndim == 2 && labels.ndim == 1 && features.shape[0] == labels.size)
        val n = features.shape[0]
        val p = features.shape[1]
        val weights = DoubleArray(p + 1)
        for (iteration in 0 until iterations) {
            val gradient = DoubleArray(p + 1)
            for (i in 0 until n) {
                var z = weights[0]
                for (j in 0 until p) z += weights[j + 1] * features.get(i, j)
                val probability = sigmoid(z)
                val error = probability - labels.get(i)
                gradient[0] += error
                for (j in 0 until p) gradient[j + 1] += error * features.get(i, j)
            }
            var norm = 0.0
            for (j in weights.indices) {
                weights[j] -= learningRate * gradient[j] / n
                norm += gradient[j] * gradient[j]
            }
            if (sqrt(norm) / n < tolerance) break
        }
        intercept = weights[0]
        coefficients = NDArray.fromArray(DoubleArray(p) { weights[it + 1] }, p)
        return this
    }

    fun predictProba(features: NDArray): NDArray {
        val weights = coefficients ?: error("模型尚未训练")
        val x = if (features.ndim == 1) features.reshape(1, features.size) else features
        return NDArray.fromArray(DoubleArray(x.shape[0]) { i ->
            var z = intercept
            for (j in 0 until weights.size) z += weights.get(j) * x.get(i, j)
            sigmoid(z)
        }, x.shape[0])
    }

    fun predict(features: NDArray, threshold: Double = 0.5): NDArray =
        predictProba(features).map { if (it >= threshold) 1.0 else 0.0 }

    fun accuracy(features: NDArray, labels: NDArray): Double {
        val predicted = predict(features)
        var correct = 0
        for (i in 0 until labels.size) if (predicted.get(i) == labels.get(i)) correct++
        return correct.toDouble() / labels.size
    }

    fun logLoss(features: NDArray, labels: NDArray): Double {
        val probabilities = predictProba(features)
        var loss = 0.0
        for (i in 0 until labels.size) {
            val p = probabilities.get(i).coerceIn(1e-12, 1 - 1e-12)
            loss += if (labels.get(i) == 1.0) -ln(p) else -ln(1 - p)
        }
        return loss / labels.size
    }

    private fun sigmoid(value: Double): Double = 1.0 / (1.0 + exp(-value))
}
