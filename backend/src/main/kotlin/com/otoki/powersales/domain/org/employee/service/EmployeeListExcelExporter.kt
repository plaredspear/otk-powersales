package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component

/**
 * 여사원 현황 목록 엑셀 export — 목록 화면(`EmployeePage.tsx` columns)과 동일 구성.
 *
 * 현재 검색 조건의 가시 범위 전량을 페이징 없이 단일 시트로 출력 (호출 측에서 최대 건수 제한).
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 * 화면의 "계정 관리" 액션 컬럼(단말/비밀번호 초기화 버튼)은 데이터가 아니므로 export 제외.
 */
@Component
class EmployeeListExcelExporter : BaseExcelExporter<EmployeeListItem>() {

    override val sheetName = "여사원현황"
    override val defaultFilename = "여사원현황.xlsx"

    // 목록 테이블 컬럼 순서 정합 (사번 ~ 앱활성). 화면과 동일하게 성별·지점코드 미포함, 전문행사조 포함.
    override val headers = listOf(
        "사번", "이름", "상태", "소속", "전문행사조", "권한",
        "직종명", "직책", "직위", "직급", "이메일(회사)", "전화번호(HP)",
        "발령일", "발령명", "입사일", "퇴사일", "만나이", "근속년수", "앱활성",
    )

    override fun writeRow(row: Row, item: EmployeeListItem) {
        row.createCell(0).setCellValue(item.employeeCode ?: "")
        row.createCell(1).setCellValue(item.name)
        row.createCell(2).setCellValue(item.status ?: "")
        row.createCell(3).setCellValue(item.orgName ?: "")
        row.createCell(4).setCellValue(item.professionalPromotionTeam)
        row.createCell(5).setCellValue(item.role ?: "")
        row.createCell(6).setCellValue(item.jikjong ?: "")
        row.createCell(7).setCellValue(item.jikchak ?: "")
        row.createCell(8).setCellValue(item.jikwee ?: "")
        row.createCell(9).setCellValue(item.jikgub ?: "")
        row.createCell(10).setCellValue(item.workEmail ?: "")
        row.createCell(11).setCellValue(item.phone ?: "")
        row.createCell(12).setCellValue(item.appointmentDate ?: "")
        row.createCell(13).setCellValue(item.ordDetailNode ?: "")
        row.createCell(14).setCellValue(item.startDate ?: "")
        // 재직 중인 사원은 퇴사일을 표시하지 않는다 (화면 정합).
        row.createCell(15).setCellValue(if (item.status == "재직") "" else (item.endDate ?: ""))
        row.createCell(16).setCellValue(item.age ?: "")
        row.createCell(17).setCellValue(item.yearsOfService ?: "")
        row.createCell(18).setCellValue(if (item.appLoginActive == true) "활성" else "비활성")
    }
}
