package cn.ac.oac.libs.andas.entity

import cn.ac.oac.libs.andas.types.AndaTypes
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset

/**
 * 流式 CSV 解析器。
 *
 * 设计目标：
 * 1. 不把整个文件读成 `List<String>`，内存占用只与列数和单行长度相关；
 * 2. 逐行解析后直接写入列式缓冲区（DoubleArray/LongArray/IntArray/BooleanArray），
 *    避免为每个单元格创建 Map.Entry 和装箱对象；
 * 3. 共享一个 O(1) 内存的范围索引，避免每列各自持有一份索引和 HashMap；
 * 4. 类型推断使用前 N 行样本，遇到后续行无法解析时自动降级为字符串列，保证不丢数据。
 */
internal object CsvReader {

    val DEFAULT_NULL_VALUES: List<String> = listOf("", "null", "NULL", "NA", "N/A")

    data class Options(
        val delimiter: String = ",",
        val header: Boolean = true,
        val autoType: Boolean = true,
        val encoding: String = "UTF-8",
        val skipLines: Int = 0,
        val nullValues: List<String> = DEFAULT_NULL_VALUES,
        val trimValues: Boolean = true,
        val sampleRows: Int = 64
    )

    fun read(inputStream: InputStream, options: Options): DataFrame {
        val session = Session(inputStream, options)
        try {
            session.initialize()
            val builders = session.newBuilders()
            session.addSamples(builders)
            var rowCount = session.sampleRowCount

            while (session.readRecord()) {
                session.addCurrentRecord(builders)
                rowCount++
            }
            return session.buildDataFrame(builders, rowCount)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("读取CSV失败: ${e.message}", e)
        } finally {
            session.close()
        }
    }

    fun read(file: File, options: Options): DataFrame {
        if (!file.exists()) {
            throw IllegalArgumentException("文件不存在: ${file.absolutePath}")
        }
        if (!file.canRead()) {
            throw SecurityException("无法读取文件: ${file.absolutePath}")
        }
        return file.inputStream().buffered(1 shl 16).use { read(it, options) }
    }

    /**
     * 真正流式的分批读取：每攒够 batchSize 行就回调一次，不会把整个文件读入内存。
     */
    fun readBatches(
        inputStream: InputStream,
        batchSize: Int,
        options: Options,
        callback: (DataFrame) -> Unit
    ) {
        require(batchSize > 0) { "batchSize 必须大于 0" }
        val session = Session(inputStream, options)
        try {
            session.initialize()

            var builders = session.newBuilders()
            var rows = 0
            for (i in 0 until session.sampleRowCount) {
                session.addSampleRow(builders, session.sampleRowAt(i))
                rows++
                if (rows >= batchSize) {
                    callback(session.buildDataFrame(builders, rows))
                    builders = session.newBuilders()
                    rows = 0
                }
            }

            while (session.readRecord()) {
                session.addCurrentRecord(builders)
                rows++
                if (rows >= batchSize) {
                    callback(session.buildDataFrame(builders, rows))
                    builders = session.newBuilders()
                    rows = 0
                }
            }

            if (rows > 0) {
                callback(session.buildDataFrame(builders, rows))
            }
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("读取CSV失败: ${e.message}", e)
        } finally {
            session.close()
        }
    }

    // ==================== 解析会话 ====================

    private class Session(inputStream: InputStream, private val options: Options) {
        private val charset: Charset = Charset.forName(options.encoding)
        private val reader = BufferedReader(InputStreamReader(inputStream, charset), 1 shl 16)
        private val recordReader: CsvRecordReader

        private val columnNames = ArrayList<String>()
        private val columnKinds = ArrayList<Kind>()
        private val sampleRows = ArrayList<Array<String>>()
        var sampleRowCount: Int = 0
            private set

        init {
            require(options.delimiter.isNotEmpty()) { "分隔符不能为空" }
            recordReader = CsvRecordReader(reader, options.delimiter[0], options.trimValues)
        }

