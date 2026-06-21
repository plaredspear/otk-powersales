package com.otoki.powersales.domain.org.leave.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.org.leave.entity.converter.HolidayTypeConverter
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.domain.org.leave.enums.HolidayType
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("공휴일마스터")
@Entity
@Table(name = "holiday_master")
@SFObject("HolidayMaster__c")
class HolidayMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("공휴일마스터ID")
    @Column(name = "holiday_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("HolidayDate__c")
    @FieldName("휴일일자")
    @Column(name = "holiday_date", nullable = false, unique = true)
    var holidayDate: LocalDate,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", nullable = false, length = 80)
    var name: String,

    @SFField("Type__c")
    @Convert(converter = HolidayTypeConverter::class)
    @FieldName("휴일구분")
    @Column(name = "type", nullable = false, length = 255)
    var type: HolidayType,

    @FieldName("연도")
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
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // V199 — SF OwnerId.referenceTo = [Group, User] polymorphic. owner_id (Employee FK) →
    // owner_user_id (User FK) + owner_group_id (Group FK) + XOR CHECK.
    // V200 — SF CreatedById/LastModifiedById.referenceTo = [User]. audit FK Employee → User 전환.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null

) : BaseEntity() {
    fun update(holidayDate: LocalDate, name: String, type: HolidayType) {
        this.holidayDate = holidayDate
        this.name = name
        this.type = type
        this.year = holidayDate.year
    }
}
