package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
@Entity
@Table(name = "agreement_word")
@HCTable("agreementword__c")
@SFObject("AgreementWord__c")
class AgreementWord(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_word_id")
    val id: Int = 0,

    @HCColumn("name")
    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("contents__c")
    @SFField("Contents__c")
    @Column(name = "contents", length = 8000)
    val contents: String? = null,

    @HCColumn("active__c")
    @SFField("Active__c")
    @Column(name = "active")
    var active: Boolean? = null,

    @HCColumn("activedate__c")
    @SFField("ActiveDate__c")
    @Column(name = "active_date")
    var activeDate: LocalDate? = null,

    @HCColumn("afteractivedate__c")
    @SFField("AfterActiveDate__c")
    @Column(name = "after_active_date")
    var afterActiveDate: LocalDate? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity() {

    /**
     * 활성 약관 비활성화 (cycle batch — rotation 분기의 oldObj 처리, 스펙 #654 / Q1 legacy 동등).
     *
     * 레거시 매핑: `AgreementWordBatch.cls:34-37` (`oldObj.Active__c=False; oldObj.AfterActiveDate__c=null`).
     */
    fun deactivate() {
        this.active = false
        this.afterActiveDate = null
    }

    /**
     * 도래 약관 활성화 + 다음 cycle 6개월 후로 갱신 (스펙 #654 / Q1 + Q6 legacy 동등).
     *
     * 레거시 매핑: `AgreementWordBatch.cls:39-41` (rotation 의 newObj 활성화) + `cls:54-56` (new-only 활성화).
     * 동작: ActiveDate = afterActiveDate (도래일을 활성일로 확정), afterActiveDate = activeDay + 6개월.
     */
    fun activate(activeDay: LocalDate) {
        this.active = true
        this.activeDate = activeDay
        this.afterActiveDate = activeDay.plusMonths(CYCLE_MONTHS)
    }

    companion object {
        /** 재동의 cycle 주기 — 6개월 (스펙 #654 / Q6 hardcoded, legacy `AgreementWordBatch.cls:41,56`). */
        const val CYCLE_MONTHS = 6L
    }
}