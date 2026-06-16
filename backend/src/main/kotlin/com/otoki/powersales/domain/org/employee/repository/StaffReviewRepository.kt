package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.entity.StaffReview
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 사원평가 Repository (SF `StaffReview__c`).
 *
 * SF → RDS 적재 대상 entity 의 JPA Repository. SF fetch sync 의 upsert 매칭(sfid IN) 에 사용한다.
 */
interface StaffReviewRepository : JpaRepository<StaffReview, Long> {

    /** SF Id(sfid) 일괄 조회 — SF fetch sync 의 upsert 매칭 키. */
    fun findBySfidIn(sfids: Collection<String>): List<StaffReview>
}
