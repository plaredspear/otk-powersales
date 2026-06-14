package com.otoki.powersales.domain.activity.schedule.policy

import com.otoki.powersales.domain.foundation.account.entity.Account

/**
 * 출근등록 GPS 거리 검증 면제 정책 평가자 (Spec #586).
 *
 * 거래처(`Account`)의 ABC 코드를 기반으로 면제 여부를 판정한다. 향후 면제 사유 다변화
 * (예: 임시 거래처, 공장직송 등) 시 본 클래스가 확장 지점 역할을 한다.
 *
 * 평가 우선순위 (Spec #586 Q6):
 * - 본 정책은 #585 GPS 검증보다 **상위** 에서 동작한다.
 * - 면제 코드인 경우 거래처의 위경도 누락 / 파싱 실패 / 사용자 좌표 무효 / Haversine 거리
 *   초과 등 #585 의 모든 검증을 진입하지 않는다.
 */
object AbcExemptPolicy {

    const val REASON_ABC_EXEMPT: String = "ABC_EXEMPT"

    /**
     * 평가 결과.
     *
     * @property skipped GPS 검증 우회 여부
     * @property reason 우회 사유 코드 (`null` 이면 검증 진행)
     */
    data class Result(val skipped: Boolean, val reason: String?) {
        companion object {
            val NOT_EXEMPT: Result = Result(skipped = false, reason = null)
        }
    }

    fun evaluate(account: Account?): Result {
        if (AbcExemptCode.isExempt(account?.abcTypeCode)) {
            return Result(skipped = true, reason = REASON_ABC_EXEMPT)
        }
        return Result.NOT_EXEMPT
    }
}
