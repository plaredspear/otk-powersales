package com.otoki.powersales.domain.activity.suggestion.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 제안하기(물류클레임 포함) 등록 임시저장(draft) 엔티티.
 *
 * 사원이 제안을 작성하다가 임시저장한 입력값을 보관한다.
 * 레거시 `salesforce2.tmp_suggest` 대응. 정식 등록(제안 생성) 성공 시 이 draft 는 삭제된다.
 * 사원(employee) 1건당 draft 1건(unique). [com.otoki.powersales.domain.activity.claim.entity.ClaimDraft] 패턴 정합.
 *
 * SF 와 동기화되지 않는 로컬 전용 테이블이므로 SFObject 어노테이션을 두지 않는다.
 *
 * 임시저장은 검증을 건너뛰므로 분류/제목/내용 등 모든 입력 필드가 nullable 이다.
 * 거래처명/제품명은 prefill 표시용으로 저장 시점 값을 함께 보관한다(form-data 에 없는 정보).
 * 사진은 레거시 tmp_suggest 와 동일하게 최대 2장(S3 private key)을 보관한다.
 */
@DomainName("제안 임시저장")
@Entity
@Table(name = "suggestion_draft")
class SuggestionDraft(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("제안임시저장ID")
    @Column(name = "suggestion_draft_id")
    val id: Long = 0,

    @FieldName("사원ID")
    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @FieldName("제안구분")
    @Column(name = "category", length = 40)
    var category: String? = null,

    @FieldName("제목")
    @Column(name = "title", length = 255)
    var title: String? = null,

    @FieldName("제안내용")
    @Column(name = "content", length = 4000)
    var content: String? = null,

    @FieldName("제품코드")
    @Column(name = "product_code", length = 50)
    var productCode: String? = null,

    @FieldName("제품명")
    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    var accountId: Long? = null,

    @FieldName("거래처명")
    @Column(name = "account_name", length = 255)
    var accountName: String? = null,

    @FieldName("거래처코드")
    @Column(name = "sap_account_code", length = 100)
    var sapAccountCode: String? = null,

    @FieldName("클레임 항목")
    @Column(name = "claim_type", length = 200)
    var claimType: String? = null,

    @FieldName("물류 클레임 발생일자")
    @Column(name = "claim_date")
    var claimDate: LocalDate? = null,

    @FieldName("물류 차량번호")
    @Column(name = "car_number", length = 20)
    var carNumber: String? = null,

    @FieldName("물류책임")
    @Column(name = "logistics_responsibility", length = 20)
    var logisticsResponsibility: String? = null,

    @FieldName("중복 제안번호")
    @Column(name = "duplicate_proposal_num", length = 255)
    var duplicateProposalNum: String? = null,

    @FieldName("조치상태")
    @Column(name = "action_status", length = 40)
    var actionStatus: String? = null,

    @FieldName("사진키1")
    @Column(name = "photo_key1", length = 255)
    var photoKey1: String? = null,

    @FieldName("사진키2")
    @Column(name = "photo_key2", length = 255)
    var photoKey2: String? = null,

) : BaseEntity() {

    /** 입력값 일괄 반영 (사진 키는 별도 처리). */
    fun apply(
        category: String?,
        title: String?,
        content: String?,
        productCode: String?,
        productName: String?,
        accountId: Long?,
        accountName: String?,
        sapAccountCode: String?,
        claimType: String?,
        claimDate: LocalDate?,
        carNumber: String?,
        logisticsResponsibility: String?,
        duplicateProposalNum: String?,
        actionStatus: String?
    ) {
        this.category = category
        this.title = title
        this.content = content
        this.productCode = productCode
        this.productName = productName
        this.accountId = accountId
        this.accountName = accountName
        this.sapAccountCode = sapAccountCode
        this.claimType = claimType
        this.claimDate = claimDate
        this.carNumber = carNumber
        this.logisticsResponsibility = logisticsResponsibility
        this.duplicateProposalNum = duplicateProposalNum
        this.actionStatus = actionStatus
    }
}
