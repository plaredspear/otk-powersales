package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PromotionProductRepository : JpaRepository<PromotionProduct, Long>, PromotionProductRepositoryCustom {

    fun findByPromotionId(promotionId: Long): PromotionProduct?


    /**
     * promotion_product.name 채번 — SF AutoNumber `PS{00000000}` 동등 (V208 신규 sequence).
     * 시퀀스 nextval 단독.
     *
     * MAX 보정은 [syncNameSeq] 가 담당하며, 번호가 외부에서 주입될 수 있는 시점
     * (부팅 1회 / SF 마이그레이션 직후)에만 실행한다 — `NameSequenceSyncService`.
     * 상시 운영 중에는 앱 밖에서 name 을 넣는 경로가 없으므로 채번마다 MAX 를 재확인하지 않는다.
     * Native query 라 hibernate.default_schema 가 적용되지 않으므로 schema prefix 명시.
     */
    @Query(
        value = "SELECT nextval('powersales.promotion_product_name_seq')",
        nativeQuery = true
    )
    fun getNextNameSeq(): Long

    /**
     * name 시퀀스를 기존 데이터 최대 suffix 위로 끌어올린다 (멱등).
     * name 에 UNIQUE 제약이 없어(SF 정합) 뒤처진 채 채번하면 조용한 중복이 되므로,
     * SF 마이그레이션 적재 직후 반드시 본 보정이 돌아야 한다.
     * MAX 대상 표현식에는 부분 인덱스(`idx_promotion_product_name_seq_num`)가 있다.
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.promotion_product_name_seq',
                GREATEST(
                    nextval('powersales.promotion_product_name_seq'),
                    COALESCE(
                        (SELECT MAX(SUBSTRING(name FROM 3)::bigint)
                           FROM powersales.promotion_product
                          WHERE name ~ '^PS[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun syncNameSeq(): Long
}
