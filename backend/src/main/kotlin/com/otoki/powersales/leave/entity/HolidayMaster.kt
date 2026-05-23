package com.otoki.powersales.leave.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.leave.entity.converter.HolidayTypeConverter
import com.otoki.powersales.leave.enums.HolidayType
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

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("HolidayDate__c")
    @Column(name = "holiday_date", nullable = false, unique = true)
    var holidayDate: LocalDate,

    @SFField("Name")
    @Column(name = "name", nullable = false, length = 80)
    var name: String,

    @SFField("Type__c")
    @Convert(converter = HolidayTypeConverter::class)
    @Column(name = "type", nullable = false, length = 255)
    var type: HolidayType,

    @Column(name = "year", nullable = false)
    var year: Int,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null

) : BaseEntity() {
    fun update(holidayDate: LocalDate, name: String, type: HolidayType) {
        this.holidayDate = holidayDate
        this.name = name
        this.type = type
        this.year = holidayDate.year
    }
}
