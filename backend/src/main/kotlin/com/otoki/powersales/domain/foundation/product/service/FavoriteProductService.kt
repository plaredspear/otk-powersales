package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.dto.response.OrderProductDto
import com.otoki.powersales.domain.foundation.product.entity.FavoriteProduct
import com.otoki.powersales.domain.foundation.product.repository.FavoriteProductRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.exception.AlreadyFavoritedException
import com.otoki.powersales.platform.common.exception.FavoriteNotFoundException
import com.otoki.powersales.platform.common.exception.ProductNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 즐겨찾기 제품 Service (레거시 `ProductFavoriteService` 정합).
 *
 * 즐겨찾기는 **사번(empcode__c) + 제품코드** 단위로 저장된다(레거시 `product_favorites`).
 * 인증 주체는 내부 `userId`(employee_id) 이므로 [resolveEmployeeCode] 로 사번을 해석한 뒤 사용한다.
 *
 * 조회 응답은 주문 작성 화면(`add_product` 즐겨찾기 탭)이 그대로 소비하도록
 * 검색과 동일한 [OrderProductDto] 형태(`isFavorite = true`)로 내려준다.
 */
@Service
@Transactional(readOnly = true)
class FavoriteProductService(
    private val favoriteProductRepository: FavoriteProductRepository,
    private val productRepository: ProductRepository,
    private val employeeRepository: EmployeeRepository,
) {

    /** 내 즐겨찾기 제품 목록 — 최근 추가순. 삭제/단종으로 제품 마스터에 없는 코드는 제외한다. */
    fun getMyFavoriteProducts(userId: Long): List<OrderProductDto> {
        val employeeCode = resolveEmployeeCode(userId)
        val favorites = favoriteProductRepository.findByEmployeeCodeOrderByCreatedAtDesc(employeeCode)
        if (favorites.isEmpty()) return emptyList()

        val productsByCode = productRepository
            .findByProductCodeIn(favorites.map { it.productCode })
            .associateBy { it.productCode }

        return favorites.mapNotNull { favorite ->
            productsByCode[favorite.productCode]?.let { product ->
                OrderProductDto.from(product).copy(isFavorite = true)
            }
        }
    }

    /**
     * 내 즐겨찾기 제품코드 집합 — 검색 결과의 즐겨찾기 표시(`isFavorite`)용.
     *
     * 레거시 `productMapper.xml` 의 `product_favorites` 서브쿼리(검색 행마다 즐겨찾기 여부 표시)와 정합.
     */
    fun getFavoriteProductCodes(userId: Long): Set<String> {
        val employeeCode = resolveEmployeeCode(userId)
        return favoriteProductRepository.findByEmployeeCodeOrderByCreatedAtDesc(employeeCode)
            .mapTo(mutableSetOf()) { it.productCode }
    }

    @Transactional
    fun addFavoriteProduct(userId: Long, productCode: String) {
        val employeeCode = resolveEmployeeCode(userId)
        productRepository.findByProductCode(productCode)
            ?: throw ProductNotFoundException(productCode)

        if (favoriteProductRepository.existsByEmployeeCodeAndProductCode(employeeCode, productCode)) {
            throw AlreadyFavoritedException()
        }

        favoriteProductRepository.save(
            FavoriteProduct(employeeCode = employeeCode, productCode = productCode)
        )
    }

    @Transactional
    fun removeFavoriteProduct(userId: Long, productCode: String) {
        val employeeCode = resolveEmployeeCode(userId)
        val favorite = favoriteProductRepository.findByEmployeeCodeAndProductCode(employeeCode, productCode)
            ?: throw FavoriteNotFoundException()
        favoriteProductRepository.delete(favorite)
    }

    /** 인증 주체(employee_id) → 사번(empcode__c). 사번 미보유 사원은 즐겨찾기 대상이 아니므로 비정상으로 간주. */
    private fun resolveEmployeeCode(userId: Long): String {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }
        return employee.employeeCode
            ?: throw IllegalStateException("즐겨찾기 요청 사원의 사번이 null - 비정상: $userId")
    }
}
