package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "professional_promotion_team_history")
@SFObject("ProfessionalPromotionTeamHistory__c")
class ProfessionalPromotionTeamHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "professional_promotion_team_history_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("oldValue__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "old_value", length = 50)
    val oldValue: ProfessionalPromotionTeamType? = null,

    @SFField("newValue__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "new_value", nullable = false, length = 50)
    val newValue: ProfessionalPromotionTeamType,

    @SFField("updateTime__c")
    @Column(name = "changed_at", nullable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(),

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,
)
