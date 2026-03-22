package com.otoki.internal.promotion.entity

import com.otoki.internal.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "professional_promotion_team_master")
class ProfessionalPromotionTeamMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "professional_promotion_team_master_id")
    val id: Long = 0,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @Column(name = "account_id", nullable = false)
    val accountId: Int,

    @Column(name = "team_type", nullable = false, length = 50)
    var teamType: String,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "is_confirmed", nullable = false)
    var isConfirmed: Boolean = false,

    @Column(name = "branch_code", length = 20)
    var branchCode: String? = null,

    @Column(name = "branch_name", length = 50)
    var branchName: String? = null

) : BaseEntity() {

    fun update(
        teamType: String,
        startDate: LocalDate,
        endDate: LocalDate?,
        isConfirmed: Boolean,
        accountId: Int? = null
    ) {
        this.teamType = teamType
        this.startDate = startDate
        this.endDate = endDate
        this.isConfirmed = isConfirmed
    }
}
