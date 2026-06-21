package com.otoki.powersales.domain.activity.draft.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.platform.common.entity.DomainName
@DomainName("임시저장 제안")
@Entity
@Table(name = "tmp_suggest")
@HerokuOnly("tmp_suggest")
class TmpSuggest(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tmp_suggest_id")
    val id: Long = 0,

    @HCColumn("tmp_category")
    @Column(name = "category", length = 80)
    var tmpCategory: String? = null,

    @HCColumn("tmp_productname")
    @Column(name = "product_name", length = 80)
    var tmpProductName: String? = null,

    @HCColumn("tmp_productcode")
    @Column(name = "product_code", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_title")
    @Column(name = "title", length = 100)
    var tmpTitle: String? = null,

    @HCColumn("tmp_description")
    @Column(name = "description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @HCColumn("tmp_employeecode")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_s3imageurl1")
    @Column(name = "s3_image_url1", length = 80)
    var tmpS3ImageUrl1: String? = null,

    @HCColumn("tmp_s3imageurl2")
    @Column(name = "s3_image_url2", length = 80)
    var tmpS3ImageUrl2: String? = null,

    @HCColumn("tmp_s3imagefilename1")
    @Column(name = "s3_image_file_name1", length = 80)
    var tmpS3ImageFileName1: String? = null,

    @HCColumn("tmp_s3imagefilename2")
    @Column(name = "s3_image_file_name2", length = 80)
    var tmpS3ImageFileName2: String? = null,

    @HCColumn("tmp_s3imagefilesize1")
    @Column(name = "s3_image_file_size1", length = 80)
    var tmpS3ImageFileSize1: String? = null,

    @HCColumn("tmp_s3imagefilesize2")
    @Column(name = "s3_image_file_size2", length = 80)
    var tmpS3ImageFileSize2: String? = null,

    @HCColumn("tmp_carnumber")
    @Column(name = "car_number", columnDefinition = "TEXT")
    var tmpCarNumber: String? = null,

    @HCColumn("tmp_accountcode")
    @Column(name = "account_code", columnDefinition = "TEXT")
    var tmpAccountCode: String? = null,

    @HCColumn("tmp_claimlist")
    @Column(name = "claim_list", columnDefinition = "TEXT")
    var tmpClaimList: String? = null,

    @HCColumn("tmp_claimdate")
    @Column(name = "claim_date")
    var tmpClaimDate: LocalDate? = null,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @Column(name = "product_id")
    var productId: Long? = null,

    @Column(name = "account_id")
    var accountId: Long? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()