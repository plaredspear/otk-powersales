package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.BranchDto
import com.otoki.internal.common.exception.BusinessException
import com.otoki.internal.sap.repository.OrganizationRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AdminScheduleService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val templateGenerator: ScheduleTemplateGenerator
) {

    fun getBranches(): List<BranchDto> {
        return userRepository.findDistinctBranches()
            .filter { it.branchCode.isNotBlank() && it.branchName.isNotBlank() }
            .map { BranchDto(costCenterCode = it.branchCode, branchName = it.branchName) }
    }

    fun generateTemplate(costCenterCode: String): TemplateResult {
        // 조직 존재 확인
        organizationRepository.findFirstByCostCenterLevel5(costCenterCode)
            ?: organizationRepository.findFirstByCostCenterLevel4(costCenterCode)
            ?: throw OrganizationNotFoundException()

        // 사원 목록 조회: appAuthority IS NULL, appLoginActive=true, status="재직"
        val employees = userRepository.findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
            costCenterCode, "재직"
        ).sortedWith(compareBy({ it.orgName }, { it.employeeId }))

        val excelBytes = templateGenerator.generate(employees)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val filename = "진열스케줄_양식_${costCenterCode}_${timestamp}.xlsx"

        return TemplateResult(excelBytes, filename)
    }

    data class TemplateResult(
        val bytes: ByteArray,
        val filename: String
    )
}

class OrganizationNotFoundException : BusinessException(
    errorCode = "ORGANIZATION_NOT_FOUND",
    message = "존재하지 않는 지점 코드입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class MissingCostCenterCodeException : BusinessException(
    errorCode = "MISSING_PARAMETER",
    message = "cost_center_code는 필수입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
