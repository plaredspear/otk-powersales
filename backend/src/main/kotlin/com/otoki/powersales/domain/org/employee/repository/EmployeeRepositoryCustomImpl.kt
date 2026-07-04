package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.schedule.entity.QTeamMemberSchedule
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.org.employee.entity.QEmployeeInfo.Companion.employeeInfo
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class EmployeeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : EmployeeRepositoryCustom {

    override fun findWithEmployeeInfoByEmployeeCode(employeeCode: String): Employee? {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(employee.employeeCode.eq(employeeCode))
            .fetchOne()
    }

    override fun findWithEmployeeInfoById(id: Long): Employee? {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(employee.id.eq(id))
            .fetchOne()
    }

    override fun findWithEmployeeInfoByStatus(status: String): List<Employee> {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(employee.status.eq(status))
            .fetch()
    }

    override fun findWithEmployeeInfoByCostCenterCodeInAndStatus(
        costCenterCodes: List<String>,
        status: String
    ): List<Employee> {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(
                employee.costCenterCode.`in`(costCenterCodes),
                employee.status.eq(status)
            )
            .fetch()
    }

    override fun findWithEmployeeInfoByCostCenterCodeAndRole(
        costCenterCode: String,
        role: String
    ): List<Employee> {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(
                employee.costCenterCode.eq(costCenterCode),
                employee.role.eq(role)
            )
            .fetch()
    }

    override fun findActiveWomenByCostCenterCodes(costCenterCodes: List<String>?): List<Employee> {
        val builder = BooleanBuilder()
        builder.and(employee.role.eq(AppAuthority.WOMAN))
        builder.and(employee.appLoginActive.isTrue)
        builder.and(employee.isDeleted.isNull.or(employee.isDeleted.isFalse))
        if (!costCenterCodes.isNullOrEmpty()) {
            builder.and(employee.costCenterCode.`in`(costCenterCodes))
        }
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(builder)
            .orderBy(employee.name.asc())
            .fetch()
    }

    override fun findWomenByCostCenterCodes(costCenterCodes: List<String>?): List<Employee> {
        // findActiveWomenByCostCenterCodes 와 동일하되 appLoginActive 조건 제외 — 퇴사/휴직 여사원 포함.
        val builder = BooleanBuilder()
        builder.and(employee.role.eq(AppAuthority.WOMAN))
        builder.and(employee.isDeleted.isNull.or(employee.isDeleted.isFalse))
        if (!costCenterCodes.isNullOrEmpty()) {
            builder.and(employee.costCenterCode.`in`(costCenterCodes))
        }
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(builder)
            .orderBy(employee.name.asc())
            .fetch()
    }

    override fun findAllEmployeeCodes(): List<String> {
        return queryFactory
            .select(employee.employeeCode)
            .from(employee)
            .fetch()
    }

    override fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        role: String?,
        roles: List<String>?,
        workType1: WorkingCategory1?,
        workType3: WorkingCategory3?,
        promotionTeam: ProfessionalPromotionTeamType?,
        promotionTeamGeneral: Boolean,
        pageable: Pageable
    ): Page<Employee> {
        val where = BooleanBuilder()
            .and(employee.isDeleted.isNull.or(employee.isDeleted.isFalse))

        if (status != null) {
            where.and(employee.status.eq(status))
        }
        if (branchCodes != null) {
            where.and(employee.costCenterCode.`in`(branchCodes))
        }
        if (!keyword.isNullOrBlank()) {
            where.and(
                employee.employeeCode.containsIgnoreCase(keyword)
                    .or(employee.name.containsIgnoreCase(keyword))
            )
        }
        if (role != null) {
            where.and(employee.role.eq(role))
        }
        if (!roles.isNullOrEmpty()) {
            where.and(employee.role.`in`(roles))
        }
        // 전문행사조 필터 — '일반'(미배정=null) 은 IS NULL, 특정 조는 등호.
        if (promotionTeamGeneral) {
            where.and(employee.professionalPromotionTeam.isNull)
        } else if (promotionTeam != null) {
            where.and(employee.professionalPromotionTeam.eq(promotionTeam))
        }
        // 근무형태1/3 필터 — 사원의 '가장 최근 출근등록 1건'(attendance_log 연결) 기준.
        // 출근등록 이력 없는 사원은 EXISTS 불충족으로 자동 제외 (사용자 결정: 미등록 제외).
        latestAttendanceWorkTypePredicate(workType1, workType3)?.let { where.and(it) }

        val content = queryFactory
            .selectFrom(employee)
            .where(where)
            .orderBy(employee.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(employee.count())
            .from(employee)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    /**
     * 근무형태(1/3) 필터 술어 — 사원의 '가장 최근 출근등록 1건' 이 선택한 근무형태와 일치하는지.
     *
     * '가장 최근 출근등록 1건' 판정은 [TeamMemberScheduleRepositoryCustomImpl.findLatestAttendanceInfoByEmployeeIds]
     * 와 동일하게 정의한다: `attendance_log_id IS NOT NULL` 인 행 중 (working_date DESC, id DESC) 최상위 1건.
     * "이 사원의 최근 출근등록 행이 존재하고 그 category1/3 이 조건과 일치" 를 EXISTS 로 표현하며,
     * "최근 1건" 은 '더 최근인(또는 같은 날 더 큰 id) 다른 출근등록 행이 없다' 는 NOT EXISTS 로 고정한다.
     *
     * @return 필터 조건 술어. workType1/workType3 모두 null 이면 null (미적용).
     */
    private fun latestAttendanceWorkTypePredicate(
        workType1: WorkingCategory1?,
        workType3: WorkingCategory3?,
    ): Predicate? {
        if (workType1 == null && workType3 == null) return null

        // 후보 = 이 사원의 출근등록 행 중, category 조건을 만족하는 것.
        val candidate = QTeamMemberSchedule("tmsWorkTypeCandidate")
        // 비교군 = 같은 사원의 다른 출근등록 행 (더 최근 여부 판정용).
        val newer = QTeamMemberSchedule("tmsWorkTypeNewer")

        val candidateWhere = BooleanBuilder()
            .and(candidate.employee.id.eq(employee.id))
            .and(candidate.attendanceLog.id.isNotNull)
        workType1?.let { candidateWhere.and(candidate.workingCategory1.eq(it)) }
        workType3?.let { candidateWhere.and(candidate.workingCategory3.eq(it)) }

        // NOT EXISTS: 같은 사원의 출근등록 행 중 candidate 보다 '더 최근'(날짜 큼, 또는 같은 날 id 큼)인 행.
        // 이게 없으면 candidate 가 곧 최근 1건 → 선택 조건이 최근 1건에 적용됨을 보장.
        val newerExists = JPAExpressions.selectOne()
            .from(newer)
            .where(
                newer.employee.id.eq(candidate.employee.id),
                newer.attendanceLog.id.isNotNull,
                newer.workingDate.gt(candidate.workingDate)
                    .or(
                        newer.workingDate.eq(candidate.workingDate)
                            .and(newer.id.gt(candidate.id))
                    ),
            )
            .exists()

        return JPAExpressions.selectOne()
            .from(candidate)
            .where(candidateWhere.and(newerExists.not()))
            .exists()
    }

    override fun resetAgreementFlagForActiveConsents(): Long {
        return queryFactory
            .update(employee)
            .set(employee.agreementFlag, false)
            .where(employee.agreementFlag.isTrue)
            .execute()
    }

    override fun findByCostCenterCodeInAndEmployeeCodeIn(
        costCenterCodes: Collection<String>,
        employeeCodes: Collection<String>
    ): List<Employee> {
        if (costCenterCodes.isEmpty() || employeeCodes.isEmpty()) return emptyList()
        return queryFactory
            .selectFrom(employee)
            .where(
                employee.costCenterCode.`in`(costCenterCodes),
                employee.employeeCode.`in`(employeeCodes)
            )
            .fetch()
    }
}
