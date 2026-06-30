package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.domain.org.employee.service.EmployeeUpsertService
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.external.sap.inbound.dto.employee.EmployeeMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.employee.EmployeeMasterRequestItem
import com.otoki.powersales.external.sap.inbound.dto.employee.FailureItem
import org.springframework.stereotype.Service

/**
 * SAP 직원 마스터 인바운드 어댑터. (Spec #557 / 어댑터-도메인 분리: #635 P2-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [EmployeeMasterRequestItem] → 도메인 커맨드 [EmployeeUpsertCommand] 매핑
 * - 도메인 서비스 [EmployeeUpsertService.upsert] 호출
 * - 도메인 결과 → SAP 응답 [EmployeeMasterDetail] 매핑
 * - `REQUEST_ACCEPTED` (success/failure 집계) audit — [SapInboundAuditAspect] 가 AOP 로 처리 (#639)
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 */
@Service
class SapEmployeeMasterService(
    private val employeeUpsertService: EmployeeUpsertService
) {

    @SapInboundAccepted("items")
    fun upsert(items: List<EmployeeMasterRequestItem>): EmployeeMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = employeeUpsertService.upsert(commands)

        return EmployeeMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun EmployeeMasterRequestItem.toCommand(): EmployeeUpsertCommand = EmployeeUpsertCommand(
        employeeCode = employeeCode,
        employeeName = employeeName,
        gender = gender,
        homePhone = homePhone,
        workPhone = workPhone,
        workEmail = workEmail,
        email = email,
        startDate = startDate,
        endDate = endDate,
        status = status,
        birthdate = birthdate,
        orgCode = orgCode,
        lockingFlag = lockingFlag
    )
}
