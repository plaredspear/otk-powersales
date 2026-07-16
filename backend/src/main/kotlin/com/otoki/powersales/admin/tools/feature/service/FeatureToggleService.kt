package com.otoki.powersales.admin.tools.feature.service

import com.otoki.powersales.admin.tools.feature.FeatureFlag
import com.otoki.powersales.admin.tools.feature.dto.FeatureToggleItem
import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * 기능 토글 조회/변경 + 등록 API 차단 게이트.
 *
 * 등록 컨트롤러 진입부에서 [ensureEnabled] 를 호출해, 비활성 flag 면 [BusinessException] 을 던져
 * 요청을 차단한다 (HTTP 409). 관리자가 입력한 사유가 있으면 그 문구를, 없으면 기본 문구를 노출한다.
 */
@Service
class FeatureToggleService(
    private val store: FeatureToggleStore,
) {

    /** 전체 flag 의 현재 상태 목록 (관리 화면 표 source). */
    fun list(): List<FeatureToggleItem> =
        FeatureFlag.entries.map { flag ->
            val state = store.getState(flag)
            FeatureToggleItem(
                code = flag.code,
                label = flag.label,
                enabled = state.enabled,
                reason = state.reason,
            )
        }

    /** flag 상태 변경. 변경 후의 최신 항목을 반환. */
    fun setEnabled(flag: FeatureFlag, enabled: Boolean, reason: String?): FeatureToggleItem {
        store.setState(flag, enabled, reason)
        val state = store.getState(flag)
        return FeatureToggleItem(
            code = flag.code,
            label = flag.label,
            enabled = state.enabled,
            reason = state.reason,
        )
    }

    /**
     * flag 가 비활성이면 [BusinessException]("FEATURE_DISABLED", 409) 을 던진다.
     * 활성이면 아무것도 하지 않는다. 등록 컨트롤러 진입부에서 호출한다.
     */
    fun ensureEnabled(flag: FeatureFlag) {
        val state = store.getState(flag)
        if (state.enabled) return
        val reason = state.reason?.takeIf { it.isNotBlank() }
        throw BusinessException(
            errorCode = "FEATURE_DISABLED",
            message = reason ?: "${flag.label} 기능이 일시적으로 중지되었습니다. 관리자에게 문의하세요.",
            httpStatus = HttpStatus.CONFLICT,
        )
    }
}
