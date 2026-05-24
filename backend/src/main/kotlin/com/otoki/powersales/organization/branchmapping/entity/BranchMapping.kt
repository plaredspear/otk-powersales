package com.otoki.powersales.organization.branchmapping.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * cost_center_code 이력 합집합 매핑.
 *
 * SF `BranchMapping__mdt` 의 backend 이전. SoT 는 [com.otoki.powersales.organization.branchmapping.BranchMappingMatrix] —
 * 부팅 시 [com.otoki.powersales.organization.branchmapping.BranchMappingSyncRunner] 가 INSERT-only sync (DB-only row 보존).
 *
 * 운영 의미: Organization 코드 변경 시 (예: `5479` → `5849`) 과거 데이터의 cost_center_code 시점 스냅샷 보존,
 * 조회 시 [com.otoki.powersales.organization.branchmapping.BranchCodeExpander] 가 현행 코드 → 이력+현행 합집합 확장.
 */
@Entity
@Table(name = "branch_mapping")
class BranchMapping(
    @Id
    @Column(name = "branch_code", length = 20, nullable = false)
    val branchCode: String,

    @Column(name = "included_branch_codes", length = 255, nullable = false)
    var includedBranchCodes: String,

    @Column(name = "label", length = 100)
    var label: String? = null,
) : BaseEntity()
