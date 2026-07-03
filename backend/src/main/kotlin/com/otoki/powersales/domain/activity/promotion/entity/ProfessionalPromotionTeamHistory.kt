package com.otoki.powersales.domain.activity.promotion.entity

import com.otoki.powersales.domain.activity.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("전문행사조 이력")
@Entity
@Table(name = "professional_promotion_team_history")
@SFObject("ProfessionalPromotionTeamHistory__c")
class ProfessionalPromotionTeamHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("전문행사조이력ID")
    @Column(name = "professional_promotion_team_history_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    // 변경을 유발한 전문행사조 마스터 FK. 생성/수정/확정/sync/만료 경로는 채우고,
    // 삭제로 인한 해제 경로는 마스터가 이미 제거되므로 null (DB FK ON DELETE SET NULL 로 보호).
    @FieldName("전문행사조마스터ID")
    @Column(name = "professional_promotion_team_master_id")
    val masterId: Long? = null,

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("oldValue__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @FieldName("변경 전")
    @Column(name = "old_value", length = 255)
    val oldValue: ProfessionalPromotionTeamType? = null,

    @SFField("newValue__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @FieldName("변경 후")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "new_value", length = 255)
    val newValue: ProfessionalPromotionTeamType? = null,

    // new_value 컬럼의 raw 문자열 read-only 매핑 — 컨버터를 거치지 않은 원본 값.
    // SF 원본에서 newValue__c 는 Text(255) 라 '일반'(미지정 해제) 등 enum 5종 밖의 문자열이
    // 마이그레이션분에 실재한다. 컨버터가 걸린 [newValue] 로는 read 시 null 로 접히고 QueryDSL
    // 문자열 비교도 불가하므로, "일반" 이력 검색을 위해 raw 컬럼을 직접 비교할 때만 쓴다 (쓰기 금지).
    @Column(name = "new_value", length = 255, insertable = false, updatable = false)
    val newValueRaw: String? = null,

    @SFField("updateTime__c")
    @FieldName("변경 시점")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "changed_at")
    val changedAt: LocalDateTime? = LocalDateTime.now(),

    // -- Group A R-2: Owner / CreatedBy / LastModifiedBy --
    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,
) : BaseEntity()
