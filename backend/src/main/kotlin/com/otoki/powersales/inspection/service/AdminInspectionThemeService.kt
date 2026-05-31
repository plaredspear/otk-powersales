package com.otoki.powersales.inspection.service

import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.UploadFileParentTypes
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
import java.time.LocalDate
import org.springframework.beans.factory.annotation.Value
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
 * - `PublicFlag__c`: SF 에서 사실상 미사용 dead field. 신규는 생성 시 true 고정 (모바일 가시성 기본 노출)
 * - 부서/지점: before-insert 자동 주입 동일. 수정 시 OwnerId 변경 미지원 → 부서/지점 불변
 * - 삭제: soft delete (`isDeleted = true`)
 */
@Service
@Transactional(readOnly = true)
class AdminInspectionThemeService(
    private val inspectionThemeRepository: InspectionThemeRepository,
    private val siteActivityRepository: SiteActivityRepository,
    private val employeeRepository: EmployeeRepository,
    private val uploadFileRepository: UploadFileRepository,
    @Value("\${app.aws.s3.bucket:otoki-bucket}")
    private val s3BucketName: String,
    @Value("\${app.aws.s3.region:ap-northeast-2}")
    private val s3Region: String,
) {

    companion object {
        private const val MAX_PAGE_SIZE = 200
        private const val THEME_NUMBER_PREFIX = "TM"
        private const val THEME_NUMBER_DIGITS = 8
    }

    /** 테마 목록 — 테마이름/부서/테마번호 검색 + 페이징 + 하위 점검결과 수. */
    fun search(keyword: String?, page: Int, size: Int): AdminThemeListResponse {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page.coerceAtLeast(0), pageSize)
        val result = inspectionThemeRepository.searchForAdmin(keyword, pageable)

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
            publicFlag = true,
            isDeleted = false,
        )
        val saved = inspectionThemeRepository.save(theme)
        return ThemeMutationResponse.from(saved)
    }

    /** 테마 수정 — 테마이름/시작일/종료일만 변경. 부서/지점/테마번호 불변. */
    @Transactional
    fun update(id: Long, request: UpdateThemeRequest): ThemeMutationResponse {
        val theme = findActiveTheme(id)
        val updated = InspectionTheme(
            id = theme.id,
            sfid = theme.sfid,
            name = theme.name,
            title = request.title,
            startDate = request.startDate?.let { LocalDate.parse(it) },
            endDate = request.endDate?.let { LocalDate.parse(it) },
            department = theme.department,
            branchCode = theme.branchCode,
            publicFlag = theme.publicFlag,
            isDeleted = theme.isDeleted,
            ownerSfid = theme.ownerSfid,
            createdBySfid = theme.createdBySfid,
            lastModifiedBySfid = theme.lastModifiedBySfid,
            ownerUser = theme.ownerUser,
            ownerGroup = theme.ownerGroup,
            createdBy = theme.createdBy,
            lastModifiedBy = theme.lastModifiedBy,
        ).also { it.createdAt = theme.createdAt }
        val saved = inspectionThemeRepository.save(updated)
        return ThemeMutationResponse.from(saved)
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

    internal fun composeS3Url(key: String): String =
        "https://$s3BucketName.s3.$s3Region.amazonaws.com/$key"
}
