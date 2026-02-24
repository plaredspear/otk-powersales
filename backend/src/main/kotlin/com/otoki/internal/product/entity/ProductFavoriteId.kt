package com.otoki.internal.product.entity

import java.io.Serializable

/**
 * FavoriteProduct 복합 키
 *
 * product_favorites 테이블의 (employeecode, productcode) 복합 PK.
 * 원본 테이블에 PK가 없으므로 @IdClass 전략으로 논리적 복합 키 구성.
 */
class ProductFavoriteId(
    val employeeCode: String = "",
    val productCode: String = ""
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProductFavoriteId) return false
        return employeeCode == other.employeeCode && productCode == other.productCode
    }

    override fun hashCode(): Int {
        var result = employeeCode.hashCode()
        result = 31 * result + productCode.hashCode()
        return result
    }
}
