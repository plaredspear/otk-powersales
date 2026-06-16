package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PromotionRepository : JpaRepository<Promotion, Long>, PromotionRepositoryCustom {

    /**
     * promotion_number 채번.
     *
     * promotion_number 는 SF AutoNumber(Name) 와 동일한 번호 공간(PM + 8자리)을 공유한다.
     * SF 데이터 sync 가 신규 시스템 시퀀스보다 큰 번호를 적재하면 nextval 만으로는 unique 위반이 발생한다.
     * 또한 시퀀스 동기화를 특정 시점에 한 번만 하면(Flyway setval 등) SF 데이터 마이그레이션과의 실행 순서에 의존해 다시 뒤처질 수 있다.
     *
     * 이를 시점 의존 없이 해소하기 위해, 채번 때마다 nextval 과 "현재 데이터 최대 번호 + 1" 중 큰 값을
     * setval 로 확정한다. setval 반환값이 곧 발급 번호이며, 항상 기존 데이터 최대값을 추월하므로 충돌이 발생하지 않는다.
     * setval 이 시퀀스 내부값을 즉시 갱신하므로, 한 번 따라잡은 뒤에는 일반 시퀀스처럼 동작한다(MAX 스캔은 항상 작은 값).
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.promotion_number_seq',
                GREATEST(
                    nextval('powersales.promotion_number_seq'),
                    COALESCE(
                        (SELECT MAX(NULLIF(regexp_replace(promotion_number, '\D', '', 'g'), '')::bigint)
                           FROM powersales.promotion
                          WHERE promotion_number ~ '^PM[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun getNextPromotionNumberSeq(): Long
}
