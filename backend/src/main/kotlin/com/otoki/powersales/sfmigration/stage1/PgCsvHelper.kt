package com.otoki.powersales.sfmigration.stage1

/**
 * PostgreSQL COPY FROM STDIN (FORMAT csv) 의 값 직렬화 helper.
 *
 * scripts/sf-data-migration/common.kts 의 `pgCsvEscape` / `pgCsvLine` 와 동일 동작.
 *
 *  - null → `\N` (FORMAT csv 의 NULL 토큰)
 *  - 빈 문자열 → `""` (literal empty)
 *  - quote 포함 → `""` escape + 전체 quote
 *  - comma / newline / CR 포함 시 전체 quote
 *  - NUL (U+0000) 은 PG text 컬럼이 거부
 *    (`invalid byte sequence for encoding "UTF8": 0x00`) 하므로 silent strip.
 *    SF export CSV 일부 셀에 잔존하는 경우가 있음 — 의미 없는 노이즈로 간주.
 */
object PgCsvHelper {
    private val NUL: Char = 0.toChar()

    fun escape(value: String?): String {
        if (value == null) return "\\N"
        val sanitized = stripNul(value)
        if (sanitized.isEmpty()) return "\"\""
        val needsQuote = sanitized.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return sanitized
        val sb = StringBuilder(sanitized.length + 4)
        sb.append('"')
        for (c in sanitized) {
            if (c == '"') sb.append('"').append('"') else sb.append(c)
        }
        sb.append('"')
        return sb.toString()
    }

    fun line(values: List<String?>): String {
        val sb = StringBuilder()
        for ((i, v) in values.withIndex()) {
            if (i > 0) sb.append(',')
            sb.append(escape(v))
        }
        sb.append('\n')
        return sb.toString()
    }

    private fun stripNul(value: String): String {
        if (value.indexOf(NUL) < 0) return value
        val sb = StringBuilder(value.length)
        for (c in value) if (c != NUL) sb.append(c)
        return sb.toString()
    }
}
