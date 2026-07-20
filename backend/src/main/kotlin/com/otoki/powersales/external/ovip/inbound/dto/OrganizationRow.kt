package com.otoki.powersales.external.ovip.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.org.organization.repository.OrganizationSnapshotRow
import java.time.LocalDateTime

/**
 * 조직(Organization) 외부 조회용 평면 row — **entity 전 컬럼 노출**.
 *
 * 거래처([AccountRow]) 와 동일 규약 — "해당 엔티티의 필드를 모두 조회" 요구에 따라 Organization entity 의
 * 매핑 컬럼 전량을 노출한다. 조직은 SAP HR 조직 마스터를 2~5레벨 트리로 비정규화해 담고 있어
 * (cc_cd/org_cd/org_nm × Level2~5), 레벨 조합 전체가 있어야 수신 측에서 트리를 재구성할 수 있다.
 *
 * 관계(ownerUser / ownerGroup / createdBy / lastModifiedBy)는 **객체를 펼치지 않고** FK id 와 함께 entity 가
 * 이미 보유한 `*_sfid` 컬럼으로만 노출한다. LAZY 관계를 직렬화하면 row 마다 추가 쿼리(N+1)가 발생하고,
 * `User` 를 통째로 내보내면 조회 목적과 무관한 계정 정보까지 노출되기 때문이다.
 * FK id 는 entity 의 관계 필드가 아니라 [OrganizationSnapshotRow] 가 쿼리에서 함께 가져온 값을 쓴다.
 *
 * **[id] 를 외부 시스템의 영속 키로 쓰면 안 된다** — 조직 테이블은 SAP 동기화 때 전체 삭제 후 재삽입되어
 * PK 가 매 동기화마다 바뀐다. 수신 측이 조직을 식별할 때는 [externalKey] 나 코스트센터/HR 조직 코드
 * ([costCenterLevel5] 등) 를 쓴다. 본 필드는 응답 내 정렬 기준 이상의 의미가 없다.
 */
data class OrganizationRow(
    /**
     * PK — 동기화마다 재발번되므로 외부 영속 키로 사용 금지 (클래스 KDoc 참조).
     */
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("sfid")
    val sfid: String?,

    @JsonProperty("name")
    val name: String?,

    @JsonProperty("costCenterLevel2")
    val costCenterLevel2: String?,

    @JsonProperty("orgCodeLevel2")
    val orgCodeLevel2: String?,

    @JsonProperty("orgNameLevel2")
    val orgNameLevel2: String?,

    @JsonProperty("costCenterLevel3")
    val costCenterLevel3: String?,

    @JsonProperty("orgCodeLevel3")
    val orgCodeLevel3: String?,

    @JsonProperty("orgNameLevel3")
    val orgNameLevel3: String?,

    @JsonProperty("costCenterLevel4")
    val costCenterLevel4: String?,

    @JsonProperty("orgCodeLevel4")
    val orgCodeLevel4: String?,

    @JsonProperty("orgNameLevel4")
    val orgNameLevel4: String?,

    @JsonProperty("costCenterLevel5")
    val costCenterLevel5: String?,

    @JsonProperty("orgCodeLevel5")
    val orgCodeLevel5: String?,

    @JsonProperty("orgNameLevel5")
    val orgNameLevel5: String?,

    @JsonProperty("externalKey")
    val externalKey: String?,

    @JsonProperty("isDeleted")
    val isDeleted: Boolean?,

    @JsonProperty("ownerSfid")
    val ownerSfid: String?,

    @JsonProperty("ownerUserId")
    val ownerUserId: Long?,

    @JsonProperty("ownerGroupId")
    val ownerGroupId: Long?,

    @JsonProperty("createdBySfid")
    val createdBySfid: String?,

    @JsonProperty("createdById")
    val createdById: Long?,

    @JsonProperty("lastModifiedBySfid")
    val lastModifiedBySfid: String?,

    @JsonProperty("lastModifiedById")
    val lastModifiedById: Long?,

    @JsonProperty("createdAt")
    val createdAt: LocalDateTime?,

    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(snapshot: OrganizationSnapshotRow): OrganizationRow = with(snapshot.organization) {
            OrganizationRow(
                id = id,
                sfid = sfid,
                name = name,
                costCenterLevel2 = costCenterLevel2,
                orgCodeLevel2 = orgCodeLevel2,
                orgNameLevel2 = orgNameLevel2,
                costCenterLevel3 = costCenterLevel3,
                orgCodeLevel3 = orgCodeLevel3,
                orgNameLevel3 = orgNameLevel3,
                costCenterLevel4 = costCenterLevel4,
                orgCodeLevel4 = orgCodeLevel4,
                orgNameLevel4 = orgNameLevel4,
                costCenterLevel5 = costCenterLevel5,
                orgCodeLevel5 = orgCodeLevel5,
                orgNameLevel5 = orgNameLevel5,
                externalKey = externalKey,
                isDeleted = isDeleted,
                ownerSfid = ownerSfid,
                ownerUserId = snapshot.ownerUserId,
                ownerGroupId = snapshot.ownerGroupId,
                createdBySfid = createdBySfid,
                createdById = snapshot.createdById,
                lastModifiedBySfid = lastModifiedBySfid,
                lastModifiedById = snapshot.lastModifiedById,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }
}
