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
import com.otoki.powersales.platform.common.entity.FieldName
@DomainName("임시저장 제안")
@Entity
@Table(name = "tmp_suggest")
@HerokuOnly("tmp_suggest")
class TmpSuggest(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("임시저장제안ID")
    @Column(name = "tmp_suggest_id")
    val id: Long = 0,

    @HCColumn("tmp_category")
    @FieldName("제안구분")
    @Column(name = "category", length = 80)
    var tmpCategory: String? = null,

    @HCColumn("tmp_productname")
    @FieldName("제품명")
    @Column(name = "product_name", length = 80)
    var tmpProductName: String? = null,

    @HCColumn("tmp_productcode")
    @FieldName("제품코드")
    @Column(name = "product_code", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_title")
    @FieldName("제목")
    @Column(name = "title", length = 100)
    var tmpTitle: String? = null,

    @HCColumn("tmp_description")
    @FieldName("행사대체제품")
    @Column(name = "description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @HCColumn("tmp_employeecode")
    @FieldName("사번")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_s3imageurl1")
    @FieldName("S3이미지URL1")
    @Column(name = "s3_image_url1", length = 80)
    var tmpS3ImageUrl1: String? = null,

    @HCColumn("tmp_s3imageurl2")
    @FieldName("S3이미지URL2")
    @Column(name = "s3_image_url2", length = 80)
    var tmpS3ImageUrl2: String? = null,

    @HCColumn("tmp_s3imagefilename1")
    @FieldName("S3이미지파일명1")
    @Column(name = "s3_image_file_name1", length = 80)
    var tmpS3ImageFileName1: String? = null,

    @HCColumn("tmp_s3imagefilename2")
    @FieldName("S3이미지파일명2")
    @Column(name = "s3_image_file_name2", length = 80)
    var tmpS3ImageFileName2: String? = null,

    @HCColumn("tmp_s3imagefilesize1")
    @FieldName("S3이미지파일크기1")
    @Column(name = "s3_image_file_size1", length = 80)
    var tmpS3ImageFileSize1: String? = null,

    @HCColumn("tmp_s3imagefilesize2")
    @FieldName("S3이미지파일크기2")
    @Column(name = "s3_image_file_size2", length = 80)
    var tmpS3ImageFileSize2: String? = null,

    @HCColumn("tmp_carnumber")
    @FieldName("물류 차량번호")
    @Column(name = "car_number", columnDefinition = "TEXT")
    var tmpCarNumber: String? = null,

    @HCColumn("tmp_accountcode")
    @FieldName("거래처유형코드")
    @Column(name = "account_code", columnDefinition = "TEXT")
    var tmpAccountCode: String? = null,

    @HCColumn("tmp_claimlist")
    @FieldName("클레임목록")
    @Column(name = "claim_list", columnDefinition = "TEXT")
    var tmpClaimList: String? = null,

    @HCColumn("tmp_claimdate")
    @FieldName("물류 클레임 발생일자")
    @Column(name = "claim_date")
    var tmpClaimDate: LocalDate? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @FieldName("제품ID")
    @Column(name = "product_id")
    var productId: Long? = null,

    @FieldName("거래처ID")
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