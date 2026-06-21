package com.otoki.powersales.domain.activity.draft.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName
@DomainName("임시저장 현장점검")
@Entity
@Table(name = "tmp_onsite")
@HerokuOnly("tmp_onsite")
class TmpOnsite(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("임시저장현장점검ID")
    @Column(name = "tmp_onsite_id")
    val id: Long = 0,

    @HCColumn("tmp_employeecode")
    @FieldName("사번")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_themecode")
    @FieldName("테마코드")
    @Column(name = "theme_code", length = 80)
    var tmpThemeCode: String? = null,

    @HCColumn("tmp_themename")
    @FieldName("테마명")
    @Column(name = "theme_name", length = 80)
    var tmpThemeName: String? = null,

    @HCColumn("tmp_classification")
    @FieldName("구분")
    @Column(name = "classification", length = 80)
    var tmpClassification: String? = null,

    @HCColumn("tmp_sapaccountname")
    @FieldName("SAP거래처명")
    @Column(name = "sap_account_name", length = 100)
    var tmpSapAccountName: String? = null,

    @HCColumn("tmp_sapaccoutncode")
    @FieldName("거래처코드")
    @Column(name = "sap_account_code", length = 80)
    var tmpSapAccountCode: String? = null,

    @HCColumn("tmp_category")
    @FieldName("제안구분")
    @Column(name = "category", length = 100)
    var tmpCategory: String? = null,

    @HCColumn("tmp_activitydate")
    @FieldName("점검날짜")
    @Column(name = "activity_date", length = 80)
    var tmpActivityDate: String? = null,

    @HCColumn("tmp_description")
    @FieldName("행사대체제품")
    @Column(name = "description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @HCColumn("tmp_productname")
    @FieldName("제품명")
    @Column(name = "product_name", length = 80)
    var tmpProductName: String? = null,

    @HCColumn("tmp_productcode")
    @FieldName("제품코드")
    @Column(name = "product_code", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_competitorname")
    @FieldName("경쟁사명")
    @Column(name = "competitor_name", length = 80)
    var tmpCompetitorName: String? = null,

    @HCColumn("tmp_competitoractivity")
    @FieldName("경쟁사활동")
    @Column(name = "competitor_activity", columnDefinition = "TEXT")
    var tmpCompetitorActivity: String? = null,

    @HCColumn("tmp_sampletastflag")
    @FieldName("경쟁사 상품 시식여부")
    @Column(name = "sample_tast_flag", length = 1)
    var tmpSampleTastFlag: String? = null,

    @HCColumn("tmp_competitorproudctname")
    @FieldName("경쟁사 상품명")
    @Column(name = "competitor_product_name", length = 80)
    var tmpCompetitorProductName: String? = null,

    @HCColumn("tmp_sampletasterprice")
    @FieldName("시식단가")
    @Column(name = "sample_taster_price", length = 80)
    var tmpSampleTasterPrice: String? = null,

    @HCColumn("tmp_activityamount")
    @FieldName("활동금액")
    @Column(name = "activity_amount", length = 40)
    var tmpActivityAmount: String? = null,

    @HCColumn("tmp_s3imagekey1")
    @FieldName("이미지1S3키")
    @Column(name = "s3_image_key1", length = 255)
    var tmpS3ImageKey1: String? = null,

    @HCColumn("tmp_s3imagekey2")
    @FieldName("이미지2S3키")
    @Column(name = "s3_image_key2", length = 255)
    var tmpS3ImageKey2: String? = null,

    @HCColumn("tmp_s3imagefilename1")
    @FieldName("이미지1파일명")
    @Column(name = "s3_image_file_name1", length = 80)
    var tmpS3ImageFileName1: String? = null,

    @HCColumn("tmp_s3imagefilesize1")
    @FieldName("이미지1파일크기")
    @Column(name = "s3_image_file_size1", length = 80)
    var tmpS3ImageFileSize1: String? = null,

    @HCColumn("tmp_s3imagefilename2")
    @FieldName("이미지2파일명")
    @Column(name = "s3_image_file_name2", length = 80)
    var tmpS3ImageFileName2: String? = null,

    @HCColumn("tmp_s3imagefilesize2")
    @FieldName("이미지2파일크기")
    @Column(name = "s3_image_file_size2", length = 80)
    var tmpS3ImageFileSize2: String? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    var accountId: Long? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @FieldName("제품ID")
    @Column(name = "product_id")
    var productId: Long? = null,

    @FieldName("현장점검테마ID")
    @Column(name = "inspection_theme_id")
    var inspectionThemeId: Long? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()