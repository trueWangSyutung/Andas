package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series
import cn.ac.oac.libs.andas.numpy.FFT
import cn.ac.oac.libs.andas.numpy.Linalg
import cn.ac.oac.libs.andas.numpy.DType
import cn.ac.oac.libs.andas.numpy.NDArray
import cn.ac.oac.libs.andas.numpy.NumpyRandom
import cn.ac.oac.libs.andas.numpy.PandasBridge
import cn.ac.oac.libs.andas.numpy.toDataFrame
import cn.ac.oac.libs.andas.numpy.toNDArray
import cn.ac.oac.libs.andas.numpy.toSeries
import cn.ac.oac.libs.andas.annotation.AndasExtensions
import cn.ac.oac.libs.andas.plot.Figure
import cn.ac.oac.libs.andas.scipy.Integrate
import cn.ac.oac.libs.andas.scipy.LinearInterpolator
import cn.ac.oac.libs.andas.scipy.Optimize
import cn.ac.oac.libs.andas.scipy.Signal
import cn.ac.oac.libs.andas.scipy.image.ImageOps
import cn.ac.oac.libs.andas.scipy.sparse.SparseMatrix
import cn.ac.oac.libs.andas.scipy.Special
import cn.ac.oac.libs.andas.scipy.LogisticRegression
import cn.ac.oac.libs.andas.scipy.MultipleLinearRegression
import cn.ac.oac.libs.andas.scipy.PolynomialRegression
import cn.ac.oac.libs.andas.scipy.RidgeRegression
import cn.ac.oac.libs.andas.scipy.Stats
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NumpyScipyTest {

    @Test
    fun ndarrayCreationAndShape() {
        val a = NDArray.arange(0.0, 6.0).reshape(2, 3)
        assertEquals(2, a.shape[0]); assertEquals(3, a.shape[1])
        assertEquals(1.0, a.get(0, 1), 0.0)
        assertEquals(5.0, a.get(1, 2), 0.0)
        val t = a.transpose()
        assertEquals(3, t.shape[0]); assertEquals(2, t.shape[1])
        assertEquals(3.0, t.get(0, 1), 0.0)
        assertArrayEquals(doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), a.toDoubleArray(), 0.0)
    }

    @Test
    fun ndarrayArithmeticAndBroadcast() {
        val a = NDArray.from2D(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val b = NDArray.from2D(arrayOf(doubleArrayOf(10.0, 20.0), doubleArrayOf(30.0, 40.0)))
        assertEquals(11.0, (a + b).get(0, 0), 0.0)
        assertEquals(14.0, (a + NDArray.of(10.0, 10.0)).get(1, 1), 0.0)
        assertEquals(20.0, (a * 2.0).sum(), 0.0)
        assertEquals(2.5, a.mean(), 1e-12)
        assertEquals(1.2909944487358056, a.std(), 1e-12)
        assertEquals(4.0, a.max(), 0.0)
        assertEquals(3, a.argmax())
    }

    @Test
    fun matrixAndLinearAlgebra() {
        val a = NDArray.from2D(arrayOf(doubleArrayOf(4.0, 7.0), doubleArrayOf(2.0, 6.0)))
        val b = NDArray.from2D(arrayOf(doubleArrayOf(1.0, 0.0), doubleArrayOf(0.0, 1.0)))
        val product = a.matmul(b)
        assertEquals(4.0, product.get(0, 0), 0.0)
        assertEquals(6.0, product.get(1, 1), 0.0)
        assertEquals(10.0, Linalg.det(a), 1e-9)
        val inverse = Linalg.inv(a)
        assertEquals(0.6, inverse.get(0, 0), 1e-9)
        val solution = Linalg.solve(a, NDArray.of(1.0, 2.0))
        assertEquals(-0.8, solution.get(0), 1e-9)
        val fit = Linalg.lstsq(NDArray.of(1.0, 2.0, 3.0), NDArray.of(2.0, 5.0, 8.0))
        assertEquals(3.0, fit.get(0), 1e-9) // slope
        assertEquals(-1.0, fit.get(1), 1e-9) // intercept
    }

    @Test
    fun fftAndRandom() {
        val spectrum = FFT.fft(NDArray.of(1.0, 1.0, 1.0, 1.0))
        assertEquals(4.0, spectrum.real[0], 1e-9)
        assertEquals(0.0, spectrum.imag[0], 1e-9)
        val restored = FFT.ifft(spectrum)
        for (i in 0 until 4) assertEquals(1.0, restored.real[i], 1e-9)
        val random = NumpyRandom(42)
        val first = random.normal(size = 5).toDoubleArray()
        val second = NumpyRandom(42).normal(size = 5).toDoubleArray()
        assertArrayEquals(first, second, 0.0)
    }

    @Test
    fun scipyStatisticsAndCalculus() {
        assertEquals(0.8427007929497149, Special.erf(1.0), 1e-6)
        assertEquals(120.0, Special.gamma(6.0), 1e-6)
        assertEquals(0.9772498680518208, Stats.normalCdf(2.0), 1e-6)

        val values = NDArray.of(1.0, 2.0, 3.0, 4.0, 5.0)
        assertEquals(3.0, Stats.quantile(values, 0.5), 1e-12)
        assertEquals(1.0, Stats.pearson(values, values), 1e-12)
        val ttest = Stats.ttestOneSample(values, 3.0)
        assertEquals(0.0, ttest.t, 1e-12)
        assertTrue(ttest.p > 0.9)

        val x = NDArray.of(1.0, 2.0, 3.0, 4.0)
        val y = NDArray.of(3.0, 5.0, 7.0, 9.0)
        val regression = Stats.linregress(x, y)
        assertEquals(2.0, regression.slope, 1e-12)
        assertEquals(1.0, regression.intercept, 1e-12)
        assertEquals(1.0, regression.r, 1e-12)

        val minimum = Optimize.goldenSection({ (it - 2.0) * (it - 2.0) }, -5.0, 5.0)
        assertEquals(2.0, minimum.x, 1e-6)
        assertEquals(3.0, Integrate.trapezoid(NDArray.of(0.0, 1.0, 4.0)), 1e-9)
        assertEquals(2.6666666666666665, Integrate.simpson(NDArray.of(0.0, 1.0, 4.0)), 1e-9)

        val interpolator = LinearInterpolator(NDArray.of(0.0, 10.0), NDArray.of(0.0, 100.0))
        assertEquals(50.0, interpolator.evaluate(5.0), 1e-9)
        assertArrayEquals(doubleArrayOf(1.0, 4.0, 7.0, 6.0), Signal.convolve(NDArray.of(1.0, 2.0, 3.0), NDArray.of(1.0, 2.0)).toDoubleArray(), 1e-9)
    }

    @Test
    fun plottingProducesSvg() {
        val figure = Figure(640, 480)
        figure.title = "Andas Plot"
        val axes = figure.axes()
        axes.title = "Series"
        axes.line(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(1.0, 4.0, 9.0), "square")
        axes.scatter(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(1.0, 4.0, 9.0))
        axes.hist(NDArray.of(1.0, 2.0, 2.0, 3.0, 4.0), bins = 3)
        val svg = figure.renderSvg()
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("polyline"))
        assertTrue(svg.contains("circle"))
        assertTrue(svg.contains("rect"))
    }

    @Test
    fun pandasStyleExtensions() {
        val series = Series(listOf(1.0, 2.0, 3.0, 4.0))
        assertTrue(series.shift(1).get(0) == null)
        assertEquals(1.0, series.shift(1).get(1)!!, 0.0)
        val differences = series.diff().toDoubleArray()
        assertTrue(differences[0].isNaN())
        assertArrayEquals(doubleArrayOf(1.0, 1.0, 1.0), differences.copyOfRange(1, 4), 0.0)
        assertTrue(series.rolling(2).mean().get(0) == null)
        assertEquals(1.5, series.rolling(2).mean().get(1)!!, 1e-12)
        assertEquals(2.5, series.median(), 1e-12)

        val df = DataFrame(
            mapOf(
                "group" to listOf("a", "a", "b", "b"),
                "value" to listOf(1, 2, 3, 4),
                "other" to listOf(4, 3, 2, 1)
            )
        )
        assertEquals(2, df.valueCounts("group")["a"])
        assertEquals(2, df.dropDuplicates("group").shape().first)
        assertEquals(4, df.nlargest(1, "value").at(0, "value"))
        assertEquals(2, df.cov().shape().first)
        assertEquals(5, df.describeAll().shape().first)
        assertTrue(df.quantile("value", 0.5) == 2.5)
        assertTrue(df.corr().shape().first == 2)
    }

    @Test
    fun regressionModelsAreConsistent() {
        val features = NDArray.from2D(
            arrayOf(
                doubleArrayOf(1.0, 2.0),
                doubleArrayOf(2.0, 1.0),
                doubleArrayOf(3.0, 4.0),
                doubleArrayOf(4.0, 3.0)
            )
        )
        val target = NDArray.of(9.0, 8.0, 19.0, 18.0) // 1 + 2*x1 + 3*x2
        val model = MultipleLinearRegression.fit(features, target, listOf("x1", "x2"))
        assertEquals(1.0, model.intercept!!, 1e-9)
        assertEquals(2.0, model.coefficients.get(0), 1e-9)
        assertEquals(3.0, model.coefficients.get(1), 1e-9)
        assertEquals(1.0, model.r2, 1e-9)
        assertEquals(9.0, model.predict(NDArray.of(1.0, 2.0)).get(0), 1e-9)

        val polynomialX = NDArray.of(0.0, 1.0, 2.0, 3.0)
        val polynomialY = polynomialX.map { 1.0 + 2.0 * it + it * it }
        val polynomial = PolynomialRegression.fit(polynomialX, polynomialY, 2)
        assertEquals(25.0, PolynomialRegression.predict(polynomial, NDArray.of(4.0)).get(0), 1e-8)

        val ridge = RidgeRegression.fit(features, target, lambda = 1e-10)
        assertEquals(2.0, ridge.coefficients.get(0), 1e-4)
        assertEquals(3.0, ridge.coefficients.get(1), 1e-4)

        val logisticX = NDArray.from2D(arrayOf(doubleArrayOf(0.0), doubleArrayOf(1.0), doubleArrayOf(2.0), doubleArrayOf(3.0)))
        val logisticY = NDArray.of(0.0, 0.0, 1.0, 1.0)
        val logistic = LogisticRegression(learningRate = 0.5, iterations = 2000).fit(logisticX, logisticY)
        assertEquals(1.0, logistic.accuracy(logisticX, logisticY), 1e-9)
        assertTrue(logistic.predictProba(NDArray.of(3.0)).get(0) > 0.5)
    }

    @Test
    fun ndarrayPandasRoundTripAndExtensions() {
        val df = DataFrame(
            mapOf(
                "a" to listOf(1, null, 3),
                "b" to listOf(4.0, 5.0, 6.0),
                "c" to listOf("x", "y", "z")
            )
        )
        val array = df.toNDArray()
        assertEquals(3, array.shape[0])
        assertEquals(2, array.shape[1])
        assertTrue(array.get(1, 0).isNaN())
        val restored = array.toDataFrame(listOf("a", "b"))
        assertEquals(null, restored.at(1, "a"))
        assertEquals(5.0, restored.at(1, "b"))

        val series = Series(listOf(1.0, 2.0, 3.0))
        assertEquals(6.0, series.toNDArray().sum(), 0.0)
        assertEquals(2.0, series.toNDArray().toSeries().get(1))

        AndasExtensions.clear()
        AndasExtensions.registerSeries("sum") { s -> s.toDoubleArray().sum() }
        assertEquals(6.0, AndasExtensions.invokeSeries("sum", series))
        AndasExtensions.registerNDArray("norm") { a -> a.norm() }
        assertEquals(series.toNDArray().norm(), AndasExtensions.invokeNDArray("norm", series.toNDArray()))
        assertEquals(2, AndasExtensions.list().size)
    }


    @Test
    fun dtypeAndGeneralBroadcast() {
        val ints = NDArray.fromIntArray(intArrayOf(1, 2, 3, 4), 2, 2)
        assertEquals(DType.INT32, ints.dtype)
        assertEquals(4L, ints.getLong(1, 1))
        assertEquals(10L, ints.sumLong())

        val floats = ints.astype(DType.FLOAT32)
        assertEquals(DType.FLOAT32, floats.dtype)
        assertEquals(1.25, (floats / 2.0).mean(), 1e-6)

        val longs = ints.astype(DType.INT64)
        assertEquals(DType.INT64, longs.dtype)
        assertEquals(4L, longs.getLong(1, 1))

        val cube = NDArray.zeros(2, 3, 4)
        val row = NDArray.zeros(1, 1, 4).map { 2.0 }
        val broadcast = cube + row
        assertEquals(2.0, broadcast.get(1, 2, 3), 0.0)
        assertEquals(2.0, broadcast.get(0, 0, 0), 0.0)

        val bools = NDArray.fromBooleanArray(booleanArrayOf(true, false, true), 3)
        assertEquals(DType.BOOLEAN, bools.dtype)
        assertEquals(2.0, bools.sum(), 0.0)
    }


    @Test
    fun advancedNumericAndSparseImage() {
        val cube = NDArray.arange(0.0, 24.0).reshape(2, 3, 4)
        val columnSums = cube.sum(axis = 1)
        assertEquals(2, columnSums.shape[0]); assertEquals(4, columnSums.shape[1])
        assertEquals(12.0, columnSums.get(0, 0), 0.0) // 0+4+8
        assertEquals(15.0, columnSums.get(0, 1), 0.0)
        val transposed = cube.transpose(0, 2, 1)
        assertEquals(2, transposed.shape[0]); assertEquals(4, transposed.shape[1]); assertEquals(3, transposed.shape[2])

        val joined = NDArray.concatenate(listOf(NDArray.of(1.0, 2.0), NDArray.of(3.0, 4.0)))
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), joined.toDoubleArray(), 0.0)
        val selected = NDArray.where(NDArray.fromBooleanArray(booleanArrayOf(true, false), 2), NDArray.of(9.0, 9.0), NDArray.of(0.0, 0.0))
        assertArrayEquals(doubleArrayOf(9.0, 0.0), selected.toDoubleArray(), 0.0)

        val beta = Special.beta(2.0, 3.0)
        assertEquals(1.0 / 12.0, beta, 1e-12)
        assertEquals(1.959963984540054, Stats.normalInv(0.975), 1e-6)
        assertEquals(0.95, Stats.uniformCdf(0.95), 1e-12)
        val correlation = Stats.corrcoef(NDArray.of(1.0, 2.0, 3.0), NDArray.of(2.0, 4.0, 6.0))
        assertEquals(1.0, correlation.get(0, 1), 1e-12)
        val welch = Stats.ttestInd(NDArray.of(1.0, 2.0, 3.0), NDArray.of(1.1, 2.1, 3.1))
        assertTrue(welch.p > 0.5)

        val integral = Integrate.quad({ kotlin.math.sin(it) }, 0.0, Math.PI)
        assertEquals(2.0, integral, 1e-7)

        val direct = Signal.convolve(NDArray.of(1.0, 2.0, 3.0), NDArray.of(4.0, 5.0))
        val fast = Signal.fftConvolve(NDArray.of(1.0, 2.0, 3.0), NDArray.of(4.0, 5.0))
        assertArrayEquals(direct.toDoubleArray(), fast.toDoubleArray(), 1e-9)
        assertEquals(64, Signal.hamming(64).size)

        val dense = NDArray.from2D(arrayOf(doubleArrayOf(1.0, 0.0, 2.0), doubleArrayOf(0.0, 3.0, 0.0)))
        val sparse = SparseMatrix.fromDense(dense)
        assertEquals(3, sparse.nnz)
        val product = sparse.multiply(NDArray.of(1.0, 2.0, 3.0))
        assertEquals(7.0, product.get(0), 1e-12)
        assertEquals(6.0, product.get(1), 1e-12)
        assertEquals(3, sparse.transpose().rows)

        val rgb = NDArray.fromArray(DoubleArray(2 * 2 * 3) { 100.0 }, 2, 2, 3)
        assertEquals(100.0, ImageOps.grayscale(rgb).get(0, 0), 1e-6)
        assertEquals(100.0, ImageOps.gaussianBlur(ImageOps.grayscale(rgb), 1.0).get(1, 1), 1e-6)
        assertEquals(0.0, ImageOps.sobel(ImageOps.grayscale(rgb)).get(1, 1), 1e-6)
        assertEquals(255.0, ImageOps.threshold(ImageOps.grayscale(rgb), 50.0).get(0, 0), 0.0)
    }


    @Test
    fun plotGallerySupportsMoreChartTypes() {
        val figure = Figure(900, 600, rows = 2, cols = 2)
        figure.axes(0, 0).apply {
            area(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(1.0, 4.0, 9.0), "area")
            step(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(2.0, 3.0, 1.0), "step")
        }
        figure.axes(0, 1).apply {
            errorBar(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(3.0, 5.0, 4.0), doubleArrayOf(0.2, 0.4, 0.3), "error")
            barh(listOf("A", "B", "C"), doubleArrayOf(4.0, 8.0, 6.0), "barh")
        }
        figure.axes(1, 0).apply {
            stackedBar(
                listOf("Q1", "Q2", "Q3"),
                listOf(doubleArrayOf(1.0, 2.0, 1.5), doubleArrayOf(2.0, 1.0, 2.5)),
                listOf("线上", "线下")
            )
        }
        figure.axes(1, 1).apply {
            box(listOf("A", "B"), listOf(doubleArrayOf(1.0, 2.0, 3.0, 4.0), doubleArrayOf(2.0, 2.5, 3.5, 5.0)))
        }
        val svg = figure.renderSvg()
        assertTrue(svg.contains("<polygon"))
        assertTrue(svg.contains("<rect"))
        assertTrue(svg.contains("Q1"))

        val heatmap = Figure(400, 300)
        heatmap.axes().heatmap(NDArray.from2D(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))))
        assertTrue(heatmap.renderSvg().contains("<rect"))
    }

}
