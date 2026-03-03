package com.otoki.internal.shelflife.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 유통기한 관리 Entity
 *
 * V1 테이블: salesforce2.expirationdate__mng (sequence PK: seq)
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
    var expirationDate: LocalDate? = null,

    @Column(name = "alarm_date")
    var alarmDate: LocalDate? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "inst_dt")
    val instDt: LocalDateTime? = null,

    @Column(name = "updt_dt")
    var updtDt: LocalDateTime? = null
) {
    fun update(expirationDate: LocalDate, alarmDate: LocalDate, description: String?) {
        this.expirationDate = expirationDate
        this.alarmDate = alarmDate
        this.description = description
        this.updtDt = LocalDateTime.now()
    }
}
