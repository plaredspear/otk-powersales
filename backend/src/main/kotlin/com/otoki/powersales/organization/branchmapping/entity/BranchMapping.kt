package com.otoki.powersales.organization.branchmapping.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * cost_center_code 이력 합집합 매핑.
 *
 * SF `BranchMapping__mdt` (Custom Metadata Type) 의 backend 이전. 적재 경로 = **SF 데이터 마이그레이션
 * Stage1 CSV** — `customMetadata/BranchMapping.*.md-meta.xml` 74개를 `extract-sharing-meta.main.kts` 가
 * `branch-mapping.csv` 로 추출 → Stage1 `BranchMapping` target (`Stage1Targets`) 으로 COPY 적재.
 * SharingRule / SObjectSetting 등 다른 XML 메타와 동일 경로 (코드 박제 + 부팅 sync 미사용).
 *
 * (V203 마이그레이션 주석은 구 방식 "BranchMappingMatrix Kotlin object + 부팅 ApplicationRunner sync" 기준
 *  — Flyway checksum 보호로 수정 불가하여 stale. 현행 권위 출처는 본 주석.)
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
