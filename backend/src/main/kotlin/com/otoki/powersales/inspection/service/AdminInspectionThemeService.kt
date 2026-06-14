package com.otoki.powersales.inspection.service

import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.inspection.dto.admin.AdminThemeDetailResponse
import com.otoki.powersales.inspection.dto.admin.AdminThemeListItem
import com.otoki.powersales.inspection.dto.admin.AdminThemeListResponse
import com.otoki.powersales.inspection.dto.admin.AdminThemeSiteActivityItem
import com.otoki.powersales.inspection.dto.admin.CreateThemeRequest
import com.otoki.powersales.inspection.dto.admin.ThemeMutationResponse
import com.otoki.powersales.inspection.dto.admin.UpdateThemeRequest
import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.inspection.repository.SiteActivityRepository
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
 */
@Service
@Transactional(readOnly = true)
class AdminInspectionThemeService(
    private val inspectionThemeRepository: InspectionThemeRepository,
    private val siteActivityRepository: SiteActivityRepository,
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val uploadFileRepository: UploadFileRepository,
) {

    companion object {
        private const val MAX_PAGE_SIZE = 200
        private const val THEME_NUMBER_PREFIX = "TM"
        private const val THEME_NUMBER_DIGITS = 8
    }

    /** 테마 목록 — 키워드(테마번호/이름/부서) + 부서/지점코드 필터 + 페이징 + 하위 점검결과 수. */
    fun search(
        keyword: String?,
        department: String?,
        branchCode: String?,
        page: Int,
        size: Int,
    ): AdminThemeListResponse {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page.coerceAtLeast(0), pageSize)
        val result = inspectionThemeRepository.searchForAdmin(keyword, department, branchCode, pageable)

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

    /** 테마 상세 + 하위 현장점검 결과 관련목록. */
    fun getDetail(id: Long): AdminThemeDetailResponse {
        val theme = findActiveTheme(id)
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

    /** 테마 수정 — 테마이름/시작일/종료일만 변경. 부서/지점/테마번호 불변. */
    @Transactional
    fun update(id: Long, request: UpdateThemeRequest): ThemeMutationResponse {
        val theme = findActiveTheme(id)

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

    /** 테마 삭제 — soft delete. */
    @Transactional
    fun delete(id: Long) {
        val theme = findActiveTheme(id)
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
