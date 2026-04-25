package com.otoki.powersales.product.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import com.otoki.powersales.common.entity.AuditedEntity
/**
 * 즐겨찾기 제품 Entity
 *
 * V1 테이블: salesforce2.product_favorites (PK 없음 → @IdClass 복합 키)
 * @ManyToOne 관계를 raw String 컬럼으로 전환.
 */
@Entity
@Table(name = "product_favorite")
@IdClass(ProductFavoriteId::class)
@HCTable("product_favorites")
class FavoriteProduct(

    @Id
    @HCColumn("employeecode")
    @Column(name = "employee_code", length = 80)
    val employeeCode: String = "",

    @Id
    @HCColumn("productcode")
    @Column(name = "product_code", length = 80)
    val productCode: String = "",

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()