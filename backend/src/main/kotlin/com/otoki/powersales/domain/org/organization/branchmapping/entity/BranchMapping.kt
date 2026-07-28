package com.otoki.powersales.domain.org.organization.branchmapping.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 지점 코드 확장 매핑 — "이 지점을 조회할 때 함께 포함할 코드 전부".
 *
 * SF `BranchMapping__mdt` (Custom Metadata Type) 의 backend 이전. 적재 경로 = **SF 데이터 마이그레이션
 * Stage1 CSV** — `customMetadata/BranchMapping.*.md-meta.xml` 74개를 `extract-sharing-meta.main.kts` 가
 * `branch-mapping.csv` 로 추출 → Stage1 `BranchMapping` target (`Stage1Targets`) 으로 COPY 적재.
 * SharingRule / SObjectSetting 등 다른 XML 메타와 동일 경로 (코드 박제 + 부팅 sync 미사용).
 *
 * (V203 마이그레이션 주석은 구 방식 "BranchMappingMatrix Kotlin object + 부팅 ApplicationRunner sync" 기준
 *  — Flyway checksum 보호로 수정 불가하여 stale. 현행 권위 출처는 본 주석.)
 *
 * ## ⚠️ [includedBranchCodes] 는 "이력 코드" 만이 아니다
 * 이 필드를 조직 개편 이력(구 코드) 보존으로만 이해하면 **롤업 행에서 의도치 않은 타 조직 데이터가
 * 유입**된다. SF 운영 레코드 74건 실측 결과 성격이 4종으로 섞여 있다:
 *
 * | 성격 | 의미 | 실측 예 |
 * |---|---|---|
 * | 이력 | 자기 자신 + 조직 개편으로 폐기된 옛 코드 (다수파) | 강북1지점 `5815` → `5452,5815` |
 * | 롤업 | **현행 타 조직**을 함께 끌어옴 (부서→하위 지점, 팀→사업부 전체) | retail3영업부 `5829` → `5826,5827,5828` (인천1·2·3) |
 * | 이중코드 | `E{N}` ↔ `{N}` 동일 조직 별칭, 양쪽 다 현역 | E-BIZ1팀 `E5706` → `5706,E5706` |
 * | 확장 없음 | 자기 자신만 (74건 중 3건) | 울산2지점 `5767` → `5767` |
 *
 * 확장 결과에 **다른 레코드의 [branchCode] 가 섞인 행 6건**이 롤업에 해당한다 —
 * `retail3영업부`(5829) / `cvs전략`계열 `CVS1팀`(E5692) · `CVS2팀`(E5693) / `대구경북급식지점`(5898,
 * 대구4지점 5763 흡수) / `KAM1영업부`(E5721) · `KAM1부장`(5721). 지점 스코프 가드에 적용할 때는
 * 이 6건을 개별 검토해야 한다.
 *
 * 데이터 품질도 균질하지 않다 — `KAM1부장`(5721) 의 값은 `"5721,E5721, 5466, 5693,5721,5466"` 로
 * 공백·중복이 그대로 남아 있고(운영자 수기 입력), 라벨도 지점/팀/부서/직책이 뒤섞여 있다.
 * 실제 확장 결과는 `시스템 > 지점 코드 맵핑` 화면에서 조직명과 함께 확인할 수 있다
 * ([com.otoki.powersales.domain.org.organization.branchmapping.service.AdminBranchMappingService]).
 *
 * ## 코드 도메인
 * [branchCode] / [includedBranchCodes] 는 명명(`cost_center_code`)과 달리 실제 값이 **OrgCode** 다
 * ([com.otoki.powersales.domain.org.organization.service.OrgCostCenterMatchService] KDoc 참조).
 * 조직명 해석 시 `cc_cd*` 가 아니라 `org_cd*` 컬럼으로 매칭해야 한다.
 *
 * 조회 시 [com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander] 가
 * 입력 코드 → 위 합집합으로 확장한다.
 */
@DomainName("지점매핑")
@Entity
@Table(name = "branch_mapping")
class BranchMapping(
    @Id
    @FieldName("지점코드")
    @Column(name = "branch_code", length = 20, nullable = false)
    val branchCode: String,

    @FieldName("포함지점코드목록")
    @Column(name = "included_branch_codes", length = 255, nullable = false)
    var includedBranchCodes: String,

    @FieldName("라벨")
    @Column(name = "label", length = 100)
    var label: String? = null,
) : BaseEntity()