        fun initialize() {
            var skipped = 0
            while (skipped < options.skipLines) {
                if (!recordReader.readRecord()) return
                skipped++
            }

            if (options.header) {
                if (!recordReader.readRecord()) return
                val header = recordReader.currentFieldsAsStrings()
                for (i in header.indices) {
                    val name = header[i].ifBlank { "col$i" }
                    // 重复列名自动去重，避免后者覆盖前者导致数据丢失
                    var unique = name
                    var suffix = 1
                    while (unique in columnNames) {
                        unique = "${name}_${suffix++}"
                    }
                    columnNames.add(unique)
                }
            }

            // 采样：用于推断列类型。header=false 时第一行也要作为数据参与采样。
            val maxSamples = options.sampleRows.coerceAtLeast(1)
            while (sampleRows.size < maxSamples && recordReader.readRecord()) {
                val fields = recordReader.currentFieldsAsStrings()
                sampleRows.add(fields)
            }
            sampleRowCount = sampleRows.size

            if (!options.header) {
                val firstCount = sampleRows.firstOrNull()?.size ?: 0
                for (i in 0 until firstCount) columnNames.add("col$i")
            }

            inferKinds()
        }

        fun newBuilders(): Array<ColumnBuilder> =
            Array(columnNames.size) { i -> ColumnBuilder(columnKinds[i]) }

        fun addSamples(builders: Array<ColumnBuilder>) {
            for (row in sampleRows) addSampleRow(builders, row)
        }

        fun addSampleRow(builders: Array<ColumnBuilder>, row: Array<String>) {
            for (c in builders.indices) {
                val text = if (c < row.size) row[c] else null
                builders[c].addString(text, options.nullValues)
            }
        }

        fun sampleRowAt(index: Int): Array<String> = sampleRows[index]

        fun addCurrentRecord(builders: Array<ColumnBuilder>) {
            val fieldCount = recordReader.fieldCount
            for (c in builders.indices) {
                if (c < fieldCount) {
                    builders[c].addField(recordReader.field(c), options.nullValues)
                } else {
                    builders[c].addNull()
                }
            }
        }

        fun readRecord(): Boolean = recordReader.readRecord()

        fun buildDataFrame(builders: Array<ColumnBuilder>, rowCount: Int): DataFrame {
            if (columnNames.isEmpty()) {
                return DataFrame(emptyList<Map<String, Any?>>())
            }
            val index = if (rowCount > 64) RangeIndexList(rowCount) else (0 until rowCount).toList()
            val series = LinkedHashMap<String, Series<Any>>(columnNames.size)
            for (i in columnNames.indices) {
                series[columnNames[i]] = builders[i].build(index, columnNames[i])
            }
            return DataFrame.fromSeries(columnNames.toList(), series)
        }

        fun close() {
            try {
                reader.close()
            } catch (_: Exception) {
            }
        }

        private fun inferKinds() {
            for (c in columnNames.indices) {
                var kind = Kind.UNKNOWN
                if (!options.autoType) {
                    kind = Kind.STRING
                } else {
                    for (row in sampleRows) {
                        val text = if (c < row.size) row[c] else null
                        val cellKind = Kind.of(text, options.nullValues)
                        kind = Kind.merge(kind, cellKind)
                        if (kind == Kind.STRING) break
                    }
                    if (kind == Kind.UNKNOWN || kind == Kind.DATE) {
                        // 全空列或日期列：保持字符串，日期不做类型转换（与原有行为一致）
                        kind = Kind.STRING
                    }
                }
                columnKinds.add(kind)
            }
        }
    }

    // ==================== 字符级 CSV 读取 ====================

