package com.otoki.internal.product.service

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * FavoriteProduct Entity가 V1 스키마로 리매핑되어 @ManyToOne 관계(user, product)가
 * raw String 컬럼으로 변환됨. 기존 비즈니스 로직이 V2 Entity 구조를 직접 참조하므로
 * 컴파일 오류 발생 → 전체 주석 처리.

import com.otoki.internal.product.dto.response.FavoriteProductResponse
import com.otoki.internal.product.entity.FavoriteProduct
import com.otoki.internal.exception.AlreadyFavoritedException
import com.otoki.internal.exception.FavoriteNotFoundException
import com.otoki.internal.order.exception.InvalidOrderParameterException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.product.repository.FavoriteProductRepository
import com.otoki.internal.product.repository.ProductRepository
import com.otoki.internal.common.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    @Transactional
    fun addFavoriteProduct(userId: Long, productCode: String) {
        val product = productRepository.findByProductCode(productCode)
            ?: throw ProductNotFoundException(productCode)

        if (favoriteProductRepository.existsByUserIdAndProductCode(userId, productCode)) {
            throw AlreadyFavoritedException()
        }

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다") }

        val favorite = FavoriteProduct(
            user = user,
            product = product,
            productCode = productCode
        )
        favoriteProductRepository.save(favorite)
    }

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

--- */
