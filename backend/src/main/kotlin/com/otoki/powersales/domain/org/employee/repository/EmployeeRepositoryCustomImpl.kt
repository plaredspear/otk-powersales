package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.org.employee.entity.QEmployeeInfo.Companion.employeeInfo
import com.querydsl.core.BooleanBuilder
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
        workTypeMatchedEmployeeIds: Set<Long>?,
        promotionTeam: ProfessionalPromotionTeamType?,
        promotionTeamGeneral: Boolean,
        pageable: Pageable
    ): Page<Employee> {
        // 근무형태 필터가 걸렸으나 매칭 사원이 0명이면 빈 결과 — employee.id IN (empty) 의 DB/QueryDSL
        // 렌더링에 의존하지 않고 명시적으로 빈 페이지를 반환한다(프로젝트 빈 컬렉션 IN 방어 패턴 정합).
        if (workTypeMatchedEmployeeIds != null && workTypeMatchedEmployeeIds.isEmpty()) {
            return PageableExecutionUtils.getPage(emptyList(), pageable) { 0L }
        }

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
        // 전문행사조 필터 — '일반'(미배정) 은 IS NULL 뿐 아니라, SF 레거시가 정규화 없이 적재한
        // '일반'·'해당없음' 문자열 행도 함께 조회한다 (화면 목록이 이 값들을 '일반'으로 표시하는 것과 정합).
        // converter 컬럼이라 enum path 로는 문자열과 직접 비교할 수 없어, stringValue() 로 원본 문자열과 비교.
        if (promotionTeamGeneral) {
            where.and(
                employee.professionalPromotionTeam.isNull
                    .or(employee.professionalPromotionTeam.stringValue().`in`(ProfessionalPromotionTeamType.UNASSIGNED_LEGACY_VALUES))
            )
        } else if (promotionTeam != null) {
            where.and(employee.professionalPromotionTeam.eq(promotionTeam))
        }
        // 근무형태1/3 필터 — 서비스 레이어가 '최근 출근등록 1건이 조건과 일치하는 사원' 집합을 미리 산출해 전달.
        // 상관 서브쿼리(구: latestAttendanceWorkTypePredicate) 를 employee.id IN (...) 로 대체해 전건 조회 timeout 을 제거.
        // null = 필터 미적용, 빈 집합 = 일치 사원 0명(빈 결과) — 두 의미를 구분한다.
        if (workTypeMatchedEmployeeIds != null) {
            where.and(employee.id.`in`(workTypeMatchedEmployeeIds))
        }

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