    private class CsvRecordReader(
        private val reader: Reader,
        private val delimiter: Char,
        private val trimValues: Boolean
    ) {
        private val buffer = CharArray(1 shl 16)
        private var pos = 0
        private var limit = 0
        private var eof = false
        private var first = true

        private val fields = ArrayList<Field>()
        var fieldCount: Int = 0
            private set

        var lineNumber: Long = 0
            private set

        fun field(index: Int): Field = fields[index]

        fun currentFieldsAsStrings(): Array<String> {
            val result = Array(fieldCount) { fields[it].toStringValue() }
            return result
        }

        private fun ensure(index: Int): Field {
            while (fields.size <= index) fields.add(Field())
            return fields[index]
        }

        /**
         * 读取一条记录。返回 false 表示已到文件末尾。
         */
        fun readRecord(): Boolean {
            fieldCount = 0
            var inQuotes = false
            var quoteChar = '"'
            var field = ensure(0)
            field.reset()
            var started = false

            while (true) {
                val code = nextChar()
                if (code < 0) {
                    if (!started && fieldCount == 0 && field.length == 0) return false
                    field.finish(trimValues)
                    fieldCount++
                    lineNumber++
                    return true
                }
                val c = code.toChar()
                started = true

                if (inQuotes) {
                    when {
                        c == quoteChar -> {
                            val next = peekChar()
                            if (next == quoteChar.code) {
                                nextChar()
                                field.append(quoteChar)
                            } else {
                                inQuotes = false
                            }
                        }
                        c == '\r' -> {
                            if (peekChar() == '\n'.code) nextChar()
                            field.append('\n')
                            lineNumber++
                        }
                        c == '\n' -> {
                            field.append('\n')
                            lineNumber++
                        }
                        else -> field.append(c)
                    }
                } else {
                    when {
                        c == '\n' -> {
                            lineNumber++
                            if (fieldCount == 0 && field.length == 0) {
                                // 跳过空行
                                field = ensure(0)
                                field.reset()
                                continue
                            }
                            field.finish(trimValues)
                            fieldCount++
                            return true
                        }
                        c == '\r' -> {
                            if (peekChar() == '\n'.code) nextChar()
                            lineNumber++
                            if (fieldCount == 0 && field.length == 0) {
                                field = ensure(0)
                                field.reset()
                                continue
                            }
                            field.finish(trimValues)
                            fieldCount++
                            return true
                        }
                        c == delimiter -> {
                            field.finish(trimValues)
                            fieldCount++
                            field = ensure(fieldCount)
                            field.reset()
                        }
                        c == '"' -> {
                            inQuotes = true
                            quoteChar = '"'
                        }
                        else -> field.append(c)
                    }
                }
            }
        }

        private fun nextChar(): Int {
            if (pos >= limit && !fill()) return -1
            var c = buffer[pos++]
            if (first) {
                first = false
                if (c == '\uFEFF') {
                    if (pos >= limit && !fill()) return -1
                    c = buffer[pos++]
                }
            }
            return c.code
        }

        private fun peekChar(): Int {
            if (pos >= limit && !fill()) return -1
            return buffer[pos].code
        }

        private fun fill(): Boolean {
            if (eof) return false
            var read = 0
            while (true) {
                val r = reader.read(buffer, 0, buffer.size)
                if (r < 0) {
                    eof = true
                    return false
                }
                if (r == 0) continue
                read = r
                break
            }
            pos = 0
            limit = read
            return true
        }
    }

    /** 可复用的字段缓冲区，避免每个字段创建 String。 */
    private class Field {
        private var chars = CharArray(32)
        var length = 0
            private set
        private var start = 0
        private var size = 0

        fun reset() {
            length = 0
            start = 0
            size = 0
        }

        fun append(c: Char) {
            if (length == chars.size) {
                chars = chars.copyOf(chars.size * 2)
            }
            chars[length++] = c
        }

        fun finish(trim: Boolean) {
            var end = length
            start = 0
            if (trim) {
                while (start < end && chars[start] <= ' ') start++
                while (end > start && chars[end - 1] <= ' ') end--
            }
            size = end - start
        }

        fun isNullValue(nullValues: List<String>): Boolean {
            if (size <= 0) return true
            for (nv in nullValues) {
                if (nv.isNotEmpty() && equalsString(nv)) return true
            }
            return false
        }

        fun toStringValue(): String = String(chars, start, size)

        fun equalsString(value: String): Boolean {
            if (value.length != size) return false
            for (i in 0 until size) {
                if (chars[start + i] != value[i]) return false
            }
            return true
        }

        fun toIntOrNull(): Int? {
            val value = toLongOrNull() ?: return null
            return if (value in Int.MIN_VALUE..Int.MAX_VALUE) value.toInt() else null
        }

