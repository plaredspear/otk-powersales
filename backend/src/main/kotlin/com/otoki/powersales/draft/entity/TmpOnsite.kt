package com.otoki.powersales.draft.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "tmp_onsite")
@HCTable("tmp_onsite")
class TmpOnsite(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tmp_onsite_id")
    val id: Long = 0,

    @HCColumn("tmp_employeecode")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_themecode")
    @Column(name = "theme_code", length = 80)
    var tmpThemeCode: String? = null,

    @HCColumn("tmp_themename")
    @Column(name = "theme_name", length = 80)
    var tmpThemeName: String? = null,

    @HCColumn("tmp_classification")
    @Column(name = "classification", length = 80)
    var tmpClassification: String? = null,

    @HCColumn("tmp_sapaccountname")
    @Column(name = "sap_account_name", length = 100)
    var tmpSapAccountName: String? = null,

    @HCColumn("tmp_sapaccoutncode")
    @Column(name = "sap_account_code", length = 80)
    var tmpSapAccountCode: String? = null,

    @HCColumn("tmp_category")
    @Column(name = "category", length = 100)
    var tmpCategory: String? = null,

    @HCColumn("tmp_activitydate")
    @Column(name = "activity_date", length = 80)
    var tmpActivityDate: String? = null,

    @HCColumn("tmp_description")
    @Column(name = "description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @HCColumn("tmp_productname")
    @Column(name = "product_name", length = 80)
    var tmpProductName: String? = null,

    @HCColumn("tmp_productcode")
    @Column(name = "product_code", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_competitorname")
    @Column(name = "competitor_name", length = 80)
    var tmpCompetitorName: String? = null,

    @HCColumn("tmp_competitoractivity")
    @Column(name = "competitor_activity", columnDefinition = "TEXT")
    var tmpCompetitorActivity: String? = null,

    @HCColumn("tmp_sampletastflag")
    @Column(name = "sample_tast_flag", length = 1)
    var tmpSampleTastFlag: String? = null,

    @HCColumn("tmp_competitorproudctname")
    @Column(name = "competitor_product_name", length = 80)
    var tmpCompetitorProductName: String? = null,

    @HCColumn("tmp_sampletasterprice")
    @Column(name = "sample_taster_price", length = 80)
    var tmpSampleTasterPrice: String? = null,

    @HCColumn("tmp_activityamount")
    @Column(name = "activity_amount", length = 40)
    var tmpActivityAmount: String? = null,

    @HCColumn("tmp_s3imagekey1")
    @Column(name = "s3_image_key1", length = 255)
    var tmpS3ImageKey1: String? = null,

    @HCColumn("tmp_s3imagekey2")
    @Column(name = "s3_image_key2", length = 255)
    var tmpS3ImageKey2: String? = null,

    @HCColumn("tmp_s3imagefilename1")
    @Column(name = "s3_image_file_name1", length = 80)
    var tmpS3ImageFileName1: String? = null,

    @HCColumn("tmp_s3imagefilesize1")
    @Column(name = "s3_image_file_size1", length = 80)
    var tmpS3ImageFileSize1: String? = null,

    @HCColumn("tmp_s3imagefilename2")
    @Column(name = "s3_image_file_name2", length = 80)
    var tmpS3ImageFileName2: String? = null,

    @HCColumn("tmp_s3imagefilesize2")
    @Column(name = "s3_image_file_size2", length = 80)
    var tmpS3ImageFileSize2: String? = null,

    @Column(name = "account_id")
    var accountId: Long? = null,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @Column(name = "product_id")
    var productId: Long? = null,

    @Column(name = "inspection_theme_id")
    var inspectionThemeId: Long? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
