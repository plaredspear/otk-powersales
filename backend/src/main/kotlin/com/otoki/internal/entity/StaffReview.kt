package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 직원 평가 Entity
 * V1 스키마: staffreview__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "staffreview__c")
class StaffReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "dkretail_employeeid__c", length = 18)
    val employeeId: String? = null,

    @Column(name = "employeename__c", length = 1300)
    val employeeName: String? = null,

    @Column(name = "employeenumber__c", length = 1300)
    val employeeNumber: String? = null,

    @Column(name = "branch__c", length = 1300)
    val branch: String? = null,

    @Column(name = "branchreviews__c", length = 18)
    val branchReviews: String? = null,

    @Column(name = "costcentercode__c", length = 1300)
    val costCenterCode: String? = null,

    @Column(name = "employeetotalscore__c")
    val employeeTotalScore: Double? = null,

    @Column(name = "attendance__c")
    val attendance: Double? = null,

    @Column(name = "instructionsdefault__c")
    val instructionsDefault: Double? = null,

    @Column(name = "priority_eventitemmanage__c")
    val priorityEventItemManage: Double? = null,

    @Column(name = "displaymanageeventgoals__c")
    val displayManageEventGoals: Double? = null,

    @Column(name = "businesspartnerties__c")
    val businessPartnerTies: Double? = null,

    @Column(name = "clothessatellite__c")
    val clothesSatellite: Double? = null,

    @Column(name = "productmanagecallment__c")
    val productManageCallment: Double? = null,

    @Column(name = "educationalevaluation__c")
    val educationalEvaluation: Double? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null
)
