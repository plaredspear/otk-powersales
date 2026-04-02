package com.otoki.internal.promotion.entity

import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import com.otoki.internal.sap.entity.Employee
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

    @SFField("EmployeeId__c")
    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("oldValue__c")
    @Column(name = "old_value", length = 50)
    val oldValue: String? = null,

    @SFField("newValue__c")
    @Column(name = "new_value", nullable = false, length = 50)
    val newValue: String,

    @SFField("updateTime__c")
    @Column(name = "changed_at", nullable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(),

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,
)
