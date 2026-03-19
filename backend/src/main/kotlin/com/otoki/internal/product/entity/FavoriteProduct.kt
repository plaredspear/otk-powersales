package com.otoki.internal.product.entity

import com.otoki.internal.common.entity.BaseEntity
import jakarta.persistence.*

/**
 * 즐겨찾기 제품 Entity
 *
 * V1 테이블: salesforce2.product_favorites (PK 없음 → @IdClass 복합 키)
 * @ManyToOne 관계를 raw String 컬럼으로 전환.
 */
@Entity
@Table(name = "product_favorites")
@IdClass(ProductFavoriteId::class)
class FavoriteProduct(

    @Id
    @Column(name = "employeecode", length = 80)
    val employeeCode: String = "",

    @Id
    @Column(name = "productcode", length = 80)
    val productCode: String = ""
) : BaseEntity()
