package com.otoki.powersales.leave.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "holiday_master")
@SFObject("HolidayMaster__c")
class HolidayMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_master_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("HolidayDate__c")
    @Column(name = "holiday_date", nullable = false, unique = true)
    var holidayDate: LocalDate,

    @SFField("Name")
    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @SFField("Type__c")
    @Column(name = "type", nullable = false, length = 20)
    var type: String,

    @Column(name = "year", nullable = false)
    var year: Int
) : BaseEntity() {
    fun update(holidayDate: LocalDate, name: String, type: String) {
        this.holidayDate = holidayDate
        this.name = name
        this.type = type
        this.year = holidayDate.year

    }

    companion object {
        // Spec #604 Q1 옵션 2: SF `Type__c` picklist 값 직접 채택 (변환 매트릭스 없음)
        val VALID_TYPES = listOf("공휴일", "주말", "기타")
    }
}
