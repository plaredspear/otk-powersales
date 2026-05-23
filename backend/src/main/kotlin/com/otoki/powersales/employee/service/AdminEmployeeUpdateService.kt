package com.otoki.powersales.employee.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.admin.exception.SapOriginEmployeeNotEditableException
import com.otoki.powersales.employee.dto.request.AdminEmployeeUpdateRequest
import com.otoki.powersales.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사원 정보 수정 service (UC-07).
 *
 * ## 정책
 * - SAP 가 원천인 사원 (origin=SAP) 은 web admin 에서 수정 금지 — 차단 + 명시적 예외 (SAP 인입과 경합 회피).
 * - MANUAL 사원만 수정 가능. 신규 등록 흐름 (UC-06) 으로 등록된 사원은 origin=MANUAL 로 저장됨.
 * - 사용자 액션의 부수 효과는 [EmployeeTriggerEffects] 가 일괄 적용 — 전화번호 미러링, 잠금 자동 제어,
 *   전문행사조 허용값 검증은 신규 시스템 P0 단계에서는 서비스 자체 호출로 보장. 이력 자동 생성, 미래 일정
 *   자동 삭제, 사용자 부서 동기 등 chain 1-hop 부수 효과는 P1 단계에서 보강.
 *
 * ## 동등성 매핑
 * - 레거시 EmployeeTriggerHandler 의 before/after update 자동 처리 중 P0 범위:
 *   - 전문행사조 허용값 검증 — applyProfessionalPromotionTeam 호출에서 enum 변환만으로 검증됨
 *     (DTO 의 ProfessionalPromotionTeamType 이 fromDisplayName 검증)
 *   - 잠금 ON → 앱 로그인 자동 비활성화 — `applyLockingFlag` 가 적용
 * - 미구현 영역은 매핑 문서 [docs/plan/legacy-pages/기본 여사원 현황/IMPLEMENTATION_MAPPING.md] P1·P2 참조.
 */
@Service
class AdminEmployeeUpdateService(
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(AdminEmployeeUpdateService::class.java)

    @Transactional
    fun update(employeeId: Long, request: AdminEmployeeUpdateRequest): EmployeeDetailResponse {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException(employeeId)

        if (employee.origin == EmployeeOrigin.SAP) {
            throw SapOriginEmployeeNotEditableException(employee.employeeCode)
        }

        applyMutableFields(employee, request)
        // before update Trigger 동등 — 전화번호 미러링 + 잠금 자동 제어 (P0 범위)
        applyTriggerEffects(employee)

        val saved = employeeRepository.save(employee)
        syncUserCache(saved)
        logger.info("EMPLOYEE_UPDATED id={} code={} role={}", saved.id, saved.employeeCode, saved.role)
        return EmployeeDetailResponse.from(saved)
    }

    /**
     * Employee 의 derived 캐시 컬럼을 매칭 User 행에 반영. 매칭 user 부재 시 silent skip.
     *
     * 현 시점 캐시 대상: `cost_center_code`. profile_id / is_sales_support 는 SAP 발령
     * 후처리(AppointmentUserProfileUpdater) 에서 별도 갱신하므로 본 경로에서 동기화하지 않는다.
     */
    private fun syncUserCache(employee: Employee) {
        val user = userRepository.findByEmployeeCode(employee.employeeCode) ?: return
        user.costCenterCode = employee.costCenterCode
    }

    private fun applyMutableFields(entity: Employee, request: AdminEmployeeUpdateRequest) {
        request.status?.let { entity.status = it }
        request.role?.let { entity.role = it }
        request.orgName?.let { entity.orgName = it }
        request.costCenterCode?.let { entity.costCenterCode = it }
        request.workArea?.let { entity.workArea = it }
        request.locationCode?.let { entity.locationCode = it }
        request.jobCode?.let { entity.jobCode = it }
        request.jikjong?.let { entity.jikjong = it }
        request.jikwee?.let { entity.jikwee = it }
        request.jikchak?.let { entity.jikchak = it }
        request.jikgub?.let { entity.jikgub = it }
        request.workType?.let { entity.workType = it }
        request.ordDetailNode?.let { entity.ordDetailNode = it }
        request.appointmentDate?.let { entity.appointmentDate = it }
        request.startDate?.let { entity.startDate = it }
        request.endDate?.let { entity.endDate = it }
        request.homePhone?.let { entity.homePhone = it }
        request.workPhone?.let { entity.workPhone = it }
        request.officePhone?.let { entity.officePhone = it }
        request.workEmail?.let { entity.workEmail = it }
        request.email?.let { entity.email = it }
        request.appLoginActive?.let { entity.appLoginActive = it }
        request.lockingFlag?.let { entity.lockingFlag = it }
        request.professionalPromotionTeam?.let { entity.professionalPromotionTeam = it }
        request.crmWorkType?.let { entity.crmWorkType = it }
        request.crmWorkStartDate?.let { entity.crmWorkStartDate = it }
        request.totalAnnualLeave?.let { entity.totalAnnualLeave = it }
        request.usedAnnualLeave?.let { entity.usedAnnualLeave = it }
        // phone 은 mirroring trigger 가 채우므로 직접 할당하지 않는다 (homePhone 우선)
    }

    /**
     * before insert/update Trigger 동등 부수 효과 (P0 범위).
     *
     * - 잠금 ON → 앱 로그인 자동 OFF (역방향 자동 활성화는 P1 보강 대상)
     */
    private fun applyTriggerEffects(entity: Employee) {
        if (entity.lockingFlag == true) {
            entity.appLoginActive = false
        }
    }
}
