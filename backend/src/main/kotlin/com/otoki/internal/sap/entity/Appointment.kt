package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "appointment",
    indexes = [
        Index(name = "idx_appointment_employee", columnList = "employee_code"),
        Index(name = "idx_appointment_date", columnList = "appoint_date")
    ]
)
class Appointment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "employee_code", nullable = false, length = 20)
    val employeeCode: String,

    @Column(name = "emp_code_exist", nullable = false)
    var empCodeExist: Boolean = false,

    @Column(name = "after_org_code", length = 20)
    val afterOrgCode: String? = null,

    @Column(name = "after_org_name", length = 100)
    val afterOrgName: String? = null,

    @Column(name = "jikchak", length = 50)
    val jikchak: String? = null,

    @Column(name = "jikwee", length = 50)
    val jikwee: String? = null,

    @Column(name = "jikgub", length = 20)
    val jikgub: String? = null,

    @Column(name = "work_type", length = 20)
    val workType: String? = null,

    @Column(name = "manage_type", length = 50)
    val manageType: String? = null,

    @Column(name = "job_code", length = 20)
    val jobCode: String? = null,

    @Column(name = "work_area", length = 50)
    val workArea: String? = null,

    @Column(name = "jikjong", length = 50)
    val jikjong: String? = null,

    @Column(name = "appoint_date", nullable = false, length = 8)
    val appointDate: String,

    @Column(name = "job_name", length = 100)
    val jobName: String? = null,

    @Column(name = "ord_detail_code", length = 20)
    val ordDetailCode: String? = null,

    @Column(name = "ord_detail_node", length = 100)
    val ordDetailNode: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
