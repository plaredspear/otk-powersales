package com.otoki.powersales.savedsearch.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * 저장된 검색 (SavedSearch) — 목록 화면의 검색 조건 프리셋 (Spec #852).
 *
 * SF ListView 의 저장된 검색 개념을 신규 시스템에 도입한 범용 모듈. [resourceKey] 로 화면을 구분하여
 * 행사마스터 외 타 목록 화면에서도 재사용 가능하다.
 *
 * 신규 자체 데이터로 Salesforce 동기화 대상이 아니다 (HC 컬럼 / @SFObject 불필요). 단, `@Table(name)`
 * 기반으로 [EntitySfNameRegistry] 가 부팅 시 자원 키 `saved_search` 를 권한 카탈로그에 자동 등록한다.
 */
@Entity
@Table(name = "saved_search")
class SavedSearch(

    /** 화면 식별자 (예: "promotion"). 같은 화면의 검색끼리 묶는다. */
    @Column(name = "resource_key", nullable = false, length = 50)
    var resourceKey: String,

    /** 검색 표시명 (예: "관리자_검색용"). 드롭다운에 노출. */
    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    /** 공유 스코프 — PRIVATE(개인) / SHARED(공용). */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    var scope: SavedSearchScope,

    /** 생성자 employee.id. SHARED 도 생성자를 보존한다 (감사 목적). */
    @Column(name = "owner_id", nullable = false)
    var ownerId: Long,

    /**
     * 필터 조건 (불투명 JSON). 화면(resourceKey)마다 키 구성이 다르므로 백엔드는 내부 키를 해석하지 않고
     * Map 으로 저장/반환만 한다. 필터 키의 의미 해석은 프런트가 담당.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", nullable = false, columnDefinition = "jsonb")
    var filters: Map<String, Any?>,

    /** 드롭다운 표시 순서. 동일값이면 생성일 오름차순. */
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,
) : BaseEntity()
