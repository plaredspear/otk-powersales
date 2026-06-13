package com.otoki.powersales.productexpiration.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.productexpiration.dto.request.AdminProductExpirationBatchDeleteRequest
import com.otoki.powersales.productexpiration.dto.request.AdminProductExpirationCreateRequest
import com.otoki.powersales.productexpiration.dto.request.AdminProductExpirationUpdateRequest
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationBatchDeleteResponse
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationListResponse
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationResponse
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.repository.ProfileRepository
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.exception.ProductNotFoundException
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.productexpiration.entity.ProductExpiration
import com.otoki.powersales.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.productexpiration.exception.ProductExpirationAccountNotFoundException
import com.otoki.powersales.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.productexpiration.repository.ProductExpirationRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminProductExpirationService(
    private val productExpirationRepository: ProductExpirationRepository,
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository
) {

    fun getList(
        userId: Long,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        employeeKeyword: String?,
        accountKeyword: String?,
        status: String?,
        pageable: Pageable
    ): AdminProductExpirationListResponse {
        val today = LocalDate.now()
        val employeeIds = resolveEmployeeScope(userId)

        val page = productExpirationRepository.findForAdmin(
            fromDate, toDate, employeeKeyword, accountKeyword, status, today, pageable, employeeIds
        )

        val content = page.content.map { AdminProductExpirationResponse.Companion.from(it, today) }
        return AdminProductExpirationListResponse(
            content = content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }

    fun getDetail(userId: Long, id: Int): AdminProductExpirationResponse {
        val entity = findById(id)
        validateScope(entity, resolveEmployeeScope(userId))
        return AdminProductExpirationResponse.Companion.from(entity)
    }

    @Transactional
    fun create(userId: Long, request: AdminProductExpirationCreateRequest): AdminProductExpirationResponse {
        val employee = employeeRepository.findByEmployeeCode(request.employeeCode)
            .orElseThrow { EmployeeNotFoundException() }

        val employeeIds = resolveEmployeeScope(userId)
        if (employeeIds != null && employee.id !in employeeIds) {
            throw ProductExpirationForbiddenException()
        }

        val account = accountRepository.findByExternalKey(request.accountCode)
            ?: throw ProductExpirationAccountNotFoundException()

        val product = productRepository.findByProductCode(request.productCode)
            ?: throw ProductNotFoundException(request.productCode)

        val expirationDate = LocalDate.parse(request.expirationDate)
        val alarmDate = LocalDate.parse(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        val entity = ProductExpiration(
            employeeId = employee.id,
            accountId = account.id,
            accountName = account.name,
            accountCode = request.accountCode,
            productId = product.id,
            productName = product.name,
            productCode = request.productCode,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            description = request.description
        )

        val saved = productExpirationRepository.save(entity)
        // Reload to populate employee relation for response
        return getDetail(userId, saved.productExpirationId)
    }

    @Transactional
    fun update(userId: Long, id: Int, request: AdminProductExpirationUpdateRequest): AdminProductExpirationResponse {
        val entity = findById(id)
        validateScopeForWrite(entity, resolveEmployeeScope(userId))

        val expirationDate = LocalDate.parse(request.expirationDate)
        val alarmDate = LocalDate.parse(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        entity.update(expirationDate, alarmDate, request.description)
        return AdminProductExpirationResponse.Companion.from(entity)
    }

    @Transactional
    fun delete(userId: Long, id: Int) {
        val entity = findById(id)
        validateScopeForWrite(entity, resolveEmployeeScope(userId))
        productExpirationRepository.delete(entity)
    }

    @Transactional
    fun batchDelete(userId: Long, request: AdminProductExpirationBatchDeleteRequest): AdminProductExpirationBatchDeleteResponse {
        val employeeIds = resolveEmployeeScope(userId)
        val entities = productExpirationRepository.findAllById(request.ids)
        if (entities.size != request.ids.size) {
            throw ProductExpirationNotFoundException()
        }
        entities.forEach { validateScopeForWrite(it, employeeIds) }
        productExpirationRepository.deleteAll(entities)
        return AdminProductExpirationBatchDeleteResponse(deletedCount = entities.size)
    }

    fun getSummary(userId: Long): AdminProductExpirationSummaryResponse {
        val employeeIds = resolveEmployeeScope(userId)
        return productExpirationRepository.getSummary(LocalDate.now(), employeeIds)
    }

    /**
     * 로그인 사원의 역할에 따라 조회 가능한 사원 ID 목록을 반환한다.
     * - 관리자급/지점장: null (전체)
     * - LEADER: 동일 orgName 팀원 ID 목록 (orgName이 null이면 본인만)
     * - 그 외(WOMAN, UNKNOWN, null 등): 본인 ID만
     */
    private fun resolveEmployeeScope(userId: Long): List<Long>? {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val role = employee.role
        val user = employee.employeeCode?.let { userRepository.findByEmployeeCode(it) }
        val profileName = user?.profileId?.let { profileRepository.findById(it).orElse(null)?.name }
        val isAllBranches = profileName == "시스템 관리자" || user?.isSalesSupport == true ||
            profileName in setOf("1.본부장", "2.사업부장", "3.영업부장")

        return when {
            isAllBranches || role == AppAuthority.BRANCH_MANAGER -> null
            role == AppAuthority.LEADER -> {
                val orgName = employee.orgName
                if (orgName != null) {
                    employeeRepository.findByOrgName(orgName).map { it.id }
                } else {
                    listOf(employee.id)
                }
            }
            else -> listOf(employee.id)
        }
    }

    private fun validateScope(entity: ProductExpiration, employeeIds: List<Long>?) {
        if (employeeIds == null) return
        if (entity.employeeId == null || entity.employeeId !in employeeIds) {
            throw ProductExpirationNotFoundException()
        }
    }

    private fun validateScopeForWrite(entity: ProductExpiration, employeeIds: List<Long>?) {
        if (employeeIds == null) return
        if (entity.employeeId == null || entity.employeeId !in employeeIds) {
            throw ProductExpirationForbiddenException()
        }
    }

    private fun findById(id: Int): ProductExpiration {
        return productExpirationRepository.findById(id)
            .orElseThrow { ProductExpirationNotFoundException() }
    }
}
