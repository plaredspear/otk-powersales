package com.otoki.powersales.platform.common.util.excel

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/**
 * 단순 표 형태 엑셀 export 공통 베이스 — 헤더 1행 + 데이터 N행 구조.
 *
 * 서브클래스는 [headers] / [sheetName] / [filename] / [writeRow] 4가지만 구현하면
 * 워크북 생성 / 헤더 스타일 / freeze pane / autoSize / 바이트 직렬화 / [ExcelResult] 래핑을 자동 처리한다.
 *
 * 그룹 소계·이미지 임베드·동적 컬럼 등 비정형 export 는 본 베이스를 쓰지 않고
 * [ExcelStyleSupport] 유틸만 직접 사용한다.
 */
abstract class BaseExcelExporter<T> {

    /** 헤더(1행) 컬럼명 — 순서가 곧 컬럼 인덱스. */
    protected abstract val headers: List<String>

    /** 시트명. */
    protected abstract val sheetName: String

    /** 다운로드 파일명 (확장자 포함). 인자 기반 동적 파일명이 필요하면 export 오버로드에서 별도 처리. */
    protected abstract val filename: String

    /** 아이템 1건 → 행. `row.createCell(idx).setCellValue(...)` 로 [headers] 순서대로 채운다. */
    protected abstract fun writeRow(row: Row, item: T)

    /** 헤더 행 아래를 freeze 할지 (기본 true). */
    protected open val freezeHeader: Boolean = true

    fun export(items: List<T>): ExcelResult {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(sheetName)
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, name ->
            headerRow.createCell(i).apply {
                setCellValue(name)
                cellStyle = headerStyle
            }
        }
        if (freezeHeader) sheet.createFreezePane(0, 1)

        items.forEachIndexed { index, item ->
            writeRow(sheet.createRow(index + 1), item)
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        return ExcelResult(ExcelStyleSupport.workbookToBytes(workbook), filename)
    }
}
