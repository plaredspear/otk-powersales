package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PromotionRepository : JpaRepository<Promotion, Long>, PromotionRepositoryCustom {

    /**
     * promotion_number 채번 (SF AutoNumber Name, "PM" + 8자리 재현). 시퀀스 nextval 단독.
     *
     * 시퀀스를 기존 데이터 최대 번호 위로 끌어올리는 MAX 보정은 [syncPromotionNumberSeq] 가 담당하며,
     * 번호가 외부에서 주입될 수 있는 시점(부팅 1회 / SF 마이그레이션 직후)에만 실행한다 —
     * `NameSequenceSyncService`. promotion_number 는 UNIQUE 제약이 있으므로 보정 누락 시 채번이
     * 실패(예외)하며 조용한 중복은 발생하지 않는다.
     */
    @Query(
        value = "SELECT nextval('powersales.promotion_number_seq')",
        nativeQuery = true
    )
    fun getNextPromotionNumberSeq(): Long

    /**
     * promotion_number 시퀀스를 기존 데이터 최대 번호 위로 끌어올린다 (멱등).
     * 이미 앞서 있으면 nextval 1개만 소모하고 값은 그대로다.
     * MAX 대상 표현식에는 부분 인덱스(`idx_promotion_number_seq_num`)가 있다.
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
    fun syncPromotionNumberSeq(): Long
}