        fun toLongOrNull(): Long? {
            if (size == 0) return null
            var i = start
            var negative = false
            if (chars[i] == '-') {
                negative = true
                i++
                if (i >= start + size) return null
            }
            // 以负数累加，才能正确表示 Long.MIN_VALUE
            var result = 0L
            while (i < start + size) {
                val d = chars[i] - '0'
                if (d !in 0..9) return null
                if (result < (Long.MIN_VALUE + d) / 10) return null
                result = result * 10 - d
                i++
            }
            if (!negative) {
                if (result == Long.MIN_VALUE) return null
                return -result
            }
            return result
        }

        fun toDoubleOrNull(): Double? {
            if (size == 0) return null
            // 浮点数包含指数、小数点等复杂情况，交给 JDK 实现；这里只做格式白名单过滤，
            // 避免把 "abc" 当成失败后反复抛异常。
            var i = start
            if (chars[i] == '-') i++
            var digits = 0
            while (i < start + size && chars[i] in '0'..'9') {
                i++
                digits++
            }
            if (i < start + size && chars[i] == '.') {
                i++
                while (i < start + size && chars[i] in '0'..'9') {
                    i++
                    digits++
                }
            }
            if (digits == 0) return null
            if (i < start + size && (chars[i] == 'e' || chars[i] == 'E')) {
                i++
                if (i < start + size && (chars[i] == '+' || chars[i] == '-')) i++
                var expDigits = 0
                while (i < start + size && chars[i] in '0'..'9') {
                    i++
                    expDigits++
                }
                if (expDigits == 0) return null
            }
            if (i != start + size) return null
            return toStringValue().toDoubleOrNull()
        }

        fun toBooleanOrNull(): Boolean? {
            if (equalsIgnoreCase("true") || equalsIgnoreCase("yes")) return true
            if (equalsIgnoreCase("false") || equalsIgnoreCase("no")) return false
            return null
        }

        fun isDateLike(): Boolean {
            // yyyy-MM-dd
            if (size == 10 && chars[start + 4] == '-' && chars[start + 7] == '-') {
                return allDigits(start, 4) && allDigits(start + 5, 2) && allDigits(start + 8, 2)
            }
            // MM/dd/yyyy
            if (size == 10 && chars[start + 2] == '/' && chars[start + 5] == '/') {
                return allDigits(start, 2) && allDigits(start + 3, 2) && allDigits(start + 6, 4)
            }
            return false
        }

        private fun allDigits(from: Int, count: Int): Boolean {
            for (i in from until from + count) {
                if (chars[i] !in '0'..'9') return false
            }
            return true
        }

        private fun equalsIgnoreCase(value: String): Boolean {
            if (value.length != size) return false
            for (i in 0 until size) {
                if (chars[start + i].lowercaseChar() != value[i].lowercaseChar()) return false
            }
            return true
        }
    }

    // ==================== 类型与列构建 ====================

    private enum class Kind {
        UNKNOWN, INT, LONG, DOUBLE, BOOLEAN, DATE, STRING;

