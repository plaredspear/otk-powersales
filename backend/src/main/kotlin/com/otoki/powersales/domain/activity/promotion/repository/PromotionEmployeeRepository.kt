package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PromotionEmployeeRepository : JpaRepository<PromotionEmployee, Long>, PromotionEmployeeRepositoryCustom {

    fun deleteByPromotionId(promotionId: Long)

    /**
     * promotion_employee.name 채번 (SF AutoNumber "행사사원#" 동등, "PE" + 8자리). 시퀀스 nextval 단독.
     *
     * MAX 보정은 [syncNameSeq] 가 담당하며 번호가 외부에서 주입될 수 있는 시점
     * (부팅 1회 / SF 마이그레이션 직후)에만 실행한다 — `NameSequenceSyncService`.
     * (Promotion.getNextPromotionNumberSeq 와 동일 패턴)
     */
    @Query(
        value = "SELECT nextval('powersales.promotion_employee_number_seq')",
        nativeQuery = true
    )
    fun getNextPromotionEmployeeNumberSeq(): Long

    /**
     * name 시퀀스를 기존 데이터 최대 번호 위로 끌어올린다 (멱등).
     * MAX 대상 표현식에는 부분 인덱스(`idx_promotion_employee_name_seq_num`)가 있다.
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.promotion_employee_number_seq',
                GREATEST(
                    nextval('powersales.promotion_employee_number_seq'),
                    COALESCE(
                        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
                           FROM powersales.promotion_employee
                          WHERE name ~ '^PE[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun syncNameSeq(): Long
}
