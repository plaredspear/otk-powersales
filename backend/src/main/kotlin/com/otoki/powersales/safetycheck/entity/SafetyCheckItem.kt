package com.otoki.powersales.safetycheck.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*

@Entity
@Table(name = "safety_check_item")
@HerokuOnly("safetycheck_list")
class SafetyCheckItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "safety_check_item_id")
    val id: Long = 0,

    @Column(name = "question_num", nullable = false)
    @HCColumn("question_num")
    val questionNum: Int = 0,

    @Column(name = "seq_num", nullable = false)
    @HCColumn("seq_num")
    val seqNum: Int = 0,

    @Column(name = "contents", nullable = false, length = 500)
    @HCColumn("contents")
    val contents: String = "",

    @Column(name = "use_yn", length = 1)
    @HCColumn("use_yn")
    val useYn: String? = "Y"

) : BaseEntity()
