package com.otoki.powersales.domain.activity.inspection.service

import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeDetailResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeListItem
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeListResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeSiteActivityItem
import com.otoki.powersales.domain.activity.inspection.dto.admin.CreateThemeRequest
import com.otoki.powersales.domain.activity.inspection.dto.admin.ThemeMutationResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.UpdateThemeRequest
import com.otoki.powersales.domain.activity.inspection.entity.InspectionTheme
import com.otoki.powersales.domain.activity.inspection.exception.InspectionThemeForbiddenException
import com.otoki.powersales.domain.activity.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.domain.activity.inspection.repository.InspectionThemeRepositoryCustomImpl
import com.otoki.powersales.domain.activity.inspection.repository.SiteActivityRepository
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.permission.SfPermissionResolver
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import java.time.LocalDate
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * admin 현장점검(등록) 테마 Service.
 *
 * ## 레거시 매핑
 * - SF: `현장점검(등록)` = `Theme__c` 표준 레코드 페이지 (목록/등록/수정/삭제 + `SiteActivityToExcel` 엑셀다운로드)
 * - Trigger: `ThemeTriggerHandler.beforeInsertTheme` — 생성자 Employee 소속으로 `Department__c`/`BranchCode__c` 자동 주입
 * - 테마번호 `Name`: SF AutoNumber `TM00000000`
 *
 * ## 신규 차이
 * - 테마번호 채번: DB 의 `TM` 최대 일련번호 + 1 (8자리 zero-pad) — SF AutoNumber 형식 유지
 * - `PublicFlag__c`: SF 에서 사실상 미사용 dead field. 신규는 레거시 기본값 정합으로 false 고정 (조회 필터엔 미사용)
 * - 부서/지점: before-insert 자동 주입 동일. 수정 시 OwnerId 변경 미지원 → 부서/지점 불변
 * - 삭제: soft delete (`isDeleted = true`)
 *
 * ## 지점 스코프 (레거시 SF 대비 정책 강화 — deviation)
 * 레거시 SF `Theme__c` admin 조회는 지점 필터가 없는 **전사 노출**이었다(표준 UI, 커스텀 Apex 0건). 모바일 현장점검
 * 등록의 `branchcode IN (공통코드 + 본인 costCenterId)` 화이트리스트는 **모바일 등록 UX 전용** 이라 admin 에는 근거 없음.
 * 신규는 관리 편의를 위해 admin 목록/상세/엑셀에 **본인 지점 스코프**를 적용한다(`branch_code` 기준, deviation).
 *
 * 스코프는 [AdminDataScopeService] 의 [DataScope] 로 판정한다 — 전사 권한자(시스템개발자/영업지원/본부장,
 * `isAllBranches`)는 전건, 그 외는 **본인 `costCenterCode` 단일값** + 전사공통 화이트리스트
 * ([InspectionThemeRepositoryCustomImpl] COMMON_BRANCH_CODES) 로 `branch_code IN (...)` 제한.
 * 테마 `branch_code` 는 생성자 사원의 `costCenterCode`(단일 지점) 로 적재되므로, 조직 트리를 확장하는
 * 여사원 일정용 `findTeamScheduleBranches`(상위 조직 매칭 시 형제 지점까지 딸려옴) 는 쓰지 않는다.
 * 사용자가 Select 로 특정 지점을 고르면 그 지점으로 좁히되, 권한 스코프 밖 값은 무시(IDOR 차단).
 * 대행(impersonation) 시 principal/DataScope 는 대행 대상 기준이라 대상의 본인 지점으로 정상 스코프된다.
 */
