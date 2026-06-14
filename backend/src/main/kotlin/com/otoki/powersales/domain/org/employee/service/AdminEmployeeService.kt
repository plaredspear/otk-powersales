package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
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
     * 사원 목록 조회 — SF 레거시 화면별 지점 스코프 정합.
     *
     * SF Employee(`DKRetail__Employee__c`) READ 를 부여하는 PermissionSet 6개는 모두
     * `viewAllRecords=true` 라 객체/DB 레이어에서는 전사 row 접근이 가능하다. 따라서 SF 화면의 실효
     * 가시 범위는 **화면별 컨트롤러 SOQL 의 지점 필터 유무에 따라 갈린다** — 본 메서드를 공유하는
     * 호출 화면마다 [applyBranchScope] 로 제어한다(SF 레거시 화면별 동작과 1:1 정합).
     *
     * - **본인 지점 스코프** (`applyBranchScope = true`): 여사원 현황, 진열사원 스케줄 사원 lookup.
     *   SF `SalesMemberListController` / `TeamMemberListController` / `ManageScheduleComponent` /
     *   `UplExcelSchduleMaster` 가 `CostCenterCode__c IN <본인 소속 지점/조직>` SOQL 로 본인 지점에
     *   한정(영업부장=부 단위 / 지점장·여사원=본인 지점).
     * - **전사** (`applyBranchScope = false`, 기본): 사원 목록/권한 관리(표준 리스트뷰 `filterScope=Everything`),
     *   행사 사원 그리드 lookup(`RelatedListDataGridController` 지점필터 없음), 거래처 담당자 lookup
     *   (SF 대화형 lookup 부재), 유통기한 사원 lookup(SF 매핑 없는 Heroku 단독). SF 가 지점으로 좁히지
     *   않으므로 전사 검색을 유지한다.
     *
     * [costCenterCode] 요청 파라미터는 사용자가 검색 드롭다운에서 특정 지점을 고른 경우의 표시 필터 —
     * `applyBranchScope = true` 일 때는 scope 의 지점 권한 범위 안에서만 유효(권한 밖 지점 요청 시
     * [EffectiveBranchResult.NoAccess] → 빈 결과). `applyBranchScope = false` 면 보안축 없이 순수
     * 표시 필터로 동작.
     */
    fun getEmployees(
        scope: DataScope,
        status: String?,
        costCenterCode: String?,
        keyword: String?,
        role: String?,
        page: Int,
        size: Int,
        applyBranchScope: Boolean = false,
    ): EmployeeListResponse {
        val requestedBranch = costCenterCode?.takeIf { it.isNotBlank() }
        val branchFilter: List<String>? = if (applyBranchScope) {
            when (val result = scope.effectiveBranchCodes(requestedBranch)) {
                // 전사 권한 (SYSTEM_ADMIN / 영업지원·본부) — 지점 보안 필터 없음
                is EffectiveBranchResult.All -> null
                // 본인 소속 지점(또는 그 안에서 선택한 단일 지점) 으로 제한
                is EffectiveBranchResult.Filtered -> result.codes
                // 권한 밖 지점 요청 — 빈 결과
                is EffectiveBranchResult.NoAccess -> return EmployeeListResponse(
                    content = emptyList(), page = page, size = size, totalElements = 0, totalPages = 0
                )
            }
        } else {
            // 전사 검색 — costCenterCode 는 사용자 표시 필터로만 전달(보안축 아님)
            requestedBranch?.let { listOf(it) }
        }

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
