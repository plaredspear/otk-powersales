package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeManualRegisterRequest
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 일반 사원 수동 등록 service (UC-06).
 *
 * ## 정책
 * - origin = MANUAL 로 고정 저장 → SAP 인입 갱신 대상에서 자동 제외 (Spec #579 동등).
 * - 시스템 관리자 등록은 본 endpoint 사용 불가 — 별도 ADMIN-prefix endpoint 사용. 본 endpoint 는 SF AppAuthority picklist 4종 (여사원/조장/지점장/AccountViewAll) 또는 null 만 허용.
 * - appLoginActive 는 등록 시점에는 false 로 시작 — 비밀번호 초기화 후 활성화하는 운영 절차.
 * - 본 service 는 비밀번호를 설정하지 않으므로 등록 후 `POST /{id}/reset-password` 로 임시 비밀번호 발급 필요.
 */
@Service
class AdminEmployeeManualRegisterService(
    private val employeeRepository: EmployeeRepository,
) {

    private val logger = LoggerFactory.getLogger(AdminEmployeeManualRegisterService::class.java)

    @Transactional
    fun register(request: AdminEmployeeManualRegisterRequest): EmployeeDetailResponse {
        // request.role 의 SF picklist 4종 검증은 @Pattern validation 에서 처리됨. 시스템 관리자 등록은 ADMIN-prefix endpoint 사용.

        if (employeeRepository.existsByEmployeeCode(request.employeeCode)) {
            throw EmployeeCodeDuplicatedException()
        }

        val employee = Employee(
            employeeCode = request.employeeCode,
            name = request.name,
            workEmail = request.workEmail,
            workPhone = request.workPhone,
            homePhone = request.homePhone,
            orgName = request.orgName,
            costCenterCode = request.costCenterCode,
            startDate = request.startDate,
        ).apply {
            role = request.role
            jobCode = request.jobCode
            jikwee = request.jikwee
            jikchak = request.jikchak
            jikgub = request.jikgub
            professionalPromotionTeam = request.professionalPromotionTeam
            origin = EmployeeOrigin.MANUAL
            appLoginActive = false
            // before insert Trigger 동등 — 전화번호 미러링 (P0 범위)
            if (!homePhone.isNullOrBlank() && phone.isNullOrBlank()) {
                phone = homePhone
            }
        }

        val saved = try {
            employeeRepository.save(employee)
        } catch (ex: DataIntegrityViolationException) {
            throw EmployeeCodeDuplicatedException()
        }

        logger.info("EMPLOYEE_MANUAL_REGISTERED id={} code={} role={}", saved.id, saved.employeeCode, saved.role)
        return EmployeeDetailResponse.from(saved)
    }
}