@Service
@Transactional(readOnly = true)
class AdminInspectionThemeService(
    private val inspectionThemeRepository: InspectionThemeRepository,
    private val siteActivityRepository: SiteActivityRepository,
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val dataScopeService: AdminDataScopeService,
    private val organizationRepository: OrganizationRepository,
) {

    companion object {
        private const val MAX_PAGE_SIZE = 200
        private const val THEME_NUMBER_PREFIX = "TM"
        private const val THEME_NUMBER_DIGITS = 8
    }

    /**
     * 테마 목록 — 키워드(테마번호/이름/부서) + 부서 필터 + 지점 스코프 + 페이징 + 하위 점검결과 수.
     *
     * 지점 스코프([ThemeScope]): 전사 권한자는 전건([ThemeScope.All]), 그 외는 본인 지점 + 전사공통
     * ([ThemeScope.Branches]), 권한 없음은 빈 목록([ThemeScope.None]). `branchCode`(Select 선택값)가 권한
     * 스코프 안이면 그 지점으로 좁히고, 밖이면 무시(IDOR 차단).
     */
    fun search(
        principal: WebUserPrincipal,
        keyword: String?,
        department: String?,
        branchCode: String?,
        page: Int,
        size: Int,
    ): AdminThemeListResponse {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page.coerceAtLeast(0), pageSize)

        val scopeBranchCodes: List<String>? = when (val scope = resolveScope(principal, branchCode)) {
            is ThemeScope.All -> null                    // 전사 — 스코프 미적용(전건)
            is ThemeScope.Branches -> scope.codes        // 본인 지점 (repository 가 COMMON 병합)
            is ThemeScope.None -> return AdminThemeListResponse(
                content = emptyList(), page = page, size = pageSize, totalElements = 0, totalPages = 0
            )
        }
        val result = inspectionThemeRepository.searchForAdmin(keyword, department, scopeBranchCodes, pageable)

        val counts = inspectionThemeRepository.countSiteActivitiesByThemeIds(result.content.map { it.id })
        val items = result.content.map { theme ->
            AdminThemeListItem(
                id = theme.id,
                name = theme.name,
                title = theme.title,
                department = theme.department,
                branchCode = theme.branchCode,
                startDate = theme.startDate?.toString(),
                endDate = theme.endDate?.toString(),
                ownerUserId = theme.ownerUser?.id,
                ownerName = theme.ownerUser?.name,
                siteActivityCount = counts[theme.id] ?: 0L,
                createdAt = theme.createdAt.toString(),
            )
        }
        return AdminThemeListResponse(
            content = items,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    /** 테마 상세 + 하위 현장점검 결과 관련목록. 지점 스코프 밖이면 403. */
    fun getDetail(principal: WebUserPrincipal, id: Long): AdminThemeDetailResponse {
        val theme = findActiveTheme(id)
        requireThemeInScope(principal, theme)
        val activities = siteActivityRepository.findByInspectionThemeIdForAdmin(id).map { sa ->
            AdminThemeSiteActivityItem(
                id = sa.id,
                name = sa.name,
                branchName = sa.costCenterCode,
                accountName = sa.account?.name,
                productName = sa.product?.name,
                orgName = sa.employee?.orgName,
                employeeName = sa.employee?.name,
                category = sa.category,
                activityDate = sa.activityDate?.toString(),
            )
        }
        return AdminThemeDetailResponse(
            id = theme.id,
            name = theme.name,
            title = theme.title,
            department = theme.department,
            branchCode = theme.branchCode,
            startDate = theme.startDate?.toString(),
            endDate = theme.endDate?.toString(),
            ownerUserId = theme.ownerUser?.id,
            ownerName = theme.ownerUser?.name,
            createdAt = theme.createdAt.toString(),
            updatedAt = theme.updatedAt.toString(),
            siteActivities = activities,
        )
    }

    /**
     * 테마 생성 — 테마번호 채번 + 생성자 소속(부서/지점) 자동 주입.
     *
     * Trigger `beforeInsertTheme` 동등: 생성자 Employee 의 `orgName`(부서)/`costCenterCode`(지점) 주입.
     */
    @Transactional
    fun create(principal: WebUserPrincipal, request: CreateThemeRequest): ThemeMutationResponse {
        val employeeId = principal.requireEmployeeId()
        val employee = employeeRepository.findById(employeeId).orElseThrow {
            IllegalStateException("생성자 직원 정보를 찾을 수 없습니다 (employeeId=$employeeId)")
        }

        val theme = InspectionTheme(
            name = nextThemeNumber(),
            title = request.title,
            startDate = request.startDate?.let { LocalDate.parse(it) },
            endDate = request.endDate?.let { LocalDate.parse(it) },
            department = employee.orgName,
            branchCode = employee.costCenterCode,
            // 레거시 Theme__c.PublicFlag__c 기본값 정합(입력란 없이 false 생성). 조회 필터엔 미사용.
            publicFlag = false,
            isDeleted = false,
        )
        val saved = inspectionThemeRepository.save(theme)
        return ThemeMutationResponse.from(saved)
    }

    /** 테마 수정 — 테마이름/시작일/종료일만 변경. 부서/지점/테마번호 불변. 소유자/전권한자만 가능. */
    @Transactional
    fun update(principal: WebUserPrincipal, id: Long, request: UpdateThemeRequest): ThemeMutationResponse {
        val theme = findActiveTheme(id)
        requireThemeOwnerOrModifyAll(principal, theme)

        // 소유권 이전 — ownerUserId 가 현재 소유자와 다르면 새 소유자로 변경 + 부서 갱신.
        // 레거시 ThemeTriggerHandler.beforeUpdateTheme 동등: 새 소유자 Employee 의 orgName 으로
        // department 갱신, BranchCode 는 불변(최초 생성자 기준 유지).
        val ownerChanged = request.ownerUserId != null && request.ownerUserId != theme.ownerUser?.id
        val newOwner: User? = if (ownerChanged) {
            userRepository.findById(request.ownerUserId!!).orElseThrow {
                IllegalArgumentException("소유자로 지정할 사용자를 찾을 수 없습니다 (userId=${request.ownerUserId})")
            }
        } else {
            null
        }
        val newDepartment = if (newOwner != null) resolveDepartment(newOwner) ?: theme.department else theme.department

        val updated = InspectionTheme(
            id = theme.id,
            sfid = theme.sfid,
            name = theme.name,
            title = request.title,
            startDate = request.startDate?.let { LocalDate.parse(it) },
            endDate = request.endDate?.let { LocalDate.parse(it) },
            department = newDepartment,
            branchCode = theme.branchCode,
            publicFlag = theme.publicFlag,
            isDeleted = theme.isDeleted,
            ownerSfid = if (newOwner != null) null else theme.ownerSfid,
            createdBySfid = theme.createdBySfid,
            lastModifiedBySfid = theme.lastModifiedBySfid,
            ownerUser = newOwner ?: theme.ownerUser,
            ownerGroup = if (newOwner != null) null else theme.ownerGroup,
            createdBy = theme.createdBy,
            lastModifiedBy = theme.lastModifiedBy,
        ).also { it.createdAt = theme.createdAt }
        val saved = inspectionThemeRepository.save(updated)
        return ThemeMutationResponse.from(saved)
    }

    /** 소유자(User) 의 매칭 Employee 소속(orgName) 해석 — User.employeeCode == Employee.employeeCode. */
    private fun resolveDepartment(owner: User): String? {
        val code = owner.employeeCode ?: return null
        return employeeRepository.findByEmployeeCode(code).orElse(null)?.orgName
    }

    /** 테마 삭제 — soft delete. 소유자/전권한자만 가능. */
    @Transactional
    fun delete(principal: WebUserPrincipal, id: Long) {
        val theme = findActiveTheme(id)
        requireThemeOwnerOrModifyAll(principal, theme)
        val deleted = InspectionTheme(
            id = theme.id,
            sfid = theme.sfid,
            name = theme.name,
            title = theme.title,
            startDate = theme.startDate,
            endDate = theme.endDate,
            department = theme.department,
            branchCode = theme.branchCode,
            publicFlag = theme.publicFlag,
            isDeleted = true,
            ownerSfid = theme.ownerSfid,
            createdBySfid = theme.createdBySfid,
            lastModifiedBySfid = theme.lastModifiedBySfid,
            ownerUser = theme.ownerUser,
            ownerGroup = theme.ownerGroup,
            createdBy = theme.createdBy,
            lastModifiedBy = theme.lastModifiedBy,
        ).also { it.createdAt = theme.createdAt }
        inspectionThemeRepository.save(deleted)
    }

    /**
     * 단건(엑셀 다운로드 등) 지점 스코프 가드 — 스코프 밖 테마면 [InspectionThemeForbiddenException] (403).
     * themeId 추측으로 타 지점 테마의 하위 점검결과를 들여다보는 것을 차단. 컨트롤러에서 직접 호출.
     */
    fun validateThemeScope(principal: WebUserPrincipal, id: Long) {
        requireThemeInScope(principal, findActiveTheme(id))
    }

    /**
     * 현장점검 테마 관리 화면 지점 셀렉터 옵션.
     *
     * 전사 권한자(시스템개발자/영업지원/본부장) 는 전 지점([OrganizationRepository.findAllTeamScheduleBranches]),
     * 그 외는 본인 `costCenterCode` 단일 지점 1건. 여사원 일정용 조직 트리 확장을 쓰지 않아 형제 지점이 딸려오지 않는다.
     */
    fun getBranches(principal: WebUserPrincipal): List<BranchResponse> {
        val scope = dataScopeService.resolve(principal)
        if (scope.isAllBranches) {
            return organizationRepository.findAllTeamScheduleBranches()
        }
        val code = principal.costCenterCode?.takeIf { it.isNotBlank() } ?: return emptyList()
        val org = organizationRepository.findFirstByAnyOrgCodeLevel(code) ?: return emptyList()
        // fetchTeamScheduleBranches 규칙과 동일 — Level5(지점) 우선, 없으면 Level4.
        val name = org.orgNameLevel5?.takeIf { it.isNotBlank() } ?: org.orgNameLevel4 ?: return emptyList()
        return listOf(BranchResponse(branchCode = code, branchName = name))
    }

    /**
     * 수정/삭제 가드 — 소유자 본인 또는 MODIFY_ALL_DATA(전 테마 수정) 만 허용. 그 외는 403.
     *
     * SF 레거시 정합: Theme__c 수정/소유자 변경은 소유자이거나 modifyAll 권한자만 가능(표준 record Edit 규칙).
     * entity EDIT/DELETE 권한(@RequiresSfPermission) 통과 후, 타인 소유 테마는 여기서 차단한다.
     */
    private fun requireThemeOwnerOrModifyAll(principal: WebUserPrincipal, theme: InspectionTheme) {
        val hasModifyAll = principal.permissions.contains(
            SfPermissionResolver.systemKey(SfSystemPermission.MODIFY_ALL_DATA)
        )
        val isOwner = theme.ownerUser?.id != null && theme.ownerUser?.id == principal.userId
        if (!hasModifyAll && !isOwner) {
            throw InspectionThemeForbiddenException()
        }
    }

    /** 목록 스코프와 동일 기준(본인 지점 + 전사공통 화이트리스트)으로 단건 가시성 검증. */
    private fun requireThemeInScope(principal: WebUserPrincipal, theme: InspectionTheme) {
        // 단건 검증은 특정 지점 선택 없이 전체 스코프 기준. 전사면 항상 통과.
        val scopeBranchCodes: List<String> = when (val scope = resolveScope(principal, null)) {
            is ThemeScope.All -> return                  // 전사 — 모든 테마 통과
            is ThemeScope.Branches -> scope.codes
            is ThemeScope.None -> emptyList()            // 권한 없음 — COMMON 만 통과, 나머지 403
        }
        if (!InspectionThemeRepositoryCustomImpl.isBranchInScope(theme.branchCode, scopeBranchCodes)) {
            throw InspectionThemeForbiddenException()
        }
    }

    /**
     * 조회 지점 스코프 산출 — [AdminDataScopeService] 의 [DataScope] 기준.
     *
     * 전사 권한자([DataScope.isAllBranches])는 [ThemeScope.All](전건). 그 외는 본인 지점(costCenterCode) 단일 +
     * 전사공통([ThemeScope.Branches]). 권한 지점이 없으면 [ThemeScope.None]. `requestedBranchCode`(Select 선택값)가
     * 권한 지점 안이면 그 지점으로 좁히고, 밖이면 전체 권한 지점(IDOR 차단).
     */
    private fun resolveScope(principal: WebUserPrincipal, requestedBranchCode: String?): ThemeScope {
        val scope = dataScopeService.resolve(principal)
        if (scope.isAllBranches) return ThemeScope.All
        val allowed = scope.branchCodes
        if (allowed.isEmpty()) return ThemeScope.None
        val codes = if (!requestedBranchCode.isNullOrBlank() && requestedBranchCode in allowed) {
            listOf(requestedBranchCode)
        } else {
            allowed
        }
        return ThemeScope.Branches(codes)
    }

    /** 테마 조회 지점 스코프 — 전건 / 지점 목록 / 권한없음. */
    private sealed interface ThemeScope {
        data object All : ThemeScope
        data class Branches(val codes: List<String>) : ThemeScope
        data object None : ThemeScope
    }

    private fun findActiveTheme(id: Long): InspectionTheme {
        val theme = inspectionThemeRepository.findById(id).orElseThrow {
            IllegalArgumentException("테마를 찾을 수 없습니다")
        }
        if (theme.isDeleted == true) {
            throw IllegalArgumentException("테마를 찾을 수 없습니다")
        }
        return theme
    }

    /** 테마번호 채번 — TM + 8자리 zero-pad 일련번호. */
    private fun nextThemeNumber(): String {
        val next = inspectionThemeRepository.findMaxThemeNumberSequence() + 1
        return THEME_NUMBER_PREFIX + next.toString().padStart(THEME_NUMBER_DIGITS, '0')
    }
}
