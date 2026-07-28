package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterMeta
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterOption
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterType
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFormMetaResponse
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFormOption
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeListDefaults
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeListMetaResponse
import com.otoki.powersales.domain.org.employee.enums.EmploymentStatus
import com.otoki.powersales.domain.org.employee.enums.FemaleStaffJobCode
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.schedule.repository.LatestAttendanceInfo
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
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
    private val organizationRepository: OrganizationRepository,
    private val branchCodeExpander: BranchCodeExpander,
) {

    companion object {
        /** 검색결과 전체 엑셀 export 최대 건수 (초과분 잘라냄 — 타 도메인 export 정합). */
        private const val EXPORT_MAX_ROWS = 50_000

        /**
         * 여사원 현황 목록 기본 페이지 크기 / 정렬 — [getEmployees] 의 실제 동작과 일치시킨다.
         * 정렬은 `Sort.by("name").ascending()` 고정이며, 서버가 단일 출처로 화면에 알린다.
         */
        private const val FEMALE_EMPLOYEE_LIST_DEFAULT_PAGE_SIZE = 20
        private const val FEMALE_EMPLOYEE_LIST_DEFAULT_SORT = "name,ASC"

        /**
         * 재직상태 필터 옵션 — [EmploymentStatus] 에서 파생 (기존 web 하드코딩을 서버 단일 출처로 이전).
         *
         * `employee.status` 는 SF `DKRetail__Status__c` 가 free-form string 이라 컬럼 타입이 `String?` 이고,
         * [EmploymentStatus] 는 그 표준 값을 `code` 로 보유한다. enum 선언 순서는 재직/퇴직/휴직 이지만
         * 화면 노출은 재직/휴직/퇴직 순이라 표시 순서를 여기서 명시한다.
         */
        private val EMPLOYEE_STATUS_OPTIONS = listOf(
            EmploymentStatus.ACTIVE,
            EmploymentStatus.ON_LEAVE,
            EmploymentStatus.RESIGNED,
        ).map { it.code }

        /**
         * 권한(role) 폼 옵션 — SF `DKRetail__AppAuthority__c` picklist 4종.
         *
         * [AppAuthority] 는 상수 object 라 선언 순서/라벨을 담지 않으므로 표시 순서와 라벨을 여기서 명시한다.
         * value 는 SF picklist raw value(= DB 저장값) 라 변경 불가. `AccountViewAll` 은 전체 거래처 조회
         * 권한(영업부장) 이라 영문 raw value 만으로는 운영자가 의미를 알기 어려워 label 에 한글을 병기한다.
         */
        private val EMPLOYEE_ROLE_FORM_OPTIONS = listOf(
            AppAuthority.WOMAN to AppAuthority.WOMAN,
            AppAuthority.LEADER to AppAuthority.LEADER,
            AppAuthority.BRANCH_MANAGER to AppAuthority.BRANCH_MANAGER,
            AppAuthority.ACCOUNT_VIEW_ALL to "영업부장 (${AppAuthority.ACCOUNT_VIEW_ALL})",
        )
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
        // "행사조 전체" — 일반(미배정) 을 제외한, 전문행사조가 배정된 모든 사원.
        val promotionTeamAssignedOnly: Boolean,
        // 직무(판촉직/OSC직) — OSC직은 구 명칭 '레이디직' 을 포함한 집합. null 이면 미적용.
        val jobCodes: Set<String>?,
    )

    /**
     * 근무형태1/근무형태3/전문행사조/직무 문자열 필터를 파싱. 유효하지 않은 근무형태 값은 [IllegalArgumentException].
     * 전문행사조는 '일반'(미배정) 을 [ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME] 로 받아 IS NULL 필터로 변환하고,
     * 그 외 유효하지 않은 값은 매칭 실패로 간주해 무시(빈 결과가 아닌 미적용)하지 않고 명시적으로 예외 처리한다.
     *
     * 직무는 [FemaleStaffJobCode.matchingCodesOrNull] 로 매칭 집합을 산출한다 — 'OSC직' 선택 시
     * 구 명칭 '레이디직' 이 함께 포함되어, 대시보드 인원현황 도넛의 OSC 세그먼트와 모수가 일치한다.
     */
    private fun parseSearchFilters(
        workType1: String?,
        workType3: String?,
        professionalPromotionTeam: String?,
        jobCode: String? = null,
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
        val assignedOnly = pptRaw == ProfessionalPromotionTeamType.ASSIGNED_ONLY_DISPLAY_NAME
        val ppt = if (general || assignedOnly) null else pptRaw?.let {
            ProfessionalPromotionTeamType.fromDisplayNameOrNull(it)
                ?: throw IllegalArgumentException("유효하지 않은 전문행사조: $it")
        }
        val jobCodes = jobCode?.takeIf { it.isNotBlank() }?.let {
            FemaleStaffJobCode.matchingCodesOrNull(it)
                ?: throw IllegalArgumentException("유효하지 않은 직무: $it")
        }
        return EmployeeSearchFilters(wt1, wt3, ppt, general, assignedOnly, jobCodes)
    }

    /**
     * 근무형태(1/3) 필터가 걸린 경우, '최근 출근등록 1건이 조건과 일치하는' employee_id 집합을 미리 산출한다.
     * 목록 쿼리(findEmployees)는 이 집합을 `employee.id IN (...)` 로 필터한다 — employee 목록 쿼리에
     * 상관 서브쿼리를 붙이던 방식(전건 조회 timeout)을 제거하기 위해 조회를 2단계로 분리한다.
     *
     * @return 근무형태 필터가 없으면 null(필터 미적용). 있으면 매칭 employee_id 집합(빈 집합 = 일치 0명).
     */
    private fun resolveWorkTypeMatchedEmployeeIds(filters: EmployeeSearchFilters): Set<Long>? {
        if (filters.workType1 == null && filters.workType3 == null) return null
        // 근무형태 컬럼은 displayName 문자열로 저장 — native DISTINCT ON 쿼리에 문자열로 전달.
        return teamMemberScheduleRepository.findEmployeeIdsByLatestWorkType(
            filters.workType1?.displayName,
            filters.workType3?.displayName,
        ).toHashSet()
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
     * 여사원 현황 목록 조회 조건 중 **권한 무관 정적 부분** — 재직상태 / 근무형태1 / 근무형태3 /
     * 전문행사조 옵션 + 목록 기본값.
     *
     * 권한 의존 지점(costCenterCode) 옵션은 호출자(controller)가
     * [com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver] 결과로 조립해 붙인다
     * (셀렉터-조회 스코프 동일 출처). 행사마스터 `getPromotionListMetaStatic` 과 동일한 역할 분담.
     *
     * 각 옵션의 value 는 [getEmployees] 가 그대로 받는 요청 파라미터 값(한글 displayName)이며,
     * "전체" 를 뜻하는 빈 값 선택지는 서버가 내리지 않는다(화면이 표시 문구와 함께 앞에 붙인다).
     */
    fun getFemaleEmployeeListMetaStatic(): FemaleEmployeeListMetaResponse {
        val statusOptions = EMPLOYEE_STATUS_OPTIONS
            .map { FemaleEmployeeFilterOption(value = it, label = it) }

        val workType1Options = WorkingCategory1.entries
            .map { FemaleEmployeeFilterOption(value = it.displayName, label = it.displayName) }

        val workType3Options = WorkingCategory3.entries
            .map { FemaleEmployeeFilterOption(value = it.displayName, label = it.displayName) }

        // 전문행사조 필터는 '행사조 전체'(일반 제외) + 정식 5개 조 + '일반'(미배정, enum 값 아님) 을 함께 노출한다.
        // 셀렉터의 빈 값('') = 완전 전체(일반 포함) 는 화면이 앞에 붙이고, 그 다음에 이 옵션들이 온다.
        // '행사조 전체'(일반 제외) 를 맨 앞에, '일반' 은 SF 레거시 표시 정합으로 그 다음에 둔다.
        val promotionTeamOptions = listOf(
            FemaleEmployeeFilterOption(
                value = ProfessionalPromotionTeamType.ASSIGNED_ONLY_DISPLAY_NAME,
                label = ProfessionalPromotionTeamType.ASSIGNED_ONLY_DISPLAY_NAME,
            ),
            FemaleEmployeeFilterOption(
                value = ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME,
                label = ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME,
            ),
        ) + ProfessionalPromotionTeamType.entries
            .map { FemaleEmployeeFilterOption(value = it.displayName, label = it.displayName) }

        // 직무 필터 — 대시보드 "판촉직/OSC직 인원현황" 도넛과 동일한 2분류.
        // 구 명칭 '레이디직' 은 별도 선택지로 노출하지 않고 'OSC직' 선택 시 함께 조회된다
        // ([FemaleStaffJobCode.OSC_CODES]) — 도넛의 OSC 세그먼트 합산 방식과 정합.
        val jobCodeOptions = listOf(FemaleStaffJobCode.PROMOTION, FemaleStaffJobCode.OSC)
            .map { FemaleEmployeeFilterOption(value = it.code, label = it.code) }

        val filters = listOf(
            FemaleEmployeeFilterMeta("status", FemaleEmployeeFilterType.SELECT, statusOptions),
            FemaleEmployeeFilterMeta("jobCode", FemaleEmployeeFilterType.SELECT, jobCodeOptions),
            FemaleEmployeeFilterMeta("workType1", FemaleEmployeeFilterType.SELECT, workType1Options),
            FemaleEmployeeFilterMeta("workType3", FemaleEmployeeFilterType.SELECT, workType3Options),
            FemaleEmployeeFilterMeta(
                "professionalPromotionTeam",
                FemaleEmployeeFilterType.SELECT,
                promotionTeamOptions,
            ),
            FemaleEmployeeFilterMeta("keyword", FemaleEmployeeFilterType.TEXT),
        )

        return FemaleEmployeeListMetaResponse(
            filters = filters,
            defaults = FemaleEmployeeListDefaults(
                pageSize = FEMALE_EMPLOYEE_LIST_DEFAULT_PAGE_SIZE,
                sort = FEMALE_EMPLOYEE_LIST_DEFAULT_SORT,
            ),
        )
    }

    /**
     * 여사원 상세 폼(수정 모달) 렌더링용 메타 — 재직상태 / 권한 / 전문행사조 Select 옵션.
     *
     * web 화면이 하드코딩하던 상수 3종(`STATUS_OPTIONS` / `ROLE_SELECT_OPTIONS` / `PPT_OPTIONS`)을
     * 서버 단일 출처로 이전한다. 권한 의존 옵션이 없어 전 사용자 동일 응답이다.
     *
     * ## 목록 [getFemaleEmployeeListMetaStatic] 과의 차이 — 전문행사조 옵션 구성
     *
     * 목록 필터는 검색 전용 선택지 '행사조 전체'([ProfessionalPromotionTeamType.ASSIGNED_ONLY_DISPLAY_NAME])
     * 를 포함하지만, 폼은 **저장 가능한 값만** 내려야 하므로 제외한다. 대신 '일반'
     * ([ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME]) 은 미배정으로 되돌리는 명시적 선택지라
     * 포함한다 — 신규 시스템의 미배정은 null 이므로 이 값을 받은 수정 요청은
     * [AdminEmployeeUpdateService] 가 null 로 해석해 저장한다.
     *
     * 정식 5개 조는 enum 선언 순서(= SF picklist 정의 순서, `sorted=false`) 를 그대로 따른다.
     */
    fun getFemaleEmployeeFormMeta(): FemaleEmployeeFormMetaResponse {
        val statuses = EMPLOYEE_STATUS_OPTIONS
            .map { FemaleEmployeeFormOption(value = it, label = it) }

        val roles = EMPLOYEE_ROLE_FORM_OPTIONS
            .map { (value, label) -> FemaleEmployeeFormOption(value = value, label = label) }

        // '일반'(미배정) 을 맨 앞에 두고 정식 5개 조가 뒤따른다 — SF 레거시 표시 정합.
        val promotionTeams = listOf(
            FemaleEmployeeFormOption(
                value = ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME,
                label = ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME,
            ),
        ) + ProfessionalPromotionTeamType.entries
            .map { FemaleEmployeeFormOption(value = it.displayName, label = it.displayName) }

        return FemaleEmployeeFormMetaResponse(
            statuses = statuses,
            roles = roles,
            professionalPromotionTeams = promotionTeams,
        )
    }

    /**
     * 지점 보안 필터에 BranchMapping 확장 코드를 합친다 (`applyBranchScope = true` 경로 전용).
     *
     * 2025-05 SAP 조직 개편으로 지점코드가 전면 재부여되었고(구 5452~/5666~ → 현행 5815~), 개편 이후
     * 발령을 받지 못한 사원은 [Employee.costCenterCode] 가 옛 코드로 남아 있다. 현행 코드만으로
     * IN 매칭하면 이들이 목록에서 누락되므로, [BranchCodeExpander] (branch_mapping = SF
     * `BranchMapping__mdt` 계보 매핑) 로 옛 코드까지 합집합에 넣는다.
     *
     * **주의**: 확장 결과가 옛 코드만은 아니다 — 일부 매핑 행은 현행 **타 조직**까지 끌어온다
     * (예: `5829` → 인천1·2·3지점). 성격 4종과 롤업 6건은
     * [com.otoki.powersales.domain.org.organization.branchmapping.entity.BranchMapping] KDoc 참조.
     *
     * [com.otoki.powersales.admin.dto.DataScope.effectiveBranchCodes] 는 지점을 **선택**하면 요청 코드
     * 1건만 `Filtered` 로 돌려주므로(`DataScope.kt` 참조), 호출부가 넘긴 확장 집합이 그 단계에서 버려진다.
     * 그래서 확장은 스코프 산출부가 아니라 **최종 필터 직전인 여기서** 다시 적용해야 선택/미선택 양쪽에
     * 일관되게 반영된다. 대시보드 기본현황(`AdminDashboardService.expandQueryCodes`) 과 동일한 축이며,
     * 두 화면의 총원이 일치해야 한다([FemaleStaffHeadcountFilter] 참조).
     *
     * 확장은 지점 **보안 필터**에만 적용한다 — `applyBranchScope = false` 인 전사 검색의 표시 필터
     * (사용자가 고른 지점 그대로 보여주는 용도) 는 확장하지 않는다.
     */
    private fun expandBranchCodes(codes: List<String>): List<String> {
        if (codes.isEmpty()) return codes
        return branchCodeExpander.expand(codes).toList()
    }

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
        // 직무(판촉직/OSC직) — 대시보드 인원현황 도넛과 동일한 jobCode 축. blank/null 이면 미적용.
        jobCode: String? = null,
        // 여사원 인원현황 모수(레거시 리포트 정합)로 좁힐지 — 여사원 현황 화면만 true.
        femaleStaffHeadcountScope: Boolean = false,
        // 발령명 '면직' 을 퇴직과 동일 취급할지 (조회 필터 + 상태 표시) — 여사원 현황 화면만 true.
        // [DismissalPolicy] 참조.
        treatDismissalAsResigned: Boolean = false,
    ): EmployeeListResponse {
        val filters = parseSearchFilters(workType1, workType3, professionalPromotionTeam, jobCode)
        val requestedBranch = costCenterCode?.takeIf { it.isNotBlank() }
        val branchFilter: List<String>? = if (applyBranchScope) {
            when (val result = scope.effectiveBranchCodes(requestedBranch)) {
                // 전사 권한 (SYSTEM_ADMIN / 영업지원·본부) — 지점 보안 필터 없음
                is EffectiveBranchResult.All -> null
                // 본인 소속 지점(또는 그 안에서 선택한 단일 지점) 으로 제한
                is EffectiveBranchResult.Filtered -> expandBranchCodes(result.codes)
                // 권한 밖 지점 요청 — 빈 결과
                is EffectiveBranchResult.NoAccess -> return EmployeeListResponse(
                    content = emptyList(), page = page, size = size, totalElements = 0, totalPages = 0
                )
            }
        } else {
            // 전사 검색 — costCenterCode 는 사용자 표시 필터로만 전달(보안축 아님)
            requestedBranch?.let { listOf(it) }
        }

        val workTypeMatchedEmployeeIds = resolveWorkTypeMatchedEmployeeIds(filters)
        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val userPage = employeeRepository.findEmployees(
            status, branchFilter, keyword, role, roles,
            workTypeMatchedEmployeeIds, filters.promotionTeam, filters.promotionTeamGeneral,
            filters.promotionTeamAssignedOnly,
            pageable, filters.jobCodes, femaleStaffHeadcountScope, treatDismissalAsResigned,
        )

        // 만나이 / 근속년수 계산 기준일 — 페이지 전체에 동일 적용
        val today = LocalDate.now()
        // 근무형태/근무거래처 — 현재 페이지 사원들의 최근 출근등록 1건을 조회 (N+1 없음)
        val attendanceInfo = loadAttendanceInfo(userPage.content.map { it.id })
        return EmployeeListResponse(
            content = userPage.content.map { emp ->
                val info = attendanceInfo[emp.id]
                EmployeeListItem.from(
                    emp, today, info?.workingCategory1, info?.workingCategory3,
                    info?.accountName, info?.accountCode, treatDismissalAsResigned,
                )
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
        // 직무(판촉직/OSC직) — 목록과 동일 축. blank/null 이면 미적용.
        jobCode: String? = null,
        // 여사원 인원현황 모수(레거시 리포트 정합)로 좁힐지 — 목록과 동일하게 전달해야 건수가 일치한다.
        femaleStaffHeadcountScope: Boolean = false,
        // 발령명 '면직' 을 퇴직과 동일 취급할지 — 목록과 동일하게 전달해야 내용이 일치한다 ([DismissalPolicy]).
        treatDismissalAsResigned: Boolean = false,
    ): ExcelResult {
        val filters = parseSearchFilters(workType1, workType3, professionalPromotionTeam, jobCode)
        val requestedBranch = costCenterCode?.takeIf { it.isNotBlank() }
        val noAccess: Boolean
        val branchFilter: List<String>? = if (applyBranchScope) {
            when (val result = scope.effectiveBranchCodes(requestedBranch)) {
                is EffectiveBranchResult.All -> { noAccess = false; null }
                is EffectiveBranchResult.Filtered -> { noAccess = false; expandBranchCodes(result.codes) }
                is EffectiveBranchResult.NoAccess -> { noAccess = true; null }
            }
        } else {
            noAccess = false
            requestedBranch?.let { listOf(it) }
        }

        val items = if (noAccess) {
            emptyList()
        } else {
            val workTypeMatchedEmployeeIds = resolveWorkTypeMatchedEmployeeIds(filters)
            val pageable = PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by("name").ascending())
            val today = LocalDate.now()
            val employees = employeeRepository.findEmployees(
                status, branchFilter, keyword, role, roles,
                workTypeMatchedEmployeeIds, filters.promotionTeam, filters.promotionTeamGeneral,
                filters.promotionTeamAssignedOnly,
                pageable, filters.jobCodes, femaleStaffHeadcountScope, treatDismissalAsResigned,
            ).content
            val attendanceInfo = loadAttendanceInfo(employees.map { it.id })
            employees.map { emp ->
                val info = attendanceInfo[emp.id]
                EmployeeListItem.from(
                    emp, today, info?.workingCategory1, info?.workingCategory3,
                    info?.accountName, info?.accountCode, treatDismissalAsResigned,
                )
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
