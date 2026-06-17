package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionListItem
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component

/**
 * 행사마스터 목록 엑셀 export — 목록 화면(`AdminPromotionService.getPromotions`) 컬럼과 동일 구성.
 *
 * 페이징 없이 현재 검색 조건의 가시 범위 전량을 단일 시트로 출력 (호출 측에서 최대 건수 제한).
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 */
@Component
class PromotionListExcelExporter : BaseExcelExporter<PromotionListItem>() {

    override val sheetName = "행사마스터"
    override val defaultFilename = "행사마스터.xlsx"

    // 목록 테이블 컬럼 정합 (행사번호 ~ 작성자).
    override val headers = listOf(
        "행사번호", "행사명", "거래처", "대표제품", "시작일", "종료일", "거래처코드",
        "행사유형", "매대위치", "제품유형", "CC Code", "목표금액", "실적금액(원)",
        "작성 일자", "작성자",
    )

    override fun writeRow(row: Row, item: PromotionListItem) {
        row.createCell(0).setCellValue(item.promotionNumber)
        row.createCell(1).setCellValue(item.promotionName ?: "")
        row.createCell(2).setCellValue(item.accountName ?: "")
        row.createCell(3).setCellValue(item.primaryProductName ?: "")
        row.createCell(4).setCellValue(item.startDate.toString())
        row.createCell(5).setCellValue(item.endDate.toString())
        row.createCell(6).setCellValue(item.accountCode ?: "")
        row.createCell(7).setCellValue(item.promotionType ?: "")
        row.createCell(8).setCellValue(item.standLocation ?: "")
        row.createCell(9).setCellValue(item.category1 ?: "")
        row.createCell(10).setCellValue(item.costCenterCode ?: "")
        row.createCell(11).setCellValue(item.targetAmount ?: 0.0)
        row.createCell(12).setCellValue(item.actualAmount ?: 0.0)
        row.createCell(13).setCellValue(item.createdAt.toString())
        row.createCell(14).setCellValue(item.createdByName ?: "")
    }
}
