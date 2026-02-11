package com.otoki.internal.service

import com.otoki.internal.dto.request.InspectionCreateRequest
import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.*
import com.otoki.internal.exception.*
import com.otoki.internal.repository.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

/**
 * 현장 점검 Service
 */
@Service
@Transactional(readOnly = true)
class InspectionService(
    private val inspectionRepository: InspectionRepository,
    private val inspectionThemeRepository: InspectionThemeRepository,
    private val inspectionFieldTypeRepository: InspectionFieldTypeRepository,
    private val inspectionPhotoRepository: InspectionPhotoRepository,
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService
) {

    /**
     * 현장 점검 목록 조회
     *
     * @param userId 사용자 ID
     * @param fromDate 시작일 (YYYY-MM-DD)
     * @param toDate 종료일 (YYYY-MM-DD)
     * @param storeId 거래처 ID (선택)
     * @param category 분류 (선택: OWN, COMPETITOR)
     * @return 점검 목록
     */
    fun getInspectionList(
        userId: Long,
        fromDate: String,
        toDate: String,
        storeId: Long?,
        category: String?
    ): InspectionListResponse {
        // 날짜 파싱 및 검증
        val from = parseDate(fromDate)
        val to = parseDate(toDate)

        if (to.isBefore(from)) {
            throw InvalidDateRangeException()
        }

        // 분류 검증 (있는 경우)
        val categoryEnum = category?.let {
            try {
                InspectionCategory.valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw InvalidCategoryException()
            }
        }

        // 목록 조회
        val inspections = inspectionRepository.findByUserIdWithFilters(
            userId = userId,
            fromDate = from,
            toDate = to,
            storeId = storeId,
            category = categoryEnum
        )

        // DTO 변환
        val items = inspections.map { InspectionListItemResponse.from(it) }

        return InspectionListResponse.of(items)
    }

    /**
     * 현장 점검 상세 조회
     *
     * @param inspectionId 점검 ID
     * @param userId 요청 사용자 ID
     * @return 점검 상세
     */
    fun getInspectionDetail(inspectionId: Long, userId: Long): InspectionDetailResponse {
        // 점검 조회 (사진 포함)
        val inspection = inspectionRepository.findByIdWithPhotos(inspectionId)
            ?: throw InspectionNotFoundException()

        // 소유권 검증
        if (inspection.user.id != userId) {
            throw InspectionForbiddenException()
        }

        return InspectionDetailResponse.from(inspection)
    }

    /**
     * 현장 점검 등록
     *
     * @param request 등록 요청
     * @param photos 사진 파일 (1~2장)
     * @param userId 사용자 ID
     * @return 생성된 점검 정보
     */
    @Transactional
    fun createInspection(
        request: InspectionCreateRequest,
        photos: Array<MultipartFile>,
        userId: Long
    ): InspectionListItemResponse {
        // 사진 검증
        validatePhotos(photos)

        // 테마 검증
        val theme = inspectionThemeRepository.findByIdOrNull(request.themeId!!)
            ?: throw ThemeNotFoundException()

        // 거래처 검증
        val store = storeRepository.findByIdOrNull(request.storeId!!)
            ?: throw StoreNotFoundException()

        // 현장 유형 검증
        val fieldType = inspectionFieldTypeRepository.findById(request.fieldTypeCode!!)
            .orElseThrow { FieldTypeNotFoundException() }

        // 분류 검증
        val category = try {
            InspectionCategory.valueOf(request.category!!)
        } catch (e: IllegalArgumentException) {
            throw InvalidCategoryException()
        }

        // 사용자 조회
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다", org.springframework.http.HttpStatus.NOT_FOUND)

        // 분류별 필수 필드 검증
        validateRequiredFields(request, category)

        // 점검 엔티티 생성
        val inspection = Inspection(
            user = user,
            store = store,
            theme = theme,
            category = category,
            storeName = store.storeName,
            inspectionDate = parseDate(request.inspectionDate!!),
            fieldTypeCode = fieldType.code,
            fieldTypeName = fieldType.name,
            description = request.description,
            productCode = request.productCode,
            productName = if (request.productCode != null) getProductName(request.productCode) else null,
            competitorName = request.competitorName,
            competitorActivity = request.competitorActivity,
            competitorTasting = request.competitorTasting,
            competitorProductName = request.competitorProductName,
            competitorProductPrice = request.competitorProductPrice,
            competitorSalesQuantity = request.competitorSalesQuantity
        )

        // 점검 저장
        val savedInspection = inspectionRepository.save(inspection)

        // 사진 업로드 및 저장
        photos.forEach { photo ->
            val url = fileStorageService.uploadInspectionPhoto(photo, userId, savedInspection.id)
            val inspectionPhoto = InspectionPhoto(
                inspection = savedInspection,
                url = url,
                originalFileName = photo.originalFilename ?: "unknown",
                fileSize = photo.size,
                contentType = photo.contentType ?: "image/jpeg"
            )
            inspectionPhotoRepository.save(inspectionPhoto)
        }

        return InspectionListItemResponse.from(savedInspection)
    }

    /**
     * 테마 목록 조회 (오늘 날짜 기준 활성 테마)
     *
     * @return 테마 목록
     */
    fun getThemes(): List<ThemeResponse> {
        val today = LocalDate.now()
        val themes = inspectionThemeRepository.findActiveThemesByDate(today)
        return themes.map { ThemeResponse.from(it) }
    }

    /**
     * 현장 유형 목록 조회
     *
     * @return 현장 유형 목록
     */
    fun getFieldTypes(): List<FieldTypeResponse> {
        val fieldTypes = inspectionFieldTypeRepository.findActiveFieldTypes()
        return fieldTypes.map { FieldTypeResponse.from(it) }
    }

    // ----- Private Helper Methods -----

    /**
     * 날짜 문자열 파싱
     */
    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            throw MissingRequiredFieldException("날짜 형식이 올바르지 않습니다: $dateStr")
        }
    }

    /**
     * 사진 파일 검증
     */
    private fun validatePhotos(photos: Array<MultipartFile>) {
        if (photos.isEmpty()) {
            throw PhotoRequiredException()
        }
        if (photos.size > 2) {
            throw PhotoCountExceededException()
        }
    }

    /**
     * 분류별 필수 필드 검증
     */
    private fun validateRequiredFields(request: InspectionCreateRequest, category: InspectionCategory) {
        when (category) {
            InspectionCategory.OWN -> {
                if (request.productCode.isNullOrBlank()) {
                    throw MissingRequiredFieldException("자사 점검 시 제품 선택은 필수입니다")
                }
                // 제품 존재 여부 검증
                productRepository.findByProductCode(request.productCode)
                    ?: throw ProductNotFoundException()
            }
            InspectionCategory.COMPETITOR -> {
                if (request.competitorName.isNullOrBlank()) {
                    throw MissingRequiredFieldException("경쟁사 점검 시 경쟁사명은 필수입니다")
                }
                if (request.competitorActivity.isNullOrBlank()) {
                    throw MissingRequiredFieldException("경쟁사 점검 시 활동 내용은 필수입니다")
                }
                if (request.competitorTasting == null) {
                    throw MissingRequiredFieldException("경쟁사 점검 시 시식 여부는 필수입니다")
                }

                // 시식 = 예인 경우 추가 검증
                if (request.competitorTasting == true) {
                    if (request.competitorProductName.isNullOrBlank()) {
                        throw MissingRequiredFieldException("시식 정보를 입력해주세요 (상품명)")
                    }
                    if (request.competitorProductPrice == null) {
                        throw MissingRequiredFieldException("시식 정보를 입력해주세요 (제품 가격)")
                    }
                    if (request.competitorSalesQuantity == null) {
                        throw MissingRequiredFieldException("시식 정보를 입력해주세요 (판매 수량)")
                    }
                }
            }
        }
    }

    /**
     * 제품 코드로 제품명 조회
     */
    private fun getProductName(productCode: String): String {
        val product = productRepository.findByProductCode(productCode)
            ?: return productCode
        return product.productName
    }
}
