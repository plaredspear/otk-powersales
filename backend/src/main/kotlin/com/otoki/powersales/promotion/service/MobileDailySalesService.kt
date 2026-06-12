package com.otoki.powersales.promotion.service

import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.promotion.dto.request.DailySalesCloseRequest
import com.otoki.powersales.promotion.dto.request.DailySalesDraftRequest
import com.otoki.powersales.promotion.dto.response.DailySalesFormResponse
import com.otoki.powersales.promotion.dto.response.DailySalesResult
import com.otoki.powersales.promotion.entity.DailySalesDraft
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.exception.DailySalesAlreadyClosedException
import com.otoki.powersales.promotion.exception.DailySalesPhotoRequiredException
import com.otoki.powersales.promotion.exception.DailySalesProductRequiredException
import com.otoki.powersales.promotion.exception.PromotionEmployeeNotFoundException
import com.otoki.powersales.promotion.exception.PromotionForbiddenException
import com.otoki.powersales.promotion.repository.DailySalesDraftRepository
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

/**
 * 여사원 행사 일매출 마감 + 임시저장(draft) 서비스.
 *
 * ## 레거시 동작 (otg_PowerSales PromotionController.dailySalesProc / tempDailySalesProc)
 * - 입력: 본인 담당 행사 1건의 주력상품 판매수량/단가, 기타상품 수량/금액, 기타상품명, 사진.
 * - 분기: 임시저장(tempDailySalesProc) 은 검증 없이 `tmp_promotion` 에 upsert. 최종 마감(dailySalesProc) 은
 *   사진 + 상품 정보 검증 후 SF Apex REST `PromotionCloseRegist` 로 push 하고 `tmp_promotion` row 삭제.
 * - 외부호출: AWS S3 사진 업로드(key 반환) + SF Apex push.
 * - 부수효과: 임시저장 테이블 upsert/삭제, SF 마감 레코드 생성.
 *
 * ## 신규 차이 (deviation)
 * - SF Apex push 제거: 신규는 SoT 가 자체 DB 이므로 [PromotionEmployee] 본 row 에 직접 반영하고
 *   `promoCloseByTm = true` 로 마감한다. (legacy SF push → 로컬 엔티티 dirty checking)
 * - 임시저장은 신규 `daily_sales_draft` 테이블([DailySalesDraft])로 대체 (tmp_promotion 대응).
 * - 사진은 S3 private/ 업로드 후 key 만 보관([FileStorageService.uploadDailySalesPhoto]), 조회 시 presigned URL 로 변환.
 *   (레거시는 public S3 URL 이었으나, "본인 PE 만 조회" 권한 통제 정합 위해 private 전환 — 현장점검과 동일.)
 *
 * ## S3 객체 수명주기
 * 교체/폐기된 이전 S3 객체는 트랜잭션 롤백 시 데이터 손실(아직 참조 중이거나 커밋 전 삭제)을 막기 위해
 * 여기서 즉시 삭제하지 않는다. 고아 객체는 S3 lifecycle 정책으로 정리한다.
 */
