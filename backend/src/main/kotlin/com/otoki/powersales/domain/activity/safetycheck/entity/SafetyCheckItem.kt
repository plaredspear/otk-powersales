package com.otoki.powersales.domain.activity.safetycheck.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@DomainName("안전점검항목")
@Entity
@Table(name = "safety_check_item")
@HerokuOnly("safetycheck_list")
class SafetyCheckItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("안전점검항목ID")
    @Column(name = "safety_check_item_id")
    val id: Long = 0,

    @FieldName("질문번호")
    @Column(name = "question_num", nullable = false)
    @HCColumn("question_num")
    val questionNum: Int = 0,

    @FieldName("순번")
    @Column(name = "seq_num", nullable = false)
    @HCColumn("seq_num")
    val seqNum: Int = 0,

    @FieldName("상세내용")
    @Column(name = "contents", nullable = false, length = 500)
    @HCColumn("contents")
    val contents: String = "",

    @FieldName("사용여부")
    @Column(name = "use_yn", length = 1)
    @HCColumn("use_yn")
    val useYn: String? = "Y"

) : BaseEntity()
