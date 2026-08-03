package com.otoki.powersales.admin.dto

/**
 * 거래처 → 지점 셀렉터 역산 결과
 * ([com.otoki.powersales.admin.service.BranchScopeGateway.resolveSelectorBranches]).
 *
 * "지점을 먼저 고르지 않으면 거래처를 고를 수 없다" 는 선행 강제를 뒤집기 위한 상태값이다.
 * 거래처가 보유한 `Account.branchCode` 하나를 **지점 셀렉터 옵션 코드**로 되돌려, 화면이
 * 지점 체크박스를 자동 선택(다중 UI)하거나 타 지점 혼입을 차단(단일 UI)할 수 있게 한다.
 *
 * 역산이 단순 동등 비교가 아닌 이유는
 * [com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander] 의 확장이
 * 별칭뿐 아니라 **롤업**(예: `5829` retail3영업부 → `5826,5827,5828`) 을 포함하기 때문이다.
 * 한 거래처 코드가 둘 이상의 셀렉터 옵션에 걸리면 자동 선택이 오히려 잘못된 지점을 고르므로
 * [Ambiguous] 로 내려 화면이 사용자에게 직접 선택을 요구한다 (fail-safe).
 */
sealed interface SelectorBranchResult {

    /** 화면이 그대로 소비하는 코드값 — 응답 DTO 의 `selectorBranchStatus` 필드에 실린다. */
    val status: String

    /** 자동 선택할 셀렉터 옵션 코드. [Resolved] 가 아니면 null. */
    val branchCode: String?
        get() = null

    /** 안내 문구용 지점명 ("현재 OO지점 거래처만 선택할 수 있습니다"). [Resolved] 가 아니면 null. */
    val branchName: String?
        get() = null

    /** 셀렉터 옵션 하나로 확정 — 화면이 이 지점을 자동 선택한다. */
    data class Resolved(
        override val branchCode: String,
        override val branchName: String,
    ) : SelectorBranchResult {
        override val status: String = STATUS_RESOLVED
    }

    /** 롤업 매핑으로 후보가 둘 이상 — 자동 선택하지 않고 사용자에게 지점 직접 선택을 요구한다. */
    data object Ambiguous : SelectorBranchResult {
        override val status: String = STATUS_AMBIGUOUS
    }

    /** 사용자의 지점 셀렉터 어디에도 귀속되지 않음 — 조회 권한 범위 밖 거래처. */
    data object OutOfScope : SelectorBranchResult {
        override val status: String = STATUS_OUT_OF_SCOPE
    }

    companion object {
        const val STATUS_RESOLVED = "RESOLVED"
        const val STATUS_AMBIGUOUS = "AMBIGUOUS"
        const val STATUS_OUT_OF_SCOPE = "OUT_OF_SCOPE"
    }
}
