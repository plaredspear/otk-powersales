package com.otoki.powersales.schedule.service

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * 여사원일정(TeamMemberSchedule) 생성 시 소유자(OwnerId)를 "대상 직원의 소속 조장 User" 로 결정.
 *
 * 레거시 SF `TeamMemberScheduleTriggerHandler.insertOwner()` 동등:
 *   "모바일에서 조원일정 생성 시, 소유자를 조장으로 맵핑" — teamleadersfid → Employee → User → OwnerId.
 * 신규는 레코드별 teamleadersfid 대신 대상 직원의 costCenterCode 로 활성 LEADER 사원을 찾아
 * 그 사원의 employeeCode == User.employeeCode 인 User 를 owner 로 지정한다 (AdminScheduleService 의
 * DisplayWorkSchedule owner chain 과 동일 패턴).
 *
 * owner 미해소(조장 부재 / costCenterCode 없음) 시 null 반환 — 호출처는 owner 미지정으로 두고,
 * [OwnerUserDefaultListener] 가 @PrePersist 에서 생성자(현재 로그인 User)로 채우는 기존 동작을 따른다.
 */
@Component
class TeamMemberScheduleOwnerResolver(
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
) {
    /**
     * 대상 직원들의 costCenterCode → 조장 User 매핑을 일괄 해소.
     * @return key = costCenterCode, value = 해당 지점의 조장 User (해소 실패한 costCenterCode 는 미포함)
     */
    fun resolveOwnersByCostCenterCode(employees: Collection<Employee>): Map<String, User> {
        val costCenterCodes = employees.mapNotNull { it.costCenterCode }.distinct()
        if (costCenterCodes.isEmpty()) return emptyMap()

        val leadersByCostCenter = employeeRepository
            .findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(costCenterCodes, AppAuthority.LEADER)
            .groupBy { it.costCenterCode }

        val leaderEmployeeCodes = leadersByCostCenter.values
            .flatten()
            .map { it.employeeCode }
            .distinct()
        if (leaderEmployeeCodes.isEmpty()) return emptyMap()

        val userByEmployeeCode = userRepository.findByEmployeeCodeIn(leaderEmployeeCodes)
            .associateBy { it.employeeCode }

        return costCenterCodes.mapNotNull { costCenterCode ->
            val owner = leadersByCostCenter[costCenterCode]
                ?.firstOrNull()
                ?.employeeCode
                ?.let { userByEmployeeCode[it] }
            owner?.let { costCenterCode to it }
        }.toMap()
    }

    /**
     * 단건 — 대상 직원의 소속 조장 User. 해소 실패 시 null.
     */
    fun resolveOwner(employee: Employee): User? =
        employee.costCenterCode
            ?.let { resolveOwnersByCostCenterCode(listOf(employee))[it] }
}
