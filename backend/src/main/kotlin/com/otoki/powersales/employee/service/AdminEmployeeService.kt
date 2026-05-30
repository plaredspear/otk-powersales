package com.otoki.powersales.employee.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.employee.dto.response.EmployeeListItem
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminEmployeeService(
    private val employeeRepository: EmployeeRepository
) {

    /**
     * 사원 목록 조회 — SF `DKRetail__Employee__c` 가시 모델 정합.
     *
     * SF 의 Employee 는 **이진 가시 모델**: Employee object READ 를 부여하는 PermissionSet 이 예외 없이
     * `viewAllRecords=true` 를 동반(READ-without-ViewAll 0건)하고 어떤 Profile 도 Employee 접근을
     * 부여하지 않는다. 즉 READ 게이트(`@RequiresSfPermission(employee, READ)`)를 통과한 사용자는
     * 전사 전 사원을 보고, 미통과면 접근 불가 — owner / role hierarchy / 지점(branch) 축이 가시 범위에
     * 전혀 개입하지 않는다. 따라서 [scope] 의 지점 보안축을 적용하지 않는다(게이트가 이미 전사 접근 보장).
     *
     * [costCenterCode] 요청 파라미터는 SF listView 의 컬럼 기반 표시 필터에 해당 — 사용자가 검색
     * 드롭다운에서 특정 지점을 고른 경우 그 지점만 표시하는 순수 표시 필터로 동작한다(보안축 아님).
     *
     * @param scope controller `@CurrentDataScope` 호환을 위해 받되, SF 이진 모델이라 가시 필터링에 미사용.
     */
    @Suppress("UNUSED_PARAMETER")
    fun getEmployees(
        scope: DataScope,
        status: String?,
        costCenterCode: String?,
        keyword: String?,
        role: String?,
        page: Int,
        size: Int
    ): EmployeeListResponse {
        // SF 이진 모델: 게이트 통과 = 전사. costCenterCode 는 사용자 표시 필터로만 전달(보안축 아님).
        val branchFilter: List<String>? = costCenterCode?.takeIf { it.isNotBlank() }?.let { listOf(it) }

        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val userPage = employeeRepository.findEmployees(status, branchFilter, keyword, role, pageable)

        // 만나이 / 근속년수 계산 기준일 — 페이지 전체에 동일 적용
        val today = LocalDate.now()
        return EmployeeListResponse(
            content = userPage.content.map { EmployeeListItem.from(it, today) },
            page = page,
            size = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages
        )
    }

    /**
     * 사원 상세 조회 — 6개 그룹 (인사·조직·직무·연락처·앱 설정·근무) 의 모든 필드 노출.
     *
     * 레거시 SF 표준 레코드 상세 페이지 동등. employee_info join 으로 단말/비밀번호 변경 필요 여부 등도 함께 로드.
     */
    fun getEmployee(employeeId: Long): EmployeeDetailResponse {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException(employeeId)
        return EmployeeDetailResponse.from(employee)
    }
}
