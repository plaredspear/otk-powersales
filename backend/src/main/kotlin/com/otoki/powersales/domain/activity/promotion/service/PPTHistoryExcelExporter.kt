package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryResponse
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * 전문행사조 이력 목록 엑셀 export — 목록 화면(`PPTHistoryPage.tsx` columns)과 동일 구성.
 *
 * 현재 검색 조건의 가시 범위 전량을 페이징 없이 단일 시트로 출력 (호출 측에서 최대 건수 제한).
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 */
@Component
class PPTHistoryExcelExporter : BaseExcelExporter<PPTMasterHistoryResponse>() {

    override val sheetName = "전문행사조이력"
    override val defaultFilename = "전문행사조이력.xlsx"

    // 목록 테이블 컬럼 순서 정합 (이력번호 ~ 변경 시점 ~ 거래처명 / 거래처코드).
    override val headers = listOf(
        "전문행사조 이력번호", "소속", "사번", "사원", "변경 전", "변경 후", "변경 시점",
        "거래처명", "거래처코드",
    )

    override fun writeRow(row: Row, item: PPTMasterHistoryResponse) {
        row.createCell(0).setCellValue(item.name ?: "")
        row.createCell(1).setCellValue(item.orgName ?: "")
        row.createCell(2).setCellValue(item.employeeCode ?: "")
        row.createCell(3).setCellValue(item.employeeName ?: "")
        row.createCell(4).setCellValue(item.oldValue?.displayName ?: "")
        row.createCell(5).setCellValue(item.newValue?.displayName ?: "")
        row.createCell(6).setCellValue(item.changedAt?.format(DATETIME_FORMAT) ?: "")
        row.createCell(7).setCellValue(item.accountName ?: "")
        row.createCell(8).setCellValue(item.accountCode ?: "")
    }

    companion object {
        private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
