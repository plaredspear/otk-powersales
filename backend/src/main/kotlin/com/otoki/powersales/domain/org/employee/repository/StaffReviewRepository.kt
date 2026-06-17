package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.entity.StaffReview
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/**
 * 사원평가 Repository (SF `StaffReview__c`).
 *
 * SF → RDS 적재 대상 entity 의 JPA Repository. SF fetch sync 의 upsert 매칭(sfid IN) 에 사용한다.
 */
interface StaffReviewRepository : JpaRepository<StaffReview, Long> {

    /** SF Id(sfid) 일괄 조회 — SF fetch sync 의 upsert 매칭 키. */
    fun findBySfidIn(sfids: Collection<String>): List<StaffReview>

    /**
     * 사원(employee FK) + 평가대상월 1일(`FirstDayofMonth__c`) 평가 행 조회 — 여사원 평가조회 지점평가 source.
     *
     * `FirstDayofMonth__c` 는 SF `CreateReviewRecordBatch` 가 set 하는 **평가 대상월(전월)의 1일** 이라
     * (예: 5월 평가 → `2024-05-01`), 조회월 M 에 대해 `firstDayOfMonth = M-01` 로 직접 등호 매칭한다.
     * 레거시 `selectBranchEval` 의 `createddate YYYYMM = (M+1)` 우회는 "5월 평가가 6월 생성" 구조에서
     * 나온 것으로, 신규는 평가 대상월 자체를 담은 `firstDayOfMonth` 로 정합한다.
     * soft-delete(`isDeleted == true`) 필터는 호출 측에서 수행 (`isDeleted` nullable).
     */
    fun findByEmployeeIdAndFirstDayOfMonth(employeeId: Long, firstDayOfMonth: LocalDate): List<StaffReview>
}
