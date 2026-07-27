package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * 진열스케줄마스터 목록 엑셀 export — 목록 화면(`DisplaySchedulePage.tsx` listColumns)과 동일 구성.
 *
 * "검색결과 다운로드"(검색 조건 전량) / "선택 다운로드"(선택 id) **양쪽이 본 exporter 를 공유** 하여
 * 두 다운로드의 컬럼 구성이 서로, 그리고 화면 테이블과 어긋나지 않게 한다.
 * 입력은 entity 가 아닌 목록 projection DTO 라 화면 컬럼(지점명·재직상태·거래처유형·거래처상태 포함)
 * 과 1:1 정합 + 목록 조회와 동일 매핑 재사용.
 *
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 */
@Component
class ScheduleListExcelExporter : BaseExcelExporter<ScheduleListItemDto>() {

    override val sheetName = "진열스케줄"
    override val defaultFilename = "진열스케줄.xlsx"

    // 목록 테이블 컬럼 순서 정합 (유효 ~ 확정). 액션 컬럼은 export 제외.
    // "유효" 는 화면의 신호등(Valid__c 색상) 을 엑셀에서 표현할 수 없어 tooltip 과 동일한
    // 유효데이터(ValidData__c: 유효/예정/종료) 텍스트로 출력한다.
    override val headers = listOf(
        "유효", "지점명", "사번", "성명", "재직상태", "거래처코드", "거래명", "거래처유형",
        "근무형태3", "근무형태5", "시작일", "종료일", "거래처상태", "전월매출", "확정",
    )

    override fun writeRow(row: Row, item: ScheduleListItemDto) {
        row.createCell(0).setCellValue(item.validData ?: "")
        row.createCell(1).setCellValue(item.branchName ?: "")
        row.createCell(2).setCellValue(item.employeeCode)
        row.createCell(3).setCellValue(item.employeeName)
        row.createCell(4).setCellValue(item.employmentStatus ?: "")
        row.createCell(5).setCellValue(item.accountCode ?: "")
        row.createCell(6).setCellValue(item.accountName ?: "")
        row.createCell(7).setCellValue(item.accountType ?: "")
        row.createCell(8).setCellValue(item.typeOfWork3 ?: "")
        row.createCell(9).setCellValue(item.typeOfWork5 ?: "")
        row.createCell(10).setCellValue(item.startDate?.format(DATE_FORMAT) ?: "")
        row.createCell(11).setCellValue(item.endDate?.format(DATE_FORMAT) ?: "")
        row.createCell(12).setCellValue(item.accountStatus ?: "")
        val revenue = item.lastMonthRevenue
        if (revenue != null) {
            row.createCell(13).setCellValue(revenue.toDouble())
        } else {
            row.createCell(13).setCellValue("")
        }
        row.createCell(14).setCellValue(if (item.confirmed == true) "확정" else "미확정")
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
