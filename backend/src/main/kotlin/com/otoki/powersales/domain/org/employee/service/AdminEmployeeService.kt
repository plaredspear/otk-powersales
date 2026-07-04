package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.schedule.repository.LatestAttendanceInfo
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminEmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val employeeListExcelExporter: EmployeeListExcelExporter,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val organizationRepository: OrganizationRepository
) {

    companion object {
        /** 검색결과 전체 엑셀 export 최대 건수 (초과분 잘라냄 — 타 도메인 export 정합). */
        private const val EXPORT_MAX_ROWS = 50_000
    }

    /**
     * 여사원 현황 조회/엑셀 공통 필터 — 문자열 요청 파라미터를 repository 술어용 값으로 파싱한 결과.
     * 근무형태(1/3)는 최근 출근등록 1건 기준, 전문행사조는 사원 자체 필드 기준.
     */
    private data class EmployeeSearchFilters(
        val workType1: WorkingCategory1?,
        val workType3: WorkingCategory3?,
        val promotionTeam: ProfessionalPromotionTeamType?,
        val promotionTeamGeneral: Boolean,
    )

    /**
     * 근무형태1/근무형태3/전문행사조 문자열 필터를 파싱. 유효하지 않은 근무형태 값은 [IllegalArgumentException].
     * 전문행사조는 '일반'(미배정) 을 [ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME] 로 받아 IS NULL 필터로 변환하고,
     * 그 외 유효하지 않은 값은 매칭 실패로 간주해 무시(빈 결과가 아닌 미적용)하지 않고 명시적으로 예외 처리한다.
     */
    private fun parseSearchFilters(
        workType1: String?,
        workType3: String?,
        professionalPromotionTeam: String?,
    ): EmployeeSearchFilters {
        val wt1 = workType1?.takeIf { it.isNotBlank() }?.let {
            WorkingCategory1.fromDisplayNameOrNull(it)
                ?: throw IllegalArgumentException("유효하지 않은 근무형태1: $it")
        }
        val wt3 = workType3?.takeIf { it.isNotBlank() }?.let {
            WorkingCategory3.fromDisplayNameOrNull(it)
                ?: throw IllegalArgumentException("유효하지 않은 근무형태3: $it")
        }
        val pptRaw = professionalPromotionTeam?.takeIf { it.isNotBlank() }
        val general = pptRaw == ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME
        val ppt = if (general) null else pptRaw?.let {
            ProfessionalPromotionTeamType.fromDisplayNameOrNull(it)
                ?: throw IllegalArgumentException("유효하지 않은 전문행사조: $it")
        }
        return EmployeeSearchFilters(wt1, wt3, ppt, general)
    }

    /**
     * 사원 목록 화면 지점 셀렉터 옵션 — 전 지점(전사) 목록.
     *
     * 사원 목록([getEmployees]) 은 SF 표준 리스트뷰(`filterScope=Everything`) 정합으로 전사 조회이며
     * `costCenterCode` 는 보안축이 아닌 순수 표시 필터다. 따라서 옵션도 목록 스코프와 일치하도록
     * 권한별 화이트리스트([WomenScheduleBranchResolver])가 아닌 전사 지점 목록을 반환한다
     * (거래처/여사원 화면과 다른 지점). [findAllTeamScheduleBranches] 는 Redis 캐시된 무인자 조회다.
     */
    fun getBranchOptions(): List<BranchResponse> =
        organizationRepository.findAllTeamScheduleBranches()

    /**
     * 현재 페이지 사원들의 최근 출근등록 1건 정보(근무형태/근무거래처) Map<employeeId, info> 조회.
     * 출근등록 이력 0건 사원은 키가 없다.
     */
    private fun loadAttendanceInfo(employeeIds: List<Long>): Map<Long, LatestAttendanceInfo> {
        if (employeeIds.isEmpty()) return emptyMap()
        return teamMemberScheduleRepository.findLatestAttendanceInfoByEmployeeIds(employeeIds)
    }

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
        role: String? = null,
        page: Int,
        size: Int,
        applyBranchScope: Boolean = false,
        // 여러 직책을 함께 노출하는 화면용 (여사원 현황 = 여사원 + 조장). null 이면 [role] 단일 필터만 적용.
        roles: List<String>? = null,
        // 근무형태(최근 출근등록 1건 기준) / 전문행사조 필터. blank/null 이면 미적용.
        workType1: String? = null,
        workType3: String? = null,
        professionalPromotionTeam: String? = null,
    ): EmployeeListResponse {
        val filters = parseSearchFilters(workType1, workType3, professionalPromotionTeam)
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
        val userPage = employeeRepository.findEmployees(
            status, branchFilter, keyword, role, roles,
            filters.workType1, filters.workType3, filters.promotionTeam, filters.promotionTeamGeneral,
            pageable,
        )

        // 만나이 / 근속년수 계산 기준일 — 페이지 전체에 동일 적용
        val today = LocalDate.now()
        // 근무형태/근무거래처 — 현재 페이지 사원들의 최근 출근등록 1건을 조회 (N+1 없음)
        val attendanceInfo = loadAttendanceInfo(userPage.content.map { it.id })
        return EmployeeListResponse(
            content = userPage.content.map { emp ->
                val info = attendanceInfo[emp.id]
                EmployeeListItem.from(emp, today, info?.workingCategory1, info?.workingCategory3, info?.accountName, info?.accountCode)
            },
            page = page,
            size = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages
        )
    }

    /**
     * 사원 목록 엑셀 export — 목록 화면([getEmployees])과 동일한 지점 스코프/필터로 전량 추출.
     *
     * 페이징 없이 [EXPORT_MAX_ROWS] 단일 페이지로 조회 (초과분 잘라냄 — 타 도메인 export 정합).
     * `applyBranchScope = true` + 권한 밖 지점 요청(NoAccess)은 쿼리 없이 헤더만 있는 빈 엑셀을 반환한다.
     * 목록과 동일한 [EmployeeListItem] 매핑 후 [EmployeeListExcelExporter] 로 위임.
     */
    fun exportEmployees(
        scope: DataScope,
        status: String?,
        costCenterCode: String?,
        keyword: String?,
        role: String? = null,
        applyBranchScope: Boolean = false,
        // 여러 직책을 함께 노출하는 화면용 (여사원 현황 = 여사원 + 조장). null 이면 [role] 단일 필터만 적용.
        roles: List<String>? = null,
        // 근무형태(최근 출근등록 1건 기준) / 전문행사조 필터. blank/null 이면 미적용.
        workType1: String? = null,
        workType3: String? = null,
        professionalPromotionTeam: String? = null,
    ): ExcelResult {
        val filters = parseSearchFilters(workType1, workType3, professionalPromotionTeam)
        val requestedBranch = costCenterCode?.takeIf { it.isNotBlank() }
        val noAccess: Boolean
        val branchFilter: List<String>? = if (applyBranchScope) {
            when (val result = scope.effectiveBranchCodes(requestedBranch)) {
                is EffectiveBranchResult.All -> { noAccess = false; null }
                is EffectiveBranchResult.Filtered -> { noAccess = false; result.codes }
                is EffectiveBranchResult.NoAccess -> { noAccess = true; null }
            }
        } else {
            noAccess = false
            requestedBranch?.let { listOf(it) }
        }

        val items = if (noAccess) {
            emptyList()
        } else {
            val pageable = PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by("name").ascending())
            val today = LocalDate.now()
            val employees = employeeRepository.findEmployees(
                status, branchFilter, keyword, role, roles,
                filters.workType1, filters.workType3, filters.promotionTeam, filters.promotionTeamGeneral,
                pageable,
            ).content
            val attendanceInfo = loadAttendanceInfo(employees.map { it.id })
            employees.map { emp ->
                val info = attendanceInfo[emp.id]
                EmployeeListItem.from(emp, today, info?.workingCategory1, info?.workingCategory3, info?.accountName, info?.accountCode)
            }
        }

        val timestamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return employeeListExcelExporter.export(items, "여사원현황_${timestamp}.xlsx")
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