        companion object {
            fun of(text: String?, nullValues: List<String>): Kind {
                if (text == null || text.isEmpty() || text in nullValues) return UNKNOWN
                // 复用 Field 的解析逻辑需要一个 Field；采样只有几十行，直接使用 String API
                text.toIntOrNull()?.let { return INT }
                text.toLongOrNull()?.let { return LONG }
                if (isDouble(text)) return DOUBLE
                if (text.equals("true", true) || text.equals("false", true) ||
                    text.equals("yes", true) || text.equals("no", true)
                ) return BOOLEAN
                if (isDate(text)) return DATE
                return STRING
            }

            private fun isDouble(text: String): Boolean {
                var i = 0
                if (text.startsWith('-')) i++
                var digits = 0
                while (i < text.length && text[i] in '0'..'9') {
                    i++
                    digits++
                }
                if (i < text.length && text[i] == '.') {
                    i++
                    while (i < text.length && text[i] in '0'..'9') {
                        i++
                        digits++
                    }
                }
                if (digits == 0) return false
                if (i < text.length && (text[i] == 'e' || text[i] == 'E')) {
                    i++
                    if (i < text.length && (text[i] == '+' || text[i] == '-')) i++
                    var exp = 0
                    while (i < text.length && text[i] in '0'..'9') {
                        i++
                        exp++
                    }
                    if (exp == 0) return false
                }
                return i == text.length
            }

            private fun isDate(text: String): Boolean {
                if (text.length != 10) return false
                return if (text[4] == '-' && text[7] == '-') {
                    text[0].isDigit() && text[1].isDigit() && text[2].isDigit() && text[3].isDigit() &&
                        text[5].isDigit() && text[6].isDigit() && text[8].isDigit() && text[9].isDigit()
                } else if (text[2] == '/' && text[5] == '/') {
                    text[0].isDigit() && text[1].isDigit() && text[3].isDigit() && text[4].isDigit() &&
                        text[6].isDigit() && text[7].isDigit() && text[8].isDigit() && text[9].isDigit()
                } else {
                    false
                }
            }

            fun merge(a: Kind, b: Kind): Kind {
                if (a == UNKNOWN) return b
                if (b == UNKNOWN) return a
                if (a == b) return a
                if (a == STRING || b == STRING) return STRING
                val numeric = setOf(INT, LONG, DOUBLE)
                if (a in numeric && b in numeric) {
                    return when {
                        a == DOUBLE || b == DOUBLE -> DOUBLE
                        a == LONG || b == LONG -> LONG
                        else -> INT
                    }
                }
                return STRING
            }
        }
    }

    private class ColumnBuilder(private var kind: Kind) {
        private var size = 0
        private var capacity = 0
        private var doubles: DoubleArray? = null
        private var longs: LongArray? = null
        private var ints: IntArray? = null
        private var booleans: BooleanArray? = null
        private var objects: ArrayList<Any?>? = null
        private var present: BooleanArray? = null
        private var stringMode = false

        fun addField(field: Field, nullValues: List<String>) {
            if (field.isNullValue(nullValues)) {
                addNull()
                return
            }
            when {
                stringMode -> addObject(field.toStringValue())
                kind == Kind.STRING -> addObject(field.toStringValue())
                kind == Kind.INT -> {
                    val v = field.toIntOrNull()
                    if (v == null) promoteAndAddString(field) else addInt(v)
                }
                kind == Kind.LONG -> {
                    val v = field.toLongOrNull()
                    if (v == null) promoteAndAddString(field) else addLong(v)
                }
                kind == Kind.DOUBLE -> {
                    val v = field.toDoubleOrNull()
                    if (v == null) promoteAndAddString(field) else addDouble(v)
                }
                kind == Kind.BOOLEAN -> {
                    val v = field.toBooleanOrNull()
                    if (v == null) promoteAndAddString(field) else addBoolean(v)
                }
                else -> addObject(field.toStringValue())
            }
        }

        fun addString(text: String?, nullValues: List<String>) {
            if (text == null || text.isEmpty() || text in nullValues) {
                addNull()
                return
            }
            when {
                stringMode -> addObject(text)
                kind == Kind.STRING -> addObject(text)
                kind == Kind.INT -> text.toIntOrNull()?.let { addInt(it) } ?: promoteAndAddString(text)
                kind == Kind.LONG -> text.toLongOrNull()?.let { addLong(it) } ?: promoteAndAddString(text)
                kind == Kind.DOUBLE -> text.toDoubleOrNull()?.let { addDouble(it) } ?: promoteAndAddString(text)
                kind == Kind.BOOLEAN -> {
                    val v = when {
                        text.equals("true", true) || text.equals("yes", true) -> true
                        text.equals("false", true) || text.equals("no", true) -> false
                        else -> null
                    }
                    if (v == null) promoteAndAddString(text) else addBoolean(v)
                }
                else -> addObject(text)
            }
        }

        fun addNull() {
            if (stringMode || kind == Kind.STRING) {
                ensureObjectCapacity()
                objects!!.add(null)
                size++
                return
            }
            ensureCapacity(size + 1)
            val mask = present ?: BooleanArray(capacity) { true }.also { present = it }
            mask[size] = false
            size++
        }

