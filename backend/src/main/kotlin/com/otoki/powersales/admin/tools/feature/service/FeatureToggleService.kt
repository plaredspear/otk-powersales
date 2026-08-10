package com.otoki.powersales.admin.tools.feature.service

import com.otoki.powersales.admin.tools.feature.FeatureFlag
import com.otoki.powersales.admin.tools.feature.dto.FeatureToggleExemptEmployee
import com.otoki.powersales.admin.tools.feature.dto.FeatureToggleItem
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * 기능 토글 조회/변경 + 등록 API 차단 게이트.
 *
 * 등록 컨트롤러 진입부에서 [ensureEnabled] 를 호출해, 비활성 flag 면 [BusinessException] 을 던져
 * 요청을 차단한다 (HTTP 409). 관리자가 입력한 사유가 있으면 그 문구를, 없으면 기본 문구를 노출한다.
 *
 * 단, flag 별 **예외 사번** 목록에 등록된 사원은 비활성 상태에서도 통과시킨다 (예: 긴급 주문을
 * 대신 넣어야 하는 담당자). 사번 조회는 비활성일 때만 수행하므로 정상 운영 중에는 DB 조회가 없다.
 */
@Service
class FeatureToggleService(
    private val store: FeatureToggleStore,
    private val employeeRepository: EmployeeRepository,
) {

    /** 전체 flag 의 현재 상태 목록 (관리 화면 표 source). */
    fun list(): List<FeatureToggleItem> {
        val exemptCodesByFlag = FeatureFlag.entries.associateWith { store.getExemptEmployeeCodes(it) }
        val namesByCode = employeeNames(exemptCodesByFlag.values.flatten().distinct())
        return FeatureFlag.entries.map { flag ->
            toItem(flag, exemptCodesByFlag[flag].orEmpty(), namesByCode)
        }
    }

    /** flag 상태 변경. 변경 후의 최신 항목을 반환. 예외 사번 목록은 그대로 유지된다. */
    fun setEnabled(flag: FeatureFlag, enabled: Boolean, reason: String?): FeatureToggleItem {
        store.setState(flag, enabled, reason)
        return currentItem(flag)
    }

    /**
     * flag 의 예외 사번을 추가한다. 존재하지 않는 사번이면 400 으로 거부해 오타를 걸러낸다.
     * 이미 등록된 사번이면 Set 특성상 멱등하게 유지된다.
     */
    fun addExemptEmployee(flag: FeatureFlag, employeeCode: String): FeatureToggleItem {
        val trimmed = employeeCode.trim()
        if (!employeeRepository.existsByEmployeeCode(trimmed)) {
            throw BusinessException(
                errorCode = "EMPLOYEE_NOT_FOUND",
                message = "사번 $trimmed 인 사원을 찾을 수 없습니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        store.addExemptEmployee(flag, trimmed)
        return currentItem(flag)
    }

    /** flag 의 예외 사번을 제거한다. 목록에 없어도 성공으로 처리한다(멱등). */
    fun removeExemptEmployee(flag: FeatureFlag, employeeCode: String): FeatureToggleItem {
        store.removeExemptEmployee(flag, employeeCode.trim())
        return currentItem(flag)
    }

    /**
     * flag 가 비활성이고 [userId] 사원이 예외 사번이 아니면 [BusinessException]("FEATURE_DISABLED", 409)
     * 을 던진다. 활성이거나 예외 사원이면 아무것도 하지 않는다. 등록 컨트롤러 진입부에서 호출한다.
     */
    fun ensureEnabled(flag: FeatureFlag, userId: Long) {
        val state = store.getState(flag)
        if (state.enabled) return
        if (isExemptEmployee(flag, userId)) return
        val reason = state.reason?.takeIf { it.isNotBlank() }
        throw BusinessException(
            errorCode = "FEATURE_DISABLED",
            message = reason ?: "${flag.label} 기능이 일시적으로 중지되었습니다. 관리자에게 문의하세요.",
            httpStatus = HttpStatus.CONFLICT,
        )
    }

    /**
     * flag 활성 여부. **동작 전환형** flag 의 분기 판정용으로, 요청을 거부하지 않고 boolean 만 돌려준다
     * (차단형은 [ensureEnabled] 를 쓴다).
     *
     * [userId] 가 예외 사번이면 비활성 상태에서도 활성으로 본다 — 차단형의 예외 사번이 "막혀도 통과"
     * 인 것과 같은 의미로, 전사 롤백 없이 특정 사원만 신규 동작을 유지/검증할 수 있다.
     * 예외 사번 조회(DB 1회)는 flag 가 비활성일 때만 수행하므로 정상 운영 중 비용은 Redis GET 1회다.
     */
    fun isEnabled(flag: FeatureFlag, userId: Long): Boolean {
        if (store.getState(flag).enabled) return true
        return isExemptEmployee(flag, userId)
    }

    /** 사번이 없는 사원(외부 위탁 등)은 예외 지정 대상이 아니므로 false. */
    private fun isExemptEmployee(flag: FeatureFlag, userId: Long): Boolean {
        val employeeCode = employeeRepository.findById(userId).orElse(null)?.employeeCode
            ?: return false
        return store.isExemptEmployee(flag, employeeCode)
    }

    private fun currentItem(flag: FeatureFlag): FeatureToggleItem {
        val exemptCodes = store.getExemptEmployeeCodes(flag)
        return toItem(flag, exemptCodes, employeeNames(exemptCodes))
    }

    private fun toItem(
        flag: FeatureFlag,
        exemptCodes: List<String>,
        namesByCode: Map<String, String>,
    ): FeatureToggleItem {
        val state = store.getState(flag)
        return FeatureToggleItem(
            code = flag.code,
            label = flag.label,
            enabled = state.enabled,
            reason = state.reason,
            exemptEmployees = exemptCodes.map {
                FeatureToggleExemptEmployee(employeeCode = it, name = namesByCode[it])
            },
        )
    }

    /** 사번 → 사원명. 사번 등록 후 사원이 삭제되면 목록에서 빠지므로 화면에서는 name 이 null 이 된다. */
    private fun employeeNames(employeeCodes: List<String>): Map<String, String> {
        if (employeeCodes.isEmpty()) return emptyMap()
        return employeeRepository.findByEmployeeCodeIn(employeeCodes)
            .mapNotNull { employee -> employee.employeeCode?.let { it to employee.name } }
            .toMap()
    }
}
