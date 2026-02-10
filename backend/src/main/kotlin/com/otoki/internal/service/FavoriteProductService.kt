package com.otoki.internal.service

import com.otoki.internal.dto.response.FavoriteProductResponse
import com.otoki.internal.entity.FavoriteProduct
import com.otoki.internal.exception.AlreadyFavoritedException
import com.otoki.internal.exception.FavoriteNotFoundException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.repository.FavoriteProductRepository
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 즐겨찾기 제품 Service
 */
@Service
@Transactional(readOnly = true)
class FavoriteProductService(
    private val favoriteProductRepository: FavoriteProductRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * 즐겨찾기 제품 목록 조회
     *
     * @param userId 로그인 사용자 ID
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20, 최대: 100)
     * @return 즐겨찾기 제품 페이지
     */
    fun getMyFavoriteProducts(
        userId: Long,
        page: Int?,
        size: Int?
    ): Page<FavoriteProductResponse> {
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE

        validatePagination(resolvedPage, resolvedSize)

        val pageable = PageRequest.of(resolvedPage, resolvedSize)
        val favoritePage = favoriteProductRepository.findByUserIdWithProduct(userId, pageable)

        return favoritePage.map { FavoriteProductResponse.from(it) }
    }

    /**
     * 즐겨찾기 추가
     *
     * @param userId 로그인 사용자 ID
     * @param productCode 제품코드
     * @throws ProductNotFoundException 제품을 찾을 수 없는 경우
     * @throws AlreadyFavoritedException 이미 즐겨찾기에 추가된 경우
     */
    @Transactional
    fun addFavoriteProduct(userId: Long, productCode: String) {
        // 1. 제품 존재 확인
        val product = productRepository.findByProductCode(productCode)
            ?: throw ProductNotFoundException(productCode)

        // 2. 중복 확인
        if (favoriteProductRepository.existsByUserIdAndProductCode(userId, productCode)) {
            throw AlreadyFavoritedException()
        }

        // 3. 사용자 조회
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다") }

        // 4. 즐겨찾기 생성 및 저장
        val favorite = FavoriteProduct(
            user = user,
            product = product,
            productCode = productCode
        )
        favoriteProductRepository.save(favorite)
    }

    /**
     * 즐겨찾기 해제
     *
     * @param userId 로그인 사용자 ID
     * @param productCode 제품코드
     * @throws FavoriteNotFoundException 즐겨찾기에 없는 경우
     */
    @Transactional
    fun removeFavoriteProduct(userId: Long, productCode: String) {
        val favorite = favoriteProductRepository.findByUserIdAndProductCode(userId, productCode)
            ?: throw FavoriteNotFoundException()

        favoriteProductRepository.delete(favorite)
    }

    private fun validatePagination(page: Int, size: Int) {
        if (page < 0) {
            throw InvalidOrderParameterException("페이지 번호는 0 이상이어야 합니다")
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw InvalidOrderParameterException("페이지 크기는 1~${MAX_PAGE_SIZE} 범위여야 합니다")
        }
    }
}
