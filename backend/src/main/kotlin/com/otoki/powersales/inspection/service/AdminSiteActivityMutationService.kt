package com.otoki.powersales.inspection.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.inspection.dto.admin.AdminCreateSiteActivityRequest
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityMutationResponse
import com.otoki.powersales.inspection.dto.admin.AdminUpdateSiteActivityRequest
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.enums.InspectionFieldType
import com.otoki.powersales.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * admin 현장점검 결과(SiteActivity) 등록 Service.
 *
 * ## 레거시 매핑
 * - SF: `DKRetail__SiteAcitivity__c` 표준 New 폼 + `IF_REST_MOBILE_SiteActivityRegist` 필드 매핑
 * - Trigger: `SiteActivityTriggerHandler.beforeInsertSiteActivity` — 점검 사원의 CostCenterCode 를 자동 주입
 *
 * ## 신규 차이
 * - mobile 등록([SiteActivityService.register])이 로그인 사원 본인 기준인 반면, admin 은 점검 사원(`employeeId`)을
 *   관리자가 명시 지정 (다른 사원 대신 보정 입력). owner 는 [com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener]
 *   가 등록자(관리자) 로 자동 세팅.
 * - parent(SiteActivity) + child(UploadFile N건) 단일 `@Transactional`.
 */
