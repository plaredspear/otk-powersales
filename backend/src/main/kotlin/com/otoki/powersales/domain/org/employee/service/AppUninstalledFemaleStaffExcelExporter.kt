package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.dto.response.AppUninstalledFemaleStaffItem
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component

/**
 * 앱 미설치 추정 여사원 명단 엑셀 export — 설치 안내 대상 배포용.
 *
 * 컬럼은 안내에 필요한 최소 3개(사번 / 이름 / 지점명). 워크북 생성 / 헤더 스타일 / 직렬화는
 * [BaseExcelExporter] 가 담당하고 본 클래스는 컬럼 정의만 책임진다.
 */
@Component
class AppUninstalledFemaleStaffExcelExporter : BaseExcelExporter<AppUninstalledFemaleStaffItem>() {

    override val sheetName = "앱미설치여사원"
    override val defaultFilename = "앱미설치여사원.xlsx"

    override val headers = listOf("사번", "이름", "지점명")

    override fun writeRow(row: Row, item: AppUninstalledFemaleStaffItem) {
        row.createCell(0).setCellValue(item.employeeCode)
        row.createCell(1).setCellValue(item.name)
        row.createCell(2).setCellValue(item.branchName ?: "")
    }
}
