package com.otoki.powersales.auth.sharing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * UserRole 트리의 정적 스냅샷 (spec #782 P1-B).
 *
 * 본 UserRole 의 모든 하위 자손 user_role_id set 을 jsonb 박제. evaluator hot path 에서
 * 매 평가 시 트리 traverse 회피 — 정적 lookup 1회.
 *
 * 갱신은 P2-B 의 UserRoleHierarchyTraversal Service 가 담당.
 *
 * jsonb 컬럼은 application layer 에서 Jackson 으로 List<Long> ↔ JSON 직렬화 (의존성 추가 회피).
 */
@Entity
@Table(name = "user_role_hierarchy_snapshot")
class UserRoleHierarchySnapshot(

    @Id
    @Column(name = "user_role_id")
    val userRoleId: Long,

    // PG = jsonb (V175), H2 = JSON-as-text. Hibernate 6+ dialect 자동 매핑 (SapOutbox.payload / ScheduledJobRun.metadata 와 동일 패턴).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "all_subordinate_ids", nullable = false)
    var allSubordinateIds: String,

    @Column(name = "depth", nullable = false)
    var depth: Int,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancestor_path")
    var ancestorPath: String? = null,

    @Column(name = "snapshot_at", nullable = false)
    var snapshotAt: LocalDateTime = LocalDateTime.now(),
)
