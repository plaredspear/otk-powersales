package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.UpdateAuthorityRequest
import com.otoki.internal.admin.dto.request.UpdateRolePermissionsRequest
import com.otoki.internal.admin.dto.request.UpdateUserPermissionsRequest
import com.otoki.internal.admin.dto.response.EmployeePermissionDetailResponse
import com.otoki.internal.admin.dto.response.RolePermissionsUpdateResponse
import com.otoki.internal.admin.dto.response.UpdateAuthorityResponse
import com.otoki.internal.admin.entity.RolePermission
import com.otoki.internal.admin.entity.UserPermission
import com.otoki.internal.admin.exception.*
import com.otoki.internal.admin.repository.RolePermissionRepository
import com.otoki.internal.admin.repository.UserPermissionRepository
import com.otoki.internal.admin.scope.AdminEmployeeHolder
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.sap.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminEmployeePermissionService(
    private val employeeRepository: EmployeeRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val userPermissionRepository: UserPermissionRepository,
    private val adminPermissionResolver: AdminPermissionResolver,
    private val adminEmployeeHolder: AdminEmployeeHolder
) {

    companion object {
        val ALLOWED_AUTHORITIES = setOf("시스템관리자", "조장", "지점장", "영업부장", "사업부장", "영업본부장", "영업지원실")
    }

    fun getEmployeePermissions(employeeId: Long): EmployeePermissionDetailResponse {
        requireSystemAdmin()
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException() }
        val result = adminPermissionResolver.resolveWithDetails(employee)
        return EmployeePermissionDetailResponse.from(employee, result)
    }

    @Transactional
    fun updateUserPermissions(employeeId: Long, request: UpdateUserPermissionsRequest): EmployeePermissionDetailResponse {
        val currentEmployee = requireSystemAdmin()
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException() }

        if (currentEmployee.id == employee.id) {
            throw CannotModifyOwnPermissionException()
        }

        validatePermissions(request.permissions)

        userPermissionRepository.deleteByEmployeeId(employeeId)
        request.permissions.forEach { perm ->
            userPermissionRepository.save(
                UserPermission(
                    employeeId = employeeId,
                    permission = perm,
                    grantedBy = currentEmployee.id
                )
            )
        }

        val result = adminPermissionResolver.resolveWithDetails(employee)
        return EmployeePermissionDetailResponse.from(employee, result)
    }

    @Transactional
    fun updateAuthority(employeeId: Long, request: UpdateAuthorityRequest): UpdateAuthorityResponse {
        val currentEmployee = requireSystemAdmin()
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException() }

        if (currentEmployee.id == employee.id) {
            throw CannotModifyOwnAuthorityException()
        }

        if (request.appAuthority !in ALLOWED_AUTHORITIES) {
            throw InvalidAuthorityException(request.appAuthority)
        }

        val previousAuthority = employee.appAuthority
        employee.appAuthority = request.appAuthority
        employeeRepository.save(employee)

        val effectivePermissions = adminPermissionResolver.resolve(employee).map { it.name }

        return UpdateAuthorityResponse(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name,
            previousAuthority = previousAuthority,
            newAuthority = request.appAuthority,
            effectivePermissions = effectivePermissions
        )
    }

    @Transactional
    fun updateRolePermissions(role: String, request: UpdateRolePermissionsRequest): RolePermissionsUpdateResponse {
        requireSystemAdmin()

        if (role !in ALLOWED_AUTHORITIES) {
            throw InvalidAuthorityException(role)
        }

        validatePermissions(request.permissions)

        rolePermissionRepository.deleteByRole(role)
        request.permissions.forEach { perm ->
            rolePermissionRepository.save(
                RolePermission(
                    role = role,
                    permission = perm
                )
            )
        }

        return RolePermissionsUpdateResponse(
            role = role,
            permissions = request.permissions
        )
    }

    private fun requireSystemAdmin(): com.otoki.internal.sap.entity.Employee {
        val employee = adminEmployeeHolder.employee
            ?: throw AdminForbiddenException()
        if (employee.appAuthority != "시스템관리자") {
            throw AdminForbiddenException()
        }
        return employee
    }

    private fun validatePermissions(permissions: List<String>) {
        val seen = mutableSetOf<String>()
        for (perm in permissions) {
            try {
                AdminPermission.valueOf(perm)
            } catch (_: IllegalArgumentException) {
                throw InvalidPermissionException(perm)
            }
            if (!seen.add(perm)) {
                throw DuplicatePermissionException(perm)
            }
        }
    }
}