@Service
@Transactional(readOnly = true)
class MobileDailySalesService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val dailySalesDraftRepository: DailySalesDraftRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService
) {

    fun getForm(userId: Long, promotionEmployeeId: Long): DailySalesFormResponse {
        val pe = loadOwnedPromotionEmployee(userId, promotionEmployeeId)
        val draft = dailySalesDraftRepository.findByPromotionEmployeeId(promotionEmployeeId)
        val imageKey = draft?.s3ImageUniqueKey ?: pe.s3ImageUniqueKey
        return DailySalesFormResponse.from(pe, draft, imageUrl(imageKey))
    }

    // 일매출 사진은 private/ 저장 → presigned URL 로만 조회 가능 (본인 PE 만 조회 권한 통제와 정합). key 없으면 null.
    private fun imageUrl(key: String?): String? =
        key?.takeIf { it.isNotBlank() }
            ?.let { storageService.getPresignedUrl(it, StorageConstants.DAILY_SALES_PRESIGN_TTL_SECONDS) }

    @Transactional
    fun close(
        userId: Long,
        promotionEmployeeId: Long,
        request: DailySalesCloseRequest,
        photo: MultipartFile?
    ): DailySalesResult {
        val pe = loadOwnedPromotionEmployee(userId, promotionEmployeeId)
        if (pe.promoCloseByTm) throw DailySalesAlreadyClosedException()

        // basePrice 는 행사마스터에서 사전 설정되는 값이므로 미전달 시 기존 값을 보존한다.
        // 나머지 매출 필드도 미전달 시 기존 PE 값을 유지하여 의도치 않은 초기화를 막는다.
        val basePrice = request.basePrice ?: pe.basePrice
        val primarySalesQuantity = request.primarySalesQuantity ?: pe.primarySalesQuantity
        val primarySalesPrice = request.primarySalesPrice ?: pe.primarySalesPrice
        val primaryProductAmount = request.primaryProductAmount ?: pe.primaryProductAmount
        val otherSalesQuantity = request.otherSalesQuantity ?: pe.otherSalesQuantity
        val otherSalesAmount = request.otherSalesAmount ?: pe.otherSalesAmount
        val description = request.description ?: pe.description

        val hasPrimary = primarySalesQuantity != null && primarySalesPrice != null
        val hasOther = otherSalesQuantity != null && otherSalesAmount != null
        if (!hasPrimary && !hasOther) throw DailySalesProductRequiredException()

        val draft = dailySalesDraftRepository.findByPromotionEmployeeId(promotionEmployeeId)
        val uploadedKey = uploadPhotoOrNull(photo, userId, pe)
        val finalKey = uploadedKey ?: draft?.s3ImageUniqueKey ?: pe.s3ImageUniqueKey
            ?: throw DailySalesPhotoRequiredException()

        pe.basePrice = basePrice
        pe.primarySalesQuantity = primarySalesQuantity
        pe.primarySalesPrice = primarySalesPrice
        pe.primaryProductAmount = primaryProductAmount
        pe.otherSalesQuantity = otherSalesQuantity
        pe.otherSalesAmount = otherSalesAmount
        pe.description = description
        pe.s3ImageUniqueKey = finalKey
        pe.actualAmount = computeActualAmount(primaryProductAmount, otherSalesAmount)
        pe.promoCloseByTm = true

        // 마감 완료 후 임시저장 row 제거 (이미지 key 는 PE 로 이관되었으므로 객체는 삭제하지 않음).
        if (draft != null) dailySalesDraftRepository.delete(draft)

        return DailySalesResult(
            promotionEmployeeId = pe.id,
            isClosed = true,
            actualAmount = pe.actualAmount,
            imageUrl = imageUrl(finalKey)
        )
    }

    @Transactional
    fun saveDraft(
        userId: Long,
        promotionEmployeeId: Long,
        request: DailySalesDraftRequest,
        photo: MultipartFile?
    ): DailySalesFormResponse {
        val pe = loadOwnedPromotionEmployee(userId, promotionEmployeeId)
        if (pe.promoCloseByTm) throw DailySalesAlreadyClosedException()

        val draft = dailySalesDraftRepository.findByPromotionEmployeeId(promotionEmployeeId)
            ?: DailySalesDraft(promotionEmployeeId = pe.id, employeeId = userId)

        draft.apply(
            basePrice = request.basePrice,
            primarySalesQuantity = request.primarySalesQuantity,
            primarySalesPrice = request.primarySalesPrice,
            primaryProductAmount = request.primaryProductAmount,
            otherSalesQuantity = request.otherSalesQuantity,
            otherSalesAmount = request.otherSalesAmount,
            description = request.description
        )

        uploadPhotoOrNull(photo, userId, pe)?.let { draft.s3ImageUniqueKey = it }

        val saved = dailySalesDraftRepository.save(draft)
        val imageKey = saved.s3ImageUniqueKey ?: pe.s3ImageUniqueKey
        return DailySalesFormResponse.from(pe, saved, imageUrl(imageKey))
    }

    @Transactional
    fun deleteDraft(userId: Long, promotionEmployeeId: Long) {
        loadOwnedPromotionEmployee(userId, promotionEmployeeId) // 소유권/존재 검증 목적 호출
        val draft = dailySalesDraftRepository.findByPromotionEmployeeId(promotionEmployeeId) ?: return
        dailySalesDraftRepository.delete(draft)
    }

    private fun loadOwnedPromotionEmployee(userId: Long, promotionEmployeeId: Long): PromotionEmployee {
        val pe = promotionEmployeeRepository.findById(promotionEmployeeId)
            .filter { it.isDeleted != true }
            .orElseThrow { PromotionEmployeeNotFoundException() }
        if (pe.employeeId != userId) throw PromotionForbiddenException()
        return pe
    }

    private fun uploadPhotoOrNull(photo: MultipartFile?, userId: Long, pe: PromotionEmployee): String? {
        if (photo == null || photo.isEmpty) return null
        return fileStorageService.uploadDailySalesPhoto(
            file = photo,
            userId = userId,
            eventId = pe.promotionId?.toString() ?: "",
            salesDate = pe.scheduleDate?.toString() ?: ""
        )
    }

    private fun computeActualAmount(primaryProductAmount: BigDecimal?, otherSalesAmount: BigDecimal?): Long {
        val primary = primaryProductAmount ?: BigDecimal.ZERO
        val other = otherSalesAmount ?: BigDecimal.ZERO
        return (primary + other).toLong()
    }
}
