package com.otoki.powersales.inspection.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.PublicUrlResolver
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.inspection.dto.request.InspectionRegisterRequest
import com.otoki.powersales.inspection.dto.response.InspectionDetailResponse
import com.otoki.powersales.inspection.dto.response.InspectionFieldTypeResponse
import com.otoki.powersales.inspection.dto.response.InspectionListItem
import com.otoki.powersales.inspection.dto.response.InspectionPhotoResponse
import com.otoki.powersales.inspection.dto.response.ThemeResponse
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.enums.InspectionFieldType
import com.otoki.powersales.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 현장점검 Service — 레거시 SF 등가 구현 (SF 메타 기반, 스펙 미경유).
 *
 * ## 레거시 매핑
 * - SF Apex: `IF_REST_MOBILE_SiteActivitySearch.cls#doPost` (목록), `IF_REST_MOBILE_SiteActivityRegist.cls#doPost` (등록)
 * - SObject: `DKRetail__SiteAcitivity__c` (현장점검 결과) + 부모 `Theme__c` (현장점검 등록)
 *
 * ## 신규 차이
 * - parent(SiteActivity) + child(UploadFile N건) `@Transactional` 단일 트랜잭션 (레거시 R10-bis 원자성 보강)
 * - 첨부는 Backend 중계 (S3 직접 PUT 폐지) — SuggestionService 패턴 재사용
 * - costCenterCode: 레거시 SiteActivityTrigger.beforeInsert 가 사원 CostCenterCode 복사 → service 단 동등 세팅
 */
@Service
@Transactional(readOnly = true)
class SiteActivityService(
    private val siteActivityRepository: SiteActivityRepository,
    private val inspectionThemeRepository: InspectionThemeRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val productRepository: com.otoki.powersales.product.repository.ProductRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val fileStorageService: FileStorageService,
    private val publicUrlResolver: PublicUrlResolver
) {

    companion object {
        private const val MAX_PHOTO_COUNT = 10
    }

    /**
     * 사원 본인의 현장점검 목록 조회.
     *
     * 기간 필수, accountId / category(자사·경쟁사) 옵션 필터. activityDate DESC.
     */
    fun getList(
        employeeId: Long,
        accountId: Long?,
        category: InspectionCategory?,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<InspectionListItem> =
        siteActivityRepository
            .searchByEmployee(employeeId, accountId, category, fromDate, toDate)
            .map { InspectionListItem.from(it) }

    /**
     * 현장점검 상세 조회. 본인 row 한정. 첨부(UploadFile) 를 photos 로 합성.
     */
    fun getDetail(inspectionId: Long, employeeId: Long): InspectionDetailResponse {
        val activity = siteActivityRepository.findByIdAndIsDeletedFalse(inspectionId)
            ?: throw IllegalArgumentException("현장점검을 찾을 수 없습니다")
        if (activity.employee?.id != employeeId) {
            throw IllegalArgumentException("본인의 현장점검만 조회할 수 있습니다")
        }

        val photos = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SITE_ACTIVITY, activity.id)
            .filter { !it.uniqueKey.isNullOrBlank() }
            .sortedBy { it.createdAt }
            .map { InspectionPhotoResponse.of(it, composeS3Url(it.uniqueKey!!)) }

        return InspectionDetailResponse.from(activity, photos)
    }

    /**
     * 현장점검 등록 (parent + 첨부 N건 단일 트랜잭션).
     *
     * 사원/거래처/테마 lookup 필수, category=OWN 시 productCode 로 제품 lookup. competitorTasting → SF Y/N flag.
     * 사원 costCenterCode 를 레거시 trigger 동등하게 세팅. 등록 결과를 목록 항목으로 반환.
     */
    @Transactional
    fun register(
        employeeId: Long,
        request: InspectionRegisterRequest,
        photos: List<MultipartFile>?
    ): InspectionListItem {
        val category = request.category ?: throw IllegalArgumentException("category는 필수입니다")
        val fieldType = InspectionFieldType.fromCode(request.fieldTypeCode)
            ?: throw IllegalArgumentException("유효하지 않은 fieldTypeCode: ${request.fieldTypeCode}")
        val themeId = request.themeId ?: throw IllegalArgumentException("themeId는 필수입니다")
        val accountId = request.accountId ?: throw IllegalArgumentException("accountId는 필수입니다")
        val activityDate = request.inspectionDate ?: throw IllegalArgumentException("inspectionDate는 필수입니다")

        if ((photos?.size ?: 0) > MAX_PHOTO_COUNT) {
            throw IllegalArgumentException("첨부 파일은 최대 ${MAX_PHOTO_COUNT}건 입니다")
        }

        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { IllegalArgumentException("사원을 찾을 수 없습니다") }
        val account = accountRepository.findById(accountId)
            .orElseThrow { IllegalArgumentException("거래처를 찾을 수 없습니다") }
        val theme = inspectionThemeRepository.findById(themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다") }

        // category=OWN(자사) 일 때만 제품 lookup (레거시 productType='자사' 분기 동등)
        val product = request.productCode
            ?.takeIf { category == InspectionCategory.OWN && it.isNotBlank() }
            ?.let { productRepository.findByProductCode(it) }

        val activity = SiteActivity(
            activityDate = activityDate,
            category = fieldType.displayName,
            productType = category.storedValue,
            description = request.description,
            sapAccountCode = account.externalKey,
            costCenterCode = employee.costCenterCode,
            competitorName = request.competitorName,
            competitorProductName = request.competitorProductName,
            competitorActivityDescription = request.competitorActivity,
            competitorProudctPrice = request.competitorProductPrice?.let { BigDecimal.valueOf(it.toLong()) },
            sampleTastFlag = booleanToSampleTastFlag(request.competitorTasting),
            salesQuantity = request.competitorSalesQuantity?.let { BigDecimal.valueOf(it.toLong()) },
            isDeleted = false,
            account = account,
            employee = employee,
            product = product,
            inspectionTheme = theme
        )

        val saved = siteActivityRepository.save(activity)

        photos?.forEachIndexed { _, file ->
            if (file.isEmpty) return@forEachIndexed
            val key = fileStorageService.uploadSiteActivityPhoto(file, saved.id)
            uploadFileRepository.save(
                UploadFile(
                    name = file.originalFilename,
                    uniqueKey = key,
                    fileSize = formatFileSize(file.size),
                    parentType = UploadFileParentTypes.SITE_ACTIVITY,
                    parentId = saved.id,
                    isDeleted = false
                )
            )
        }

        return InspectionListItem.from(saved)
    }

    /**
     * 오늘 기준 유효 기간(시작 ≤ 오늘 ≤ 종료) 테마 목록.
     */
    fun getThemes(): List<ThemeResponse> =
        inspectionThemeRepository
            .findActiveThemesByDate(LocalDate.now())
            .map { ThemeResponse.from(it) }

    /**
     * 현장유형 코드 목록 (SF Category picklist 고정값).
     */
    fun getFieldTypes(): List<InspectionFieldTypeResponse> =
        InspectionFieldType.entries.map { InspectionFieldTypeResponse.from(it) }

    /** Boolean competitorTasting → SF SampleTastFlag picklist (Y/N). null → null. */
    private fun booleanToSampleTastFlag(value: Boolean?): String? =
        when (value) {
            true -> "Y"
            false -> "N"
            null -> null
        }

    private fun composeS3Url(key: String): String =
        publicUrlResolver.resolve(key)!!

    private fun formatFileSize(bytes: Long): String =
        when {
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1fKB".format(bytes / 1024.0)
            else -> "${bytes}B"
        }
}
