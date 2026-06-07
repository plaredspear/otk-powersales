package com.otoki.powersales.inspection.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.PublicUrlResolver
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityDetailResponse
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityFilter
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityListItem
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityListResponse
import com.otoki.powersales.inspection.dto.response.InspectionPhotoResponse
import com.otoki.powersales.inspection.entity.QSiteActivity.Companion.siteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.enums.InspectionFieldType
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import java.time.LocalDate
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * admin 현장점검 조회 Service.
 *
 * ## 레거시 매핑
 * - SF: `현장점검(등록)` Theme 페이지의 SiteActivity related list + `IF_REST_MOBILE_SiteActivitySearch` admin 관점
 * - SObject: `DKRetail__SiteAcitivity__c`
 *
 * ## 신규 차이
 * - 조회 전용 (등록/수정은 mobile 전용 — 레거시도 현장 사원 모바일 입력 성격)
 * - SF 가시 범위(SharingRule) Predicate 로 admin 데이터 스코프 적용 (AdminSuggestion 패턴)
 */
@Service
@Transactional(readOnly = true)
class AdminSiteActivityService(
    private val siteActivityRepository: SiteActivityRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val policyEvaluator: SharingRulePolicyEvaluator,
    private val publicUrlResolver: PublicUrlResolver
) {

    companion object {
        private const val MAX_PAGE_SIZE = 200
    }

    /**
     * admin 현장점검 목록 검색 — 가시 범위 + 기간/거래처/분류/현장유형/사원명 필터 + 페이징.
     */
    fun search(
        scope: DataScope,
        startDate: LocalDate?,
        endDate: LocalDate?,
        category: InspectionCategory?,
        fieldType: InspectionFieldType?,
        employeeName: String?,
        accountCode: String?,
        page: Int,
        size: Int
    ): AdminSiteActivityListResponse {
        val effectiveStart = startDate ?: LocalDate.now().minusDays(30)
        val effectiveEnd = endDate ?: LocalDate.now()
        if (effectiveEnd.isBefore(effectiveStart)) {
            throw IllegalArgumentException("종료일이 시작일보다 빠를 수 없습니다")
        }

        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "DKRetail__SiteAcitivity__c",
            entityPath = siteActivity
        )

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        val filter = AdminSiteActivityFilter(
            startDate = effectiveStart,
            endDate = effectiveEnd,
            category = category,
            fieldType = fieldType,
            employeeName = employeeName,
            accountCode = accountCode
        )
        val result = siteActivityRepository.searchForAdmin(policyPredicate, filter, pageable)
        return AdminSiteActivityListResponse(
            content = result.content.map { AdminSiteActivityListItem.from(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    /**
     * admin 현장점검 단건 상세 — 가시 범위 검증 후 본문 + 첨부 사진 합성.
     *
     * 가시 범위 밖이면 IllegalArgumentException (404 — SF OWD=Private 동등).
     */
    fun getDetail(scope: DataScope, id: Long): AdminSiteActivityDetailResponse {
        val activity = siteActivityRepository.findByIdAndIsDeletedFalse(id)
            ?: throw IllegalArgumentException("현장점검을 찾을 수 없습니다")
        if (!isVisible(scope, id)) {
            throw IllegalArgumentException("현장점검을 찾을 수 없습니다")
        }

        val photos = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SITE_ACTIVITY, activity.id)
            .filter { !it.uniqueKey.isNullOrBlank() }
            .sortedBy { it.createdAt }
            .map { InspectionPhotoResponse.of(it, composeS3Url(it.uniqueKey!!)) }

        return AdminSiteActivityDetailResponse.from(activity, photos)
    }

    /** 가시 범위 내에 해당 id 가 포함되는지 — policyPredicate + id 필터로 count. */
    private fun isVisible(scope: DataScope, id: Long): Boolean {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "DKRetail__SiteAcitivity__c",
            entityPath = siteActivity
        )
        return siteActivityRepository.existsVisibleById(policyPredicate, id)
    }

    private fun composeS3Url(key: String): String =
        publicUrlResolver.resolve(key)!!
}
