package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PromotionEmployeeRepository : JpaRepository<PromotionEmployee, Long>, PromotionEmployeeRepositoryCustom {

    fun deleteByPromotionId(promotionId: Long)

    /**
     * promotion_employee.name 채번 (SF AutoNumber "행사사원#" 동등, "PE" + 8자리).
     *
     * name 은 SF AutoNumber(Name) 와 동일한 번호 공간(PE + 8자리)을 공유한다.
     * SF 데이터 sync 가 신규 시스템 시퀀스보다 큰 번호를 적재하면 nextval 만으로는 unique 위반이 발생한다.
     * 시점 의존 없이 해소하기 위해 채번 때마다 nextval 과 "현재 데이터 최대 번호 + 1" 중 큰 값을
     * setval 로 확정한다. setval 반환값이 곧 발급 번호이며, 항상 기존 데이터 최대값을 추월해 충돌이 없다.
     * (Promotion.getNextPromotionNumberSeq 와 동일 패턴)
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
    fun getNextPromotionEmployeeNumberSeq(): Long
}
