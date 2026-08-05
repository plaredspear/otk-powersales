package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.admin.exception.SapOriginEmployeeNotEditableException
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeAppLoginActiveUpdateRequest
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeRoleUpdateRequest
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeUpdateRequest
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.policy.EmployeeLockingPolicy
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
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
 * - 사용자 액션의 부수 효과 중 잠금 축은 [EmployeeLockingPolicy] 가 적용한다 (SAP 인입 경로
 *   [EmployeeUpsertService] 와 동일 정책 공유 — SF 는 전역 트리거라 경로 무관 동일 규칙).
 *   전문행사조 허용값 검증은 서비스 자체 호출로 보장. 이력 자동 생성, 미래 일정 자동 삭제,
 *   사용자 부서 동기 등 chain 1-hop 부수 효과는 P1 단계에서 보강.
 *
 * ## 동등성 매핑
 * - 레거시 EmployeeTriggerHandler 의 before/after update 자동 처리 중 P0 범위:
 *   - 전문행사조 허용값 검증 — [applyProfessionalPromotionTeam] 의 enum 변환이 겸한다
 *     (허용값 밖 문자열은 예외. '일반' 은 미배정 복귀 신호라 별도 처리)
 *   - 잠금 ON → 앱 로그인 자동 비활성화 + 현장 여사원 직군 보호 복원 — [EmployeeLockingPolicy]
 * - **[updateEmployeeRole] 은 잠금 정책 대상이 아니다** — SF 는 트리거가 전역이라 권한만 바꿔도
 *   보호 규칙이 함께 발화하지만, 신규의 해당 API 는 origin=SAP 사원도 허용하는 role 전용 경로라
 *   잠금/앱로그인까지 건드리면 "role 외 필드 무변경 = SAP SoT 불가침" 전제가 깨진다. 의도적 이탈.
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
            throw SapOriginEmployeeNotEditableException(employee.employeeCode ?: "")
        }

        applyMutableFields(employee, request)
        // before update Trigger 동등 — 잠금 ↔ 앱 로그인 축 (SAP 인입 경로와 동일 정책 공유)
        EmployeeLockingPolicy.applyBeforeSave(employee)

        val saved = employeeRepository.save(employee)
        syncUserCache(saved)
        logger.info("EMPLOYEE_UPDATED id={} code={} role={}", saved.id, saved.employeeCode, saved.role)
        return EmployeeDetailResponse.from(saved)
    }

    /**
     * 사원 권한(role) 전용 수정.
     *
     * 일반 수정([update]) 과 달리 origin=SAP 사원도 허용한다 — 권한 필드는 SAP 인입
     * ([EmployeeUpsertService.applyMutableFields]) 이 갱신하지 않는 컬럼이라 SAP 인입과
     * 경합하지 않기 때문이다. AccountViewAll 처럼 SAP 발령으로 산출되지 않는 권한을 부여하는
     * 유일한 경로다. role 외 다른 필드는 건드리지 않으므로 SAP SoT 를 침해하지 않는다.
     */
    @Transactional
    fun updateEmployeeRole(employeeId: Long, request: AdminEmployeeRoleUpdateRequest): EmployeeDetailResponse {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException(employeeId)

        val previousRole = employee.role
        employee.role = request.role

        val saved = employeeRepository.save(employee)
        logger.info(
            "EMPLOYEE_ROLE_UPDATED id={} code={} previousRole={} newRole={}",
            saved.id, saved.employeeCode, previousRole, saved.role,
        )
        return EmployeeDetailResponse.from(saved)
    }

    /**
     * 사원 앱 로그인 활성(appLoginActive) 전용 수정 — origin 게이트 없음.
     *
     * ## 분리 이유
     * 일반 수정([update]) 은 origin=SAP 사원을 차단하는데, 운영 사원은 전량 origin=SAP 다
     * (컬럼 DEFAULT `'SAP'` + SF 마이그레이션이 origin 을 매핑하지 않음). 그래서 웹 관리자에서
     * 앱 로그인을 수동으로 켤 수단이 아예 없었고, `appLoginActive=false` 인 사원은 비밀번호/단말
     * 초기화 버튼마저 잠겨 구제 경로가 없었다 ([AdminEmployeeCredentialService] 의 활성 게이트).
     * role 전용 경로([updateEmployeeRole]) 와 같은 단일 축 API 로 분리해 이 사각지대를 연다.
     *
     * ## 레거시 동등 — lockingFlag 는 건드리지 않는다
     * SF 는 사원 레코드 상세 레이아웃(`DKRetail__Employee__c-직원_조장.layout-meta.xml`, "현장사원 설정"
     * 섹션) 에서 `DKRetail__APPLoginActive__c` 를 `behavior=Edit`, `LockingFlag__c` 를
     * `behavior=Readonly` 로 두어 **잠금 플래그는 SAP 전용(관리자 읽기전용), 앱 로그인 활성만 수동
     * 토글**하게 했다. 본 경로도 동일하게 appLoginActive 한 축만 쓴다.
     *
     * 이 수동 토글이 필요한 이유도 레거시에 있다 — SF 의 SAP 인입(`IF_REST_SAP_EmployeeMaster.cls:128-131`)
     * 은 `LockingFlag='Y'` 일 때 `APPLoginActive=false` 만 쓰고 잠금 해제 시 true 로 복원하지 않는다.
     * 그렇게 꺼진 채 넘어온 사원(= SF 마이그레이션 적재분) 을 되살리는 유일한 수단이 수동 토글이다.
     *
     * ## 요청값이 그대로 저장되지 않을 수 있다
     * [EmployeeLockingPolicy] 를 저장 직전에 그대로 적용한다 (경로 무관 동일 규칙 — SF 전역 트리거
     * 정합). 두 방향 모두 정책이 되돌릴 수 있다:
     * - 활성화 요청 + `lockingFlag=true` → 정책 1번 규칙이 false 로 되돌림 (SF `EmployeeTriggerHandler.cls:40`
     *   동등 — 레거시에서도 잠긴 사원은 체크해도 활성화되지 않는다. SAP 잠금 해제가 선행돼야 한다.)
     * - 비활성화 요청 + 현장 여사원 직군(판촉/레이디/OSC) + 재직 + 여사원/조장 → 정책 2번 규칙이
     *   활성으로 강제 복원 (SF `lockingFlagException` 동등)
     *
     * 따라서 응답의 `appLoginActive` 를 호출자가 확인해야 한다 (컨트롤러가 사유별 메시지로 구분).
     *
     * ## 지속성 한계
     * appLoginActive 는 SAP 인입이 갱신하는 컬럼이라 다음 인사 인입 시 SAP 의 LockingFlag 기준으로
     * 덮어써진다([EmployeeUpsertService]). 본 경로는 SoT 변경이 아니라 인입 사이의 수동 구제 수단이다.
     */
    @Transactional
    fun updateAppLoginActive(
        employeeId: Long,
        request: AdminEmployeeAppLoginActiveUpdateRequest,
    ): EmployeeDetailResponse {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException(employeeId)

        val previous = employee.appLoginActive
        val requested = request.appLoginActive == true

        // SF 레이아웃 정합 — 관리자가 만지는 축은 appLoginActive 하나뿐 (lockingFlag 는 SAP 전용)
        employee.appLoginActive = requested
        // before update Trigger 동등 — 잠금 ↔ 앱 로그인 축 (현장 여사원 보호 규칙 포함)
        EmployeeLockingPolicy.applyBeforeSave(employee)

        val saved = employeeRepository.save(employee)
        logger.info(
            "EMPLOYEE_APP_LOGIN_ACTIVE_UPDATED id={} code={} previous={} requested={} effective={}",
            saved.id, saved.employeeCode, previous, requested, saved.appLoginActive,
        )
        return EmployeeDetailResponse.from(saved)
    }

    /**
     * Employee 의 derived 캐시 컬럼을 매칭 User 행에 반영. 매칭 user 부재 시 silent skip.
     *
     * 현 시점 캐시 대상: `cost_center_code`. profile_id / is_sales_support 는 SAP 발령
     * 후처리(AppointmentUserProfileUpdater) 에서 별도 갱신하므로 본 경로에서 동기화하지 않는다.
     */
    private fun syncUserCache(employee: Employee) {
        val user = employee.employeeCode?.let { userRepository.findByEmployeeCode(it) } ?: return
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
        applyProfessionalPromotionTeam(entity, request.professionalPromotionTeam)
        request.crmWorkType?.let { entity.crmWorkType = it }
        request.crmWorkStartDate?.let { entity.crmWorkStartDate = it }
        request.totalAnnualLeave?.let { entity.totalAnnualLeave = it }
        request.usedAnnualLeave?.let { entity.usedAnnualLeave = it }
        // phone 은 mirroring trigger 가 채우므로 직접 할당하지 않는다 (homePhone 우선)
    }

    /**
     * 전문행사조 갱신 — partial update 규칙의 유일한 예외 (명시적 해제 지원).
     *
     * 요청 필드는 `String?` 이라 세 가지 입력을 구분한다:
     * - `null` / 공백 — 필드 미전송. 다른 필드와 동일하게 **값 변경 없음**.
     * - `'일반'` ([ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME]) — 명시적 미배정 복귀.
     *   신규 시스템의 미배정은 null 이므로 `null` 로 저장한다 (레거시 잔존 문자열 '일반'/'해당없음' 을
     *   새로 쓰지 않는다).
     * - 정식 5개 조 표시명 — 해당 enum 으로 갱신. 표시명이 바뀐 유형(카레세일조 ← 카레행사조)의
     *   이전 문자열도 `legacyAliases` 로 매핑된다.
     *
     * 폼이 내려받는 옵션 출처는 `GET /api/v1/admin/female-employees/form-meta`
     * ([AdminEmployeeService.getFemaleEmployeeFormMeta]) 이며, 그 6개 값이 여기서 허용되는 입력과 일치한다.
     * 레거시 EmployeeTriggerHandler 의 전문행사조 허용값 검증 동등 — 허용값 밖 문자열은 예외로 거른다.
     */
    private fun applyProfessionalPromotionTeam(entity: Employee, requested: String?) {
        val value = requested?.takeIf { it.isNotBlank() } ?: return
        if (value == ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME) {
            entity.professionalPromotionTeam = null
            return
        }
        entity.professionalPromotionTeam = ProfessionalPromotionTeamType.fromDisplayNameOrNull(value)
            ?: throw IllegalArgumentException("유효하지 않은 전문행사조 유형: $value")
    }

}
