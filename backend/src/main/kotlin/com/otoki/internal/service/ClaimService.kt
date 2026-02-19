/*
package com.otoki.internal.service

import com.otoki.internal.dto.request.ClaimCreateRequest
import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.*
import com.otoki.internal.exception.*
import com.otoki.internal.repository.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

/ **
 * 클레임 Service
 * /
@Service
@Transactional(readOnly = true)
class ClaimService(
    private val claimRepository: ClaimRepository,
    private val claimPhotoRepository: ClaimPhotoRepository,
    private val claimCategoryRepository: ClaimCategoryRepository,
    private val claimSubcategoryRepository: ClaimSubcategoryRepository,
    private val claimPurchaseMethodRepository: ClaimPurchaseMethodRepository,
    private val claimRequestTypeRepository: ClaimRequestTypeRepository,
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService
) {

    / **
     * 클레임 등록
     *
     * @param userId JWT에서 추출한 사용자 ID
     * @param request 클레임 등록 요청
     * @param defectPhoto 불량 사진 (필수)
     * @param labelPhoto 일부인 사진 (필수)
     * @param receiptPhoto 구매 영수증 사진 (선택)
     * @return 클레임 등록 결과
     * /
    @Transactional
    fun createClaim(
        userId: Long,
        request: ClaimCreateRequest,
        defectPhoto: MultipartFile,
        labelPhoto: MultipartFile,
        receiptPhoto: MultipartFile?
    ): ClaimCreateResponse {
        // 1. 사용자 조회
        val user = userRepository.findByIdOrNull(userId)
            ?: throw InvalidParameterException("사용자를 찾을 수 없습니다")

        // 2. 거래처 존재 여부 확인
        val store = storeRepository.findByIdOrNull(request.storeId!!)
            ?: throw StoreNotFoundException()

        // 3. 제품 존재 여부 확인
        val product = productRepository.findByProductCode(request.productCode!!)
            ?: throw ProductNotFoundException()

        // 4. 기한 종류 유효성 검증
        val dateType = try {
            ClaimDateType.valueOf(request.dateType!!)
        } catch (e: IllegalArgumentException) {
            throw InvalidDateTypeException()
        }

        // 5. 기한 날짜 파싱
        val date = try {
            LocalDate.parse(request.date!!)
        } catch (e: Exception) {
            throw InvalidParameterException("유효하지 않은 날짜 형식입니다")
        }

        // 6. 클레임 종류1 존재 여부 확인
        val category = claimCategoryRepository.findByIdOrNull(request.categoryId!!)
            ?: throw ClaimCategoryNotFoundException()

        if (!category.isActive) {
            throw ClaimCategoryNotFoundException()
        }

        // 7. 클레임 종류2 존재 여부 확인
        val subcategory = claimSubcategoryRepository.findByIdOrNull(request.subcategoryId!!)
            ?: throw ClaimSubcategoryNotFoundException()

        if (!subcategory.isActive) {
            throw ClaimSubcategoryNotFoundException()
        }

        // 8. 종류2가 종류1에 속하는지 확인
        if (subcategory.category.id != category.id) {
            throw InvalidParameterException("클레임 세부 종류가 선택한 종류에 속하지 않습니다")
        }

        // 9. 구매 정보 조건부 검증
        var purchaseMethodName: String? = null
        if (request.purchaseAmount != null && request.purchaseAmount > 0) {
            // 구매 금액이 있으면 구매 방법과 영수증 사진 필수
            if (request.purchaseMethodCode.isNullOrBlank() || receiptPhoto == null) {
                throw PurchaseInfoRequiredException()
            }

            // 구매 방법 유효성 확인
            val purchaseMethod = claimPurchaseMethodRepository.findByIdOrNull(request.purchaseMethodCode)
                ?: throw PurchaseMethodNotFoundException()

            if (!purchaseMethod.isActive) {
                throw PurchaseMethodNotFoundException()
            }

            purchaseMethodName = purchaseMethod.name
        }

        // 10. 요청사항 유효성 확인 (선택)
        var requestTypeName: String? = null
        if (!request.requestTypeCode.isNullOrBlank()) {
            val requestType = claimRequestTypeRepository.findByIdOrNull(request.requestTypeCode)
                ?: throw RequestTypeNotFoundException()

            if (!requestType.isActive) {
                throw RequestTypeNotFoundException()
            }

            requestTypeName = requestType.name
        }

        // 11. Claim 엔티티 생성 및 저장
        val claim = Claim(
            user = user,
            store = store,
            storeName = store.storeName,
            productCode = product.productCode,
            productName = product.productName,
            dateType = dateType,
            date = date,
            category = category,
            subcategory = subcategory,
            defectDescription = request.defectDescription!!,
            defectQuantity = request.defectQuantity!!,
            purchaseAmount = request.purchaseAmount,
            purchaseMethodCode = request.purchaseMethodCode,
            purchaseMethodName = purchaseMethodName,
            requestTypeCode = request.requestTypeCode,
            requestTypeName = requestTypeName,
            status = ClaimStatus.SUBMITTED
        )

        val savedClaim = claimRepository.save(claim)

        // 12. 사진 업로드 및 ClaimPhoto 엔티티 생성
        val photos = mutableListOf<ClaimPhoto>()

        // 불량 사진 (필수)
        val defectPhotoUrl = fileStorageService.uploadClaimPhoto(
            file = defectPhoto,
            userId = userId,
            claimId = savedClaim.id,
            photoType = ClaimPhotoType.DEFECT.name
        )
        photos.add(
            ClaimPhoto(
                claim = savedClaim,
                photoType = ClaimPhotoType.DEFECT,
                url = defectPhotoUrl,
                originalFileName = defectPhoto.originalFilename ?: "unknown",
                fileSize = defectPhoto.size,
                contentType = defectPhoto.contentType ?: "image/jpeg"
            )
        )

        // 일부인 사진 (필수)
        val labelPhotoUrl = fileStorageService.uploadClaimPhoto(
            file = labelPhoto,
            userId = userId,
            claimId = savedClaim.id,
            photoType = ClaimPhotoType.LABEL.name
        )
        photos.add(
            ClaimPhoto(
                claim = savedClaim,
                photoType = ClaimPhotoType.LABEL,
                url = labelPhotoUrl,
                originalFileName = labelPhoto.originalFilename ?: "unknown",
                fileSize = labelPhoto.size,
                contentType = labelPhoto.contentType ?: "image/jpeg"
            )
        )

        // 구매 영수증 사진 (선택)
        if (receiptPhoto != null) {
            val receiptPhotoUrl = fileStorageService.uploadClaimPhoto(
                file = receiptPhoto,
                userId = userId,
                claimId = savedClaim.id,
                photoType = ClaimPhotoType.RECEIPT.name
            )
            photos.add(
                ClaimPhoto(
                    claim = savedClaim,
                    photoType = ClaimPhotoType.RECEIPT,
                    url = receiptPhotoUrl,
                    originalFileName = receiptPhoto.originalFilename ?: "unknown",
                    fileSize = receiptPhoto.size,
                    contentType = receiptPhoto.contentType ?: "image/jpeg"
                )
            )
        }

        claimPhotoRepository.saveAll(photos)

        // 13. 응답 생성
        return ClaimCreateResponse.from(savedClaim)
    }

    / **
     * 폼 초기화 데이터 조회
     * 종류1+종류2, 구매방법, 요청사항을 한 번에 반환
     *
     * @return 폼 초기화 데이터
     * /
    fun getFormData(): ClaimFormDataResponse {
        // 1. 활성 종류1 목록 조회 (sortOrder 순)
        val categories = claimCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc()

        // 2. 각 종류1에 속하는 활성 종류2 목록 조회 및 중첩
        val categoriesWithSubcategories = categories.map { category ->
            val subcategories = claimSubcategoryRepository
                .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(category.id)
                .map { ClaimSubcategoryResponse.from(it) }

            ClaimCategoryWithSubcategoriesResponse.from(category, subcategories)
        }

        // 3. 활성 구매 방법 목록 조회 (sortOrder 순)
        val purchaseMethods = claimPurchaseMethodRepository
            .findByIsActiveTrueOrderBySortOrderAsc()
            .map { PurchaseMethodResponse.from(it) }

        // 4. 활성 요청사항 목록 조회 (sortOrder 순)
        val requestTypes = claimRequestTypeRepository
            .findByIsActiveTrueOrderBySortOrderAsc()
            .map { ClaimRequestTypeResponse.from(it) }

        // 5. 통합 응답 생성
        return ClaimFormDataResponse(
            categories = categoriesWithSubcategories,
            purchaseMethods = purchaseMethods,
            requestTypes = requestTypes
        )
    }
}
*/
