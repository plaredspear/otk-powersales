package com.otoki.internal.inspection.entity

import com.otoki.internal.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 현장 점검 테마 Entity
 * V1 스키마: theme__c
 */
@Entity
@Table(name = "inspection_theme")
class InspectionTheme(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "title__c", length = 250)
    val title: String? = null,

    @Column(name = "startdate__c")
    val startDate: LocalDate? = null,

    @Column(name = "enddate__c")
    val endDate: LocalDate? = null,

    @Column(name = "department__c", length = 100)
    val department: String? = null,

    @Column(name = "branchcode__c", length = 30)
    val branchCode: String? = null,

    @Column(name = "publicflag__c")
    val publicFlag: Boolean? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

) : BaseEntity()
