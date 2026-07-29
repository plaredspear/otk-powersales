package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.DismissalPolicy
import com.otoki.powersales.domain.org.employee.enums.EmploymentStatus
import com.otoki.powersales.domain.org.employee.enums.FemaleStaffHeadcountFilter
import com.otoki.powersales.domain.org.employee.enums.FemaleStaffJobCode
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.org.employee.entity.QEmployeeInfo.Companion.employeeInfo
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
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
            // 유예 발령 참조는 사원 상세 응답(PostponedAppointmentSummary)에 포함된다 — LAZY 미초기화로
            // DTO 가 null 이 되는 enhancement 함정을 피하기 위해 fetch join 으로 즉시 로드.
            .leftJoin(employee.postponedAppointment).fetchJoin()
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

    override fun findActiveWomenForPromotionByCostCenterCodes(costCenterCodes: List<String>?): List<Employee> {
        // 행사사원 후보 전용 — SF `RelatedListDataGridController.getLookupCandidates` 정합.
        // appLoginActive 대신 status='재직' 으로 걸어 확정 검증(status 휴직/퇴직 차단)과 축을 일치시킨다.
        val builder = BooleanBuilder()
        builder.and(employee.role.eq(AppAuthority.WOMAN))
        builder.and(employee.status.eq(EmploymentStatus.ACTIVE.code))
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

    /**
     * 인원현황 모수에서 테스트/시스템 계정을 배제하는 술어 —
     * 레거시 리포트 필터 `CUST_NAME notContain '테스트,관리자,파워세일즈'` 정합.
     *
     * 레거시 notContain 은 대소문자를 구분하지 않으므로 `containsIgnoreCase` 로 맞춘다.
     * `Employee.name` 은 non-null 컬럼이지만, NOT LIKE 가 NULL 에 대해 UNKNOWN 을 반환해 행을 통째로
     * 떨어뜨리는 것을 막기 위해 IS NULL 분기를 함께 둔다 (레거시 데이터 이관 중 공백 유입 방어).
     */
    private fun excludeTestAccountNames(): BooleanBuilder {
        val predicate = BooleanBuilder()
        FemaleStaffHeadcountFilter.EXCLUDED_NAME_KEYWORDS.forEach { keyword ->
            predicate.and(employee.name.isNull.or(employee.name.containsIgnoreCase(keyword).not()))
        }
        return predicate
    }

    /**
     * 재직상태 필터 술어 — SAP 원본 `status` 만 비교한다 (`status = ?`).
     *
     * 면직 보정 전면 적용([DismissalPolicy]) 이 화면 단위로 점진 전환 중이라 잠정 유지한다.
     * 미전환 호출부가 0이 되면 본 함수와 [findEmployees] 의 `treatDismissalAsResigned` 파라미터를
     * 함께 제거하고 [employmentStatusPredicate] 로 일원화한다.
     */
    private fun statusPredicate(status: String): BooleanBuilder =
        BooleanBuilder().and(employee.status.eq(status))

    /**
     * 재직상태 필터 술어 — 발령명 '면직' 을 퇴직과 동일 취급한다 ([DismissalPolicy]).
     *
     * - 퇴직 조회: `status = '퇴직' OR ord_detail_node = '면직'` (상태가 아직 재직인 면직자 포함)
     * - 재직/휴직 조회: `status = ? AND (ord_detail_node IS NULL OR ord_detail_node <> '면직')`
     *   — SQL 3값 논리상 `<>` 만 쓰면 NULL 행이 통째로 탈락하므로 IS NULL 을 함께 허용한다.
     *
     * 면직 보정 전면 적용이 전 화면의 목표 동작이며, 전환이 끝나면 [statusPredicate] 를 삭제하고
     * 본 함수만 남긴다.
     */
    private fun employmentStatusPredicate(status: String): BooleanBuilder {
        val dismissed = employee.ordDetailNode.eq(DismissalPolicy.ORD_DETAIL_NODE)
        return if (status == EmploymentStatus.RESIGNED.code) {
            BooleanBuilder().and(employee.status.eq(status).or(dismissed))
        } else {
            BooleanBuilder().and(employee.status.eq(status))
                .and(employee.ordDetailNode.isNull.or(dismissed.not()))
        }
    }

    override fun findDashboardBasicStatsProjection(
        branchCodes: List<String>?
    ): List<DashboardEmployeeProjection> {
        // 레거시 SF 홈 대시보드(조장) 인원현황 리포트(`reports/X00/new_report_72Y.report-meta.xml`) 의
        // 필터 4개를 그대로 재현한다 ([FemaleStaffHeadcountFilter] 참조):
        //   JobCode IN (판촉직,레이디직,OSC직) / Status <> 퇴직 / AppAuthority IN (조장,여사원)
        //   / 사원명에 테스트·관리자·파워세일즈 미포함
        // status=NULL 은 레거시 notEqual 의미(퇴직만 배제) 정합으로 포함하며, 재직/휴직 미분류로 계상된다.
        //
        // 여기에 레거시에 없는 조건 1개를 더한다(deviation) — 발령명 '면직' 사원 제외([DismissalPolicy]).
        // 면직 발령을 받고도 status 가 '재직' 으로 남은 사원이 재직 인원으로 부풀려지는 것을 막으며,
        // 여사원 현황 목록의 재직 조회와 동일한 판정이라 두 화면의 인원이 어긋나지 않는다.
        //
        // 지점 매칭은 **cost_center_code** 축 — 투입현황·매출현황·여사원 현황 목록과 동일하다.
        // 호출부가 BranchCodeExpander 로 조직 개편 이력 코드까지 확장해 넘긴다.
        // (레거시 리포트는 OrgName__c 그룹핑이었으나, 조직명 재배치/소멸분이 어긋나 코드 축으로 통일했다.)
        val where = BooleanBuilder()
            .and(employee.isDeleted.isNull.or(employee.isDeleted.isFalse))
            .and(employee.role.`in`(FemaleStaffHeadcountFilter.ROLES))
            .and(employee.jobCode.`in`(FemaleStaffJobCode.ALL_CODES))
            .and(employee.status.isNull.or(employee.status.ne(EmploymentStatus.RESIGNED.code)))
            // 면직 = 퇴직 취급. 발령명 NULL 행이 3값 논리로 탈락하지 않도록 IS NULL 을 함께 허용한다.
            .and(
                employee.ordDetailNode.isNull
                    .or(employee.ordDetailNode.ne(DismissalPolicy.ORD_DETAIL_NODE))
            )
            .and(excludeTestAccountNames())
        if (!branchCodes.isNullOrEmpty()) {
            where.and(employee.costCenterCode.`in`(branchCodes))
        }
        return queryFactory
            .select(
                Projections.constructor(
                    DashboardEmployeeProjectionDto::class.java,
                    employee.jobCode,
                    employee.status,
                    employee.birthDate,
                    employee.jikchak,
                    employee.jikwee,
                )
            )
            .from(employee)
            .where(where)
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
        promotionTeamAssignedOnly: Boolean,
        pageable: Pageable,
        jobCodes: Set<String>?,
        femaleStaffHeadcountScope: Boolean,
        treatDismissalAsResigned: Boolean,
    ): Page<Employee> {
        // 근무형태 필터가 걸렸으나 매칭 사원이 0명이면 빈 결과 — employee.id IN (empty) 의 DB/QueryDSL
        // 렌더링에 의존하지 않고 명시적으로 빈 페이지를 반환한다(프로젝트 빈 컬렉션 IN 방어 패턴 정합).
        if (workTypeMatchedEmployeeIds != null && workTypeMatchedEmployeeIds.isEmpty()) {
            return PageableExecutionUtils.getPage(emptyList(), pageable) { 0L }
        }

        val where = BooleanBuilder()
            .and(employee.isDeleted.isNull.or(employee.isDeleted.isFalse))

        if (status != null) {
            where.and(
                if (treatDismissalAsResigned) employmentStatusPredicate(status)
                else statusPredicate(status)
            )
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
        // 직무 필터(판촉직/OSC직) — 대시보드 인원현황 도넛과 동일한 jobCode 축.
        // OSC직 선택 시 구 명칭 '레이디직' 이 포함된 집합이 전달된다(서비스 레이어에서 확장).
        if (!jobCodes.isNullOrEmpty()) {
            where.and(employee.jobCode.`in`(jobCodes))
        }
        // 여사원 인원현황 모수 — 레거시 리포트(new_report_72Y) 정합. 여사원 현황 화면 전용이며,
        // 본 메소드를 공유하는 전체 사원 관리/lookup 화면은 이 조건 없이 기존 모수를 유지한다.
        if (femaleStaffHeadcountScope) {
            where.and(employee.jobCode.`in`(FemaleStaffJobCode.ALL_CODES))
            where.and(excludeTestAccountNames())
        }
        // 전문행사조 필터 — '일반'(미배정) 은 IS NULL 뿐 아니라, SF 레거시가 정규화 없이 적재한
        // '일반'·'해당없음' 문자열 행도 함께 조회한다 (화면 목록이 이 값들을 '일반'으로 표시하는 것과 정합).
        // converter 컬럼이라 enum path 로는 문자열과 직접 비교할 수 없어, stringValue() 로 원본 문자열과 비교.
        if (promotionTeamGeneral) {
            where.and(
                employee.professionalPromotionTeam.isNull
                    .or(employee.professionalPromotionTeam.stringValue().`in`(ProfessionalPromotionTeamType.UNASSIGNED_LEGACY_VALUES))
            )
        } else if (promotionTeamAssignedOnly) {
            // '행사조 전체' = 일반(미배정) 제외 — '일반' 필터(IS NULL OR 레거시 미배정 문자열) 의 정확한 여집합.
            where.and(
                employee.professionalPromotionTeam.isNotNull
                    .and(employee.professionalPromotionTeam.stringValue().notIn(ProfessionalPromotionTeamType.UNASSIGNED_LEGACY_VALUES))
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
