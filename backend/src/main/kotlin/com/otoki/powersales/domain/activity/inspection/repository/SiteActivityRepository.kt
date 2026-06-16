package com.otoki.powersales.domain.activity.inspection.repository

import com.otoki.powersales.domain.activity.inspection.entity.SiteActivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SiteActivityRepository :
    JpaRepository<SiteActivity, Long>,
    SiteActivityRepositoryCustom {

    fun findByIdAndIsDeletedFalse(id: Long): SiteActivity?

    /**
     * site_activity.name 채번 — SF Name AutoNumber(`SA{00000000}`) 동등.
     *
     * name 은 SF AutoNumber 와 동일한 번호 공간(SA + 8자리)을 공유한다. SF 데이터 sync 가
     * 시퀀스보다 큰 suffix 를 적재하면 nextval 만으로는 기존 row 와 충돌한다. 마이그레이션 setval
     * 및 부팅 SyncRunner 동기화는 시점 의존이라 부팅 이후 SF sync 유입에는 다시 뒤처질 수 있다.
     *
     * 이를 시점 의존 없이 해소하기 위해, 채번 때마다 nextval 과 "현재 name 최대 suffix + 1" 중
     * 큰 값을 setval 로 확정한다. setval 반환값이 곧 채번값이며 항상 기존 데이터를 추월한다.
     * setval 이 시퀀스 내부값을 즉시 갱신하므로 한 번 따라잡은 뒤에는 일반 시퀀스처럼 동작한다.
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
    fun getNextNameSeq(): Long
}
