package com.otoki.internal.shelflife.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 유통기한 관리 Entity
 *
 * V1 테이블: salesforce2.expirationdate__mng (sequence PK: seq)
 * @ManyToOne 관계를 raw String 컬럼으로 전환.
 */
@Entity
@Table(name = "expirationdate__mng")
class ShelfLife(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    val seq: Int = 0,

    @Column(name = "account_id", length = 100)
    val accountId: String? = null,

    @Column(name = "account_code", length = 100)
    val accountCode: String? = null,

    @Column(name = "employee_id", length = 100)
    val employeeId: String? = null,

    @Column(name = "product_id", length = 100)
    val productId: String? = null,

    @Column(name = "product_code", length = 100)
    val productCode: String? = null,

    @Column(name = "expiration_date")
    val expirationDate: LocalDate? = null,

    @Column(name = "alarm_date")
    val alarmDate: LocalDate? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "inst_dt")
    val instDt: LocalDateTime? = null,

    @Column(name = "updt_dt")
    val updtDt: LocalDateTime? = null
)

/* --- 주석 처리: V1에 없는 기존 필드 ---
id: Long — V2 IDENTITY PK → seq: Int (sequence) 로 대체
user: User (@ManyToOne) → employeeId: String
store: Account (@ManyToOne) → accountId: String
product: Product (@ManyToOne) → productId: String
productName: V1에 없음 (비정규화 필드)
storeName: V1에 없음 (비정규화 필드)
expiryDate → expirationDate
alertDate → alarmDate
alertSent: V1에 없음
createdAt → instDt
updatedAt → updtDt
update(): V1에서 앱 직접 갱신 안 함
@PreUpdate onPreUpdate(): V1에서 앱 직접 갱신 안 함
@UniqueConstraint, @Table(indexes): V1 매핑 시 제거
--- */
