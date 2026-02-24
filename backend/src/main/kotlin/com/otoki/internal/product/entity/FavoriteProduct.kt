package com.otoki.internal.product.entity

import jakarta.persistence.*
import java.time.LocalDateTime

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
    val productCode: String = "",

    @Column(name = "inst_date")
    val instDate: LocalDateTime? = null,

    @Column(name = "upd_date")
    val updDate: LocalDateTime? = null
)

/* --- 주석 처리: V1에 없는 기존 필드 ---
id: Long — V2 IDENTITY PK → @IdClass(employeeCode, productCode) 로 대체
user: User (@ManyToOne) → employeeCode: String
product: Product (@ManyToOne) → productCode: String (컬럼 재정의)
productCode: String (기존 @Column 정의) → @Id 컬럼으로 승격
createdAt → instDate
@UniqueConstraint, @Table(indexes): V1 매핑 시 제거
--- */
