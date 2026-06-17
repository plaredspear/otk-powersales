package com.otoki.powersales.platform.common.util.excel

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream

/** 엑셀(xlsx) MIME 타입 — 컨트롤러 응답 Content-Type 공통값. */
const val XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/**
 * 엑셀 생성 결과 (바이트 + 파일명) — 모든 exporter 공통 반환 타입.
 *
 * `equals`/`hashCode` 는 byte array content 기준 (data class 기본 구현은 참조 비교라 테스트에서 오동작).
 */
data class ExcelResult(
    val bytes: ByteArray,
    val filename: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ExcelResult
        return bytes.contentEquals(other.bytes) && filename == other.filename
    }

    override fun hashCode(): Int = bytes.contentHashCode() * 31 + filename.hashCode()
}

/**
 * 엑셀 헤더 스타일/직렬화 공통 유틸 — 각 exporter 가 중복 정의하던 createHeaderStyle / workbookToBytes 통합.
 */
object ExcelStyleSupport {

    /** 표준 헤더 스타일 — 남색 배경(#1E2F97) + 흰색 볼드 + 가운데 정렬. (기존 다수 exporter 정합) */
    fun primaryHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle = workbook.createCellStyle().apply {
        setFillForegroundColor(XSSFColor(byteArrayOf(0x1E, 0x2F, 0x97.toByte()), null))
        fillPattern = FillPatternType.SOLID_FOREGROUND
        alignment = HorizontalAlignment.CENTER
        setFont(workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        })
    }

    /** 워크북 → ByteArray 직렬화 + close. */
    fun workbookToBytes(workbook: XSSFWorkbook): ByteArray =
        ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }
}