        private fun addObject(value: Any?) {
            ensureObjectCapacity()
            objects!!.add(value)
            size++
        }

        private fun addDouble(value: Double) {
            ensureCapacity(size + 1)
            doubles!![size] = value
            present?.set(size, true)
            size++
        }

        private fun addLong(value: Long) {
            ensureCapacity(size + 1)
            longs!![size] = value
            present?.set(size, true)
            size++
        }

        private fun addInt(value: Int) {
            ensureCapacity(size + 1)
            ints!![size] = value
            present?.set(size, true)
            size++
        }

        private fun addBoolean(value: Boolean) {
            ensureCapacity(size + 1)
            booleans!![size] = value
            present?.set(size, true)
            size++
        }

        private fun ensureCapacity(required: Int) {
            if (required <= capacity) return
            var newCapacity = if (capacity == 0) 64 else capacity
            while (newCapacity < required) newCapacity = newCapacity shl 1
            when (kind) {
                Kind.INT -> ints = ints?.copyOf(newCapacity) ?: IntArray(newCapacity)
                Kind.LONG -> longs = longs?.copyOf(newCapacity) ?: LongArray(newCapacity)
                Kind.DOUBLE -> doubles = doubles?.copyOf(newCapacity) ?: DoubleArray(newCapacity)
                Kind.BOOLEAN -> booleans = booleans?.copyOf(newCapacity) ?: BooleanArray(newCapacity)
                else -> Unit
            }
            present = present?.copyOf(newCapacity)
            capacity = newCapacity
        }

        private fun ensureObjectCapacity() {
            if (objects == null) {
                objects = ArrayList(if (capacity > 0) capacity else 64)
                // 从原始类型切换到对象列时，先把已有值搬过去
                when (kind) {
                    Kind.INT -> for (i in 0 until size) objects!!.add(if (present == null || present!![i]) ints!![i] else null)
                    Kind.LONG -> for (i in 0 until size) objects!!.add(if (present == null || present!![i]) longs!![i] else null)
                    Kind.DOUBLE -> for (i in 0 until size) objects!!.add(if (present == null || present!![i]) doubles!![i] else null)
                    Kind.BOOLEAN -> for (i in 0 until size) objects!!.add(if (present == null || present!![i]) booleans!![i] else null)
                    else -> for (i in 0 until size) objects!!.add(null)
                }
                ints = null
                longs = null
                doubles = null
                booleans = null
                present = null
            }
        }

        private fun promoteAndAddString(field: Field) {
            promoteAndAddString(field.toStringValue())
        }

        private fun promoteAndAddString(value: String) {
            if (!stringMode) {
                ensureObjectCapacity()
                stringMode = true
                kind = Kind.STRING
            }
            objects!!.add(value)
            size++
        }

        fun build(index: List<Any>, name: String): Series<Any> {
            if (stringMode || kind == Kind.STRING) {
                val list = objects ?: ArrayList<Any?>()
                return Series.wrap(list, index, name, AndaTypes.STRING)
            }
            return when (kind) {
                Kind.INT -> {
                    val values = if (capacity == size) ints!! else ints!!.copyOf(size)
                    Series.fromIntArray(values, trimMask(), index, name, AndaTypes.INT32)
                }
                Kind.LONG -> {
                    val values = if (capacity == size) longs!! else longs!!.copyOf(size)
                    Series.fromLongArray(values, trimMask(), index, name, AndaTypes.INT64)
                }
                Kind.DOUBLE -> {
                    val values = if (capacity == size) doubles!! else doubles!!.copyOf(size)
                    Series.fromDoubleArray(values, trimMask(), index, name, AndaTypes.FLOAT64)
                }
                Kind.BOOLEAN -> {
                    val values = if (capacity == size) booleans!! else booleans!!.copyOf(size)
                    Series.fromBooleanArray(values, trimMask(), index, name, AndaTypes.BOOL)
                }
                else -> Series.wrap(objects ?: ArrayList<Any?>(), index, name, AndaTypes.STRING)
            }
        }

        private fun trimMask(): BooleanArray? {
            val mask = present ?: return null
            return if (capacity == size) mask else mask.copyOf(size)
        }
    }
}
