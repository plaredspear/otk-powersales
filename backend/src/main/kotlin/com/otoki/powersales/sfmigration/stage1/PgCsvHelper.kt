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
 */
object PgCsvHelper {
    fun escape(value: String?): String {
        if (value == null) return "\\N"
        if (value.isEmpty()) return "\"\""
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return value
        val sb = StringBuilder(value.length + 4)
        sb.append('"')
        for (c in value) {
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
}