@Service
@Transactional(readOnly = true)
class AdminSiteActivityMutationService(
    private val siteActivityRepository: SiteActivityRepository,
    private val inspectionThemeRepository: InspectionThemeRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val productRepository: ProductRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val fileStorageService: FileStorageService,
) {

    companion object {
        private const val MAX_PHOTO_COUNT = 10
    }

    /**
     * 현장점검 결과 등록 — SF New 폼 동등.
     *
     * category=OWN(자사) 면 productCode 로 제품 lookup(레거시 productType='자사' 분기). 점검 사원 CostCenterCode 자동 주입.
     */
    @Transactional
    fun create(request: AdminCreateSiteActivityRequest, photos: List<MultipartFile>?): AdminSiteActivityMutationResponse {
        val category = InspectionCategory.fromJson(request.category)
        val fieldType = InspectionFieldType.fromCode(request.fieldTypeCode)
            ?: throw IllegalArgumentException("유효하지 않은 fieldTypeCode: ${request.fieldTypeCode}")
        val activityDate = LocalDate.parse(request.inspectionDate)

        if ((photos?.size ?: 0) > MAX_PHOTO_COUNT) {
            throw IllegalArgumentException("첨부 파일은 최대 ${MAX_PHOTO_COUNT}건 입니다")
        }

        val employee = employeeRepository.findById(request.employeeId)
            .orElseThrow { IllegalArgumentException("점검 사원을 찾을 수 없습니다") }
        val account = accountRepository.findById(request.accountId)
            .orElseThrow { IllegalArgumentException("거래처를 찾을 수 없습니다") }
        val theme = inspectionThemeRepository.findById(request.themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다") }

        // category=OWN(자사) 일 때만 제품 lookup (레거시 productType='자사' 분기 동등)
        val product = request.productCode
            ?.takeIf { category == InspectionCategory.OWN && it.isNotBlank() }
            ?.let { productRepository.findByProductCode(it) }
        if (category == InspectionCategory.OWN && product == null && !request.productCode.isNullOrBlank()) {
            throw IllegalArgumentException("제품을 찾을 수 없습니다: ${request.productCode}")
        }

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
            inspectionTheme = theme,
        )

        val saved = siteActivityRepository.save(activity)

        photos?.forEach { file ->
            if (file.isEmpty) return@forEach
            val key = fileStorageService.uploadSiteActivityPhoto(file, saved.id)
            uploadFileRepository.save(
                UploadFile(
                    name = file.originalFilename,
                    uniqueKey = key,
                    fileSize = formatFileSize(file.size),
                    parentType = UploadFileParentTypes.SITE_ACTIVITY,
                    parentId = saved.id,
                    isDeleted = false,
                )
            )
        }

        return AdminSiteActivityMutationResponse(id = saved.id, name = saved.name)
    }

    /**
     * 현장점검 결과 수정 — SF 표준 Edit 폼 동등.
     *
     * 본문 필드 + lookup(거래처/사원/제품/테마) 재설정. 기존 sfid/name/owner/audit 보존.
     * SiteActivity trigger 는 insert 전용이라 수정 부수효과 없음 — costCenterCode 는 사원 기준 재계산.
     */
    @Transactional
    fun update(id: Long, request: AdminUpdateSiteActivityRequest): AdminSiteActivityMutationResponse {
        val existing = siteActivityRepository.findByIdAndIsDeletedFalse(id)
            ?: throw IllegalArgumentException("현장점검을 찾을 수 없습니다")

        val category = InspectionCategory.fromJson(request.category)
        val fieldType = InspectionFieldType.fromCode(request.fieldTypeCode)
            ?: throw IllegalArgumentException("유효하지 않은 fieldTypeCode: ${request.fieldTypeCode}")
        val activityDate = LocalDate.parse(request.inspectionDate)

        val employee = employeeRepository.findById(request.employeeId)
            .orElseThrow { IllegalArgumentException("점검 사원을 찾을 수 없습니다") }
        val account = accountRepository.findById(request.accountId)
            .orElseThrow { IllegalArgumentException("거래처를 찾을 수 없습니다") }
        val theme = inspectionThemeRepository.findById(request.themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다") }

        val product = request.productCode
            ?.takeIf { category == InspectionCategory.OWN && it.isNotBlank() }
            ?.let { productRepository.findByProductCode(it) }
        if (category == InspectionCategory.OWN && product == null && !request.productCode.isNullOrBlank()) {
            throw IllegalArgumentException("제품을 찾을 수 없습니다: ${request.productCode}")
        }

        val updated = SiteActivity(
            id = existing.id,
            sfid = existing.sfid,
            name = existing.name,
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
            accountSfid = existing.accountSfid,
            employeeSfid = existing.employeeSfid,
            productSfid = existing.productSfid,
            themeSfid = existing.themeSfid,
            ownerSfid = existing.ownerSfid,
            createdBySfid = existing.createdBySfid,
            lastModifiedBySfid = existing.lastModifiedBySfid,
            account = account,
            employee = employee,
            product = product,
            inspectionTheme = theme,
            ownerUser = existing.ownerUser,
            ownerGroup = existing.ownerGroup,
            createdBy = existing.createdBy,
            lastModifiedBy = existing.lastModifiedBy,
        ).also { it.createdAt = existing.createdAt }

        val saved = siteActivityRepository.save(updated)
        return AdminSiteActivityMutationResponse(id = saved.id, name = saved.name)
    }

    /** 현장점검 결과 삭제 — soft delete (Theme 와 동일 패턴, 운영 안전성). */
    @Transactional
    fun delete(id: Long) {
        val existing = siteActivityRepository.findByIdAndIsDeletedFalse(id)
            ?: throw IllegalArgumentException("현장점검을 찾을 수 없습니다")
        val deleted = SiteActivity(
            id = existing.id,
            sfid = existing.sfid,
            name = existing.name,
            activityDate = existing.activityDate,
            category = existing.category,
            productType = existing.productType,
            description = existing.description,
            title = existing.title,
            sapAccountCode = existing.sapAccountCode,
            costCenterCode = existing.costCenterCode,
            competitorName = existing.competitorName,
            competitorProductName = existing.competitorProductName,
            competitorActivityDescription = existing.competitorActivityDescription,
            competitorProudctPrice = existing.competitorProudctPrice,
            sampleTastFlag = existing.sampleTastFlag,
            salesQuantity = existing.salesQuantity,
            isDeleted = true,
            accountSfid = existing.accountSfid,
            employeeSfid = existing.employeeSfid,
            productSfid = existing.productSfid,
            themeSfid = existing.themeSfid,
            ownerSfid = existing.ownerSfid,
            createdBySfid = existing.createdBySfid,
            lastModifiedBySfid = existing.lastModifiedBySfid,
            account = existing.account,
            employee = existing.employee,
            product = existing.product,
            inspectionTheme = existing.inspectionTheme,
            ownerUser = existing.ownerUser,
            ownerGroup = existing.ownerGroup,
            createdBy = existing.createdBy,
            lastModifiedBy = existing.lastModifiedBy,
        ).also { it.createdAt = existing.createdAt }
        siteActivityRepository.save(deleted)
    }

    /**
     * SF Name AutoNumber(`SA{00000000}`) 채번 — prefix SA + 8자리 zero-pad.
     * sequence nextval (race-free). 레거시 SF 가 SObject 생성 시 자동 발행하던 Name 동등.
     */
    private fun generateName(): String = "SA" + "%08d".format(siteActivityRepository.getNextNameSeq())

    private fun booleanToSampleTastFlag(value: Boolean?): String? =
        when (value) {
            true -> "Y"
            false -> "N"
            null -> null
        }

    private fun formatFileSize(bytes: Long): String =
        when {
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1fKB".format(bytes / 1024.0)
            else -> "${bytes}B"
        }
}
