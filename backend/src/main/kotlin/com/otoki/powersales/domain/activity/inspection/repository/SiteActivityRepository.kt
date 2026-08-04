package com.otoki.powersales.domain.activity.inspection.repository

import com.otoki.powersales.domain.activity.inspection.entity.SiteActivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SiteActivityRepository :
    JpaRepository<SiteActivity, Long>,
    SiteActivityRepositoryCustom {

    fun findByIdAndIsDeletedFalse(id: Long): SiteActivity?

    /**
     * site_activity.name 채번 — SF Name AutoNumber(`SA{00000000}`) 동등. 시퀀스 nextval 단독.
     *
     * MAX 보정은 [syncNameSeq] 가 담당하며, 번호가 외부에서 주입될 수 있는 시점
     * (부팅 1회 / SF 마이그레이션 직후)에만 실행한다 — `NameSequenceSyncService`.
     */
    @Query(
        value = "SELECT nextval('powersales.site_activity_name_seq')",
        nativeQuery = true
    )
    fun getNextNameSeq(): Long

    /**
     * name 시퀀스를 기존 데이터 최대 suffix 위로 끌어올린다 (멱등).
     * MAX 대상 표현식에는 부분 인덱스(`idx_site_activity_name_seq_num`)가 있다.
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.site_activity_name_seq',
                GREATEST(
                    nextval('powersales.site_activity_name_seq'),
                    COALESCE(
                        (SELECT MAX(SUBSTRING(name FROM 3)::bigint)
                           FROM powersales.site_activity
                          WHERE name ~ '^SA[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun syncNameSeq(): Long
}
