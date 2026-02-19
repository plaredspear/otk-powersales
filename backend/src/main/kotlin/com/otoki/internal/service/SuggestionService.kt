package com.otoki.internal.service

import com.otoki.internal.dto.request.SuggestionCreateRequest
import com.otoki.internal.dto.response.SuggestionCreateResponse
import com.otoki.internal.entity.*
import com.otoki.internal.exception.*
import com.otoki.internal.repository.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 제안 Service
 */
@Service
@Transactional(readOnly = true)
class SuggestionService(
    private val suggestionRepository: SuggestionRepository,
    // private val suggestionPhotoRepository: SuggestionPhotoRepository,  // Phase2: PG 대응 테이블 없음
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService
) {

    companion object {
        private const val MAX_PHOTO_COUNT = 2
        private const val MAX_PHOTO_SIZE = 10 * 1024 * 1024L // 10MB
    }

    /**
     * 제안 등록
     *
     * @param userId JWT에서 추출한 사용자 ID
     * @param request 제안 등록 요청
     * @param photos 사진 목록 (최대 2장, 선택)
     * @return 제안 등록 결과
     */
    @Transactional
    fun createSuggestion(
        userId: Long,
        request: SuggestionCreateRequest,
        photos: List<MultipartFile>?
    ): SuggestionCreateResponse {
        // 1. 사용자 조회
        val user = userRepository.findByIdOrNull(userId)
            ?: throw InvalidParameterException("사용자를 찾을 수 없습니다")

        // 2. category 유효성 검증
        val category = try {
            SuggestionCategory.valueOf(request.category!!)
        } catch (e: IllegalArgumentException) {
            throw InvalidCategoryException()
        }

        // 3. category별 처리
        var productCode: String? = null
        var productName: String? = null

        when (category) {
            SuggestionCategory.EXISTING_PRODUCT -> {
                // 기존제품인 경우 productCode 필수
                if (request.productCode.isNullOrBlank()) {
                    throw ProductRequiredForExistingException()
                }

                // 제품 존재 여부 확인
                val product = productRepository.findByProductCode(request.productCode)
                    ?: throw ProductNotFoundException(request.productCode)

                // 제품명 비정규화 저장
                productCode = product.productCode
                productName = product.productName
            }
            SuggestionCategory.NEW_PRODUCT -> {
                // 신제품인 경우 productCode 무시 (null 처리)
                productCode = null
                productName = null
            }
        }

        // 4. 사진 검증
        if (photos != null && photos.isNotEmpty()) {
            // 사진 개수 검증
            if (photos.size > MAX_PHOTO_COUNT) {
                throw PhotoCountExceededException()
            }

            // 각 사진 검증
            photos.forEach { photo ->
                // 파일 크기 검증
                if (photo.size > MAX_PHOTO_SIZE) {
                    throw PhotoSizeExceededException()
                }

                // 파일 타입 검증
                val contentType = photo.contentType
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw InvalidPhotoException("사진 파일 형식이 올바르지 않습니다")
                }

                // 빈 파일 검증
                if (photo.isEmpty) {
                    throw InvalidPhotoException("빈 파일은 업로드할 수 없습니다")
                }
            }
        }

        // 5. Suggestion 엔티티 생성 및 저장
        val suggestion = Suggestion(
            user = user,
            category = category,
            productCode = productCode,
            productName = productName,
            title = request.title!!,
            content = request.content!!,
            status = SuggestionStatus.SUBMITTED
        )

        val savedSuggestion = suggestionRepository.save(suggestion)

        // Phase2: SuggestionPhoto PG 대응 테이블 없음 - 사진 업로드 주석 처리
        // if (photos != null && photos.isNotEmpty()) {
        //     val photoEntities = photos.mapIndexed { index, photo ->
        //         val photoUrl = fileStorageService.uploadSuggestionPhoto(
        //             file = photo, userId = userId, suggestionId = savedSuggestion.id, sortOrder = index
        //         )
        //         SuggestionPhoto(
        //             suggestion = savedSuggestion, url = photoUrl,
        //             originalFileName = photo.originalFilename ?: "unknown",
        //             fileSize = photo.size, contentType = photo.contentType ?: "image/jpeg",
        //             sortOrder = index
        //         )
        //     }
        //     suggestionPhotoRepository.saveAll(photoEntities)
        // }

        // 7. 응답 생성
        return SuggestionCreateResponse.from(savedSuggestion)
    }
}
