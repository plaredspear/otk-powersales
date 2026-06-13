package com.otoki.powersales.inspection.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
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
import com.otoki.powersales.inspection.repository.SiteActivityDraftRepository
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
    private val productRepository: ProductRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService,
    private val siteActivityDraftRepository: SiteActivityDraftRepository
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
            name = generateName(),
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

        // 정식 등록 성공 시 해당 사원의 임시저장 row 삭제 (레거시 tempFieldChkProc 후처리 동등)
        siteActivityDraftRepository.findByEmployeeId(employeeId)?.let { siteActivityDraftRepository.delete(it) }

        return InspectionListItem.from(saved)
    }

    /**
     * 현장점검 등록용 활성 테마 목록(레거시 fieldChk selectTheme 정합).
     * 오늘이 기간 내이고, branch_code 가 공통 화이트리스트이거나 사원 코스트센터와 일치하는 테마.
     * 코스트센터는 URL 파라미터가 아닌 인증 사원에서 도출(레거시 URL 조작 취약점 제거).
     */
    fun getThemes(employeeId: Long): List<ThemeResponse> {
        val costCenterCode = employeeRepository.findById(employeeId).orElse(null)?.costCenterCode
        return inspectionThemeRepository
            .findActiveThemesByDate(LocalDate.now(), costCenterCode)
            .map { ThemeResponse.from(it) }
    }

    /**
     * 현장유형 코드 목록 (SF Category picklist 고정값).
     */
    fun getFieldTypes(): List<InspectionFieldTypeResponse> =
        InspectionFieldType.entries.map { InspectionFieldTypeResponse.from(it) }

    /**
     * SF Name AutoNumber(`SA{00000000}`) 채번 — prefix SA + 8자리 zero-pad.
     * sequence nextval (race-free). 레거시 SF 가 SObject 생성 시 자동 발행하던 Name 동등.
     */
    private fun generateName(): String = "SA" + "%08d".format(siteActivityRepository.getNextNameSeq())

    /** Boolean competitorTasting → SF SampleTastFlag picklist (Y/N). null → null. */
    private fun booleanToSampleTastFlag(value: Boolean?): String? =
        when (value) {
            true -> "Y"
            false -> "N"
            null -> null
        }

    // 현장점검 사진은 private/ 저장 → presigned URL 로만 조회 가능 (본인만 조회 권한 통제와 정합).
    private fun composeS3Url(key: String): String =
        storageService.getPresignedUrl(key, StorageConstants.SITE_ACTIVITY_PRESIGN_TTL_SECONDS)

    private fun formatFileSize(bytes: Long): String =
        when {
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1fKB".format(bytes / 1024.0)
            else -> "${bytes}B"
        }
}
