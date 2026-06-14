package com.otoki.powersales.domain.org.employee.dto.response

import com.otoki.powersales.domain.org.employee.entity.Employee
import java.time.LocalDate

data class EmployeeListResponse(
    val content: List<EmployeeListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class EmployeeListItem(
    val id: Long,
    val employeeCode: String?,
    val name: String,
    val status: String?,
    val gender: String?,
    val orgName: String?,
    val costCenterCode: String?,
    val role: String?,
    val startDate: String?,
    val endDate: String?,
    val appLoginActive: Boolean?,
    val workPhone: String?,
    val jikchak: String?,
    val jikwee: String?,
    val jikgub: String?,
    val jobCode: String?,
    val appointmentDate: String?,
    val ordDetailNode: String?,
    // SF 여사원 리스트뷰 정합 컬럼 (직종명 / 회사 이메일 / 휴대전화 / 만나이 / 근속년수)
    val jikjong: String?,
    val workEmail: String?,
    val phone: String?,
    val age: String?,
    val yearsOfService: String?
) {
    companion object {
        fun from(employee: Employee, today: LocalDate): EmployeeListItem = EmployeeListItem(
            id = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name,
            status = employee.status,
            gender = employee.gender?.displayName,
            orgName = employee.orgName,
            costCenterCode = employee.costCenterCode,
            role = employee.role,
            startDate = employee.startDate?.toString(),
            endDate = employee.endDate?.toString(),
            appLoginActive = employee.appLoginActive,
            workPhone = employee.workPhone,
            jikchak = employee.jikchak,
            jikwee = employee.jikwee,
            jikgub = employee.jikgub,
            jobCode = employee.jobCode,
            appointmentDate = employee.appointmentDate?.toString(),
            ordDetailNode = employee.ordDetailNode,
            jikjong = employee.jikjong,
            workEmail = employee.workEmail,
            phone = employee.phone,
            age = employee.calculateAge(today),
            yearsOfService = employee.calculateYearsOfService(today)
        )
    }
}

/**
 * 사원 상세 조회 응답 — 레거시 SF 표준 레코드 상세 페이지의 6개 그룹 (인사·조직·직무·연락처·앱 설정·근무) 을 반영.
 *
 * `origin = MANUAL` 사원은 web admin 에서 수정 가능 / `origin = SAP` 사원은 SAP 인입으로만 갱신.
 */
data class EmployeeDetailResponse(
    // -- 인사 정보 --
    val id: Long,
    val employeeCode: String?,
    val name: String,
    val gender: String?,
    val status: String?,
    val birthDate: String?,
    val startDate: String?,
    val endDate: String?,
    val appointmentDate: String?,
    val origin: String?,

    // -- 조직 정보 --
    val costCenterCode: String?,
    val orgName: String?,
    val locationCode: String?,
    val workArea: String?,

    // -- 직무 정보 --
    val jobCode: String?,
    val jikjong: String?,
    val jikwee: String?,
    val jikchak: String?,
    val jikgub: String?,
    val workType: String?,
    val ordDetailNode: String?,

    // -- 연락처 --
    val phone: String?,
    val homePhone: String?,
    val workPhone: String?,
    val officePhone: String?,
    val workEmail: String?,
    val email: String?,

    // -- 앱 설정 --
    val role: String?,
    val appLoginActive: Boolean?,
    val lockingFlag: Boolean?,
    val professionalPromotionTeam: String?,
    val agreementFlag: Boolean?,
    // 사용자가 마지막 로그인/리프레시 때 보고한 현재 사용 앱 버전 (미보고 시 null).
    val appVersionName: String?,
    val appVersionCode: Long?,
    val appPlatform: String?,
    val appVersionSeenAt: String?,

    // -- 근무 정보 --
    val crmWorkType: String?,
    val crmWorkStartDate: String?,
    val totalAnnualLeave: String?,
    val usedAnnualLeave: String?
) {
    companion object {
        fun from(employee: Employee): EmployeeDetailResponse = EmployeeDetailResponse(
            id = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name,
            gender = employee.gender?.displayName,
            status = employee.status,
            birthDate = employee.birthDate,
            startDate = employee.startDate?.toString(),
            endDate = employee.endDate?.toString(),
            appointmentDate = employee.appointmentDate?.toString(),
            origin = employee.origin?.name,
            costCenterCode = employee.costCenterCode,
            orgName = employee.orgName,
            locationCode = employee.locationCode,
            workArea = employee.workArea,
            jobCode = employee.jobCode,
            jikjong = employee.jikjong,
            jikwee = employee.jikwee,
            jikchak = employee.jikchak,
            jikgub = employee.jikgub,
            workType = employee.workType,
            ordDetailNode = employee.ordDetailNode,
            phone = employee.phone,
            homePhone = employee.homePhone,
            workPhone = employee.workPhone,
            officePhone = employee.officePhone,
            workEmail = employee.workEmail,
            email = employee.email,
            role = employee.role,
            appLoginActive = employee.appLoginActive,
            lockingFlag = employee.lockingFlag,
            professionalPromotionTeam = employee.professionalPromotionTeam?.name,
            agreementFlag = employee.agreementFlag,
            appVersionName = employee.appVersionName,
            appVersionCode = employee.appVersionCode,
            appPlatform = employee.appPlatform,
            appVersionSeenAt = employee.appVersionSeenAt?.toString(),
            crmWorkType = employee.crmWorkType?.name,
            crmWorkStartDate = employee.crmWorkStartDate?.toString(),
            totalAnnualLeave = employee.totalAnnualLeave?.toPlainString(),
            usedAnnualLeave = employee.usedAnnualLeave?.toPlainString()
        )
    }
}
