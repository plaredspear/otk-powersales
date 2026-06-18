package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * 진열스케줄마스터 목록 엑셀 export — 목록 화면(`DisplaySchedulePage.tsx` listColumns)과 동일 구성.
 *
 * 현재 검색/프리셋 필터의 가시 범위 전량을 페이징 없이 단일 시트로 출력 (호출 측에서 최대 건수 제한).
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 *
 * 선택 레코드 export([ScheduleExportGenerator]) 와의 차이: 입력이 entity 가 아닌 목록 projection DTO 라
 * 화면 컬럼(지점명·재직상태·거래처유형·거래처상태 포함)과 1:1 정합 + 동일 매핑 재사용.
 */
@Component
class ScheduleListExcelExporter : BaseExcelExporter<ScheduleListItemDto>() {

    override val sheetName = "진열스케줄"
    override val defaultFilename = "진열스케줄.xlsx"

    // 목록 테이블 컬럼 순서 정합 (지점명 ~ 확정). 액션 컬럼은 export 제외.
    override val headers = listOf(
        "지점명", "사번", "성명", "재직상태", "거래처코드", "거래명", "거래처유형",
        "근무형태3", "근무형태5", "시작일", "종료일", "거래처상태", "전월매출", "확정",
    )

    override fun writeRow(row: Row, item: ScheduleListItemDto) {
        row.createCell(0).setCellValue(item.branchName ?: "")
        row.createCell(1).setCellValue(item.employeeCode)
        row.createCell(2).setCellValue(item.employeeName)
        row.createCell(3).setCellValue(item.employmentStatus ?: "")
        row.createCell(4).setCellValue(item.accountCode ?: "")
        row.createCell(5).setCellValue(item.accountName ?: "")
        row.createCell(6).setCellValue(item.accountType ?: "")
        row.createCell(7).setCellValue(item.typeOfWork3 ?: "")
        row.createCell(8).setCellValue(item.typeOfWork5 ?: "")
        row.createCell(9).setCellValue(item.startDate?.format(DATE_FORMAT) ?: "")
        row.createCell(10).setCellValue(item.endDate?.format(DATE_FORMAT) ?: "")
        row.createCell(11).setCellValue(item.accountStatus ?: "")
        val revenue = item.lastMonthRevenue
        if (revenue != null) {
            row.createCell(12).setCellValue(revenue.toDouble())
        } else {
            row.createCell(12).setCellValue("")
        }
        row.createCell(13).setCellValue(if (item.confirmed == true) "확정" else "미확정")
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
