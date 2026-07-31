package com.otoki.powersales.platform.common.dto.response

import java.text.Collator
import java.util.Locale

data class BranchResponse(
    val branchCode: String,
    val branchName: String
) {
    companion object {
        /**
         * 지점 셀렉터 표시 순서 — **지점명 가나다순** (2026-07-31 운영 결정).
         *
         * 종전에는 조직코드(`org_cd3 → org_cd4 → org_cd5`) 오름차순이었다. 조직 계층(사업부 → 영업부 →
         * 지점) 순서는 보존됐지만, 영업부 안에서 지점 코드가 지점명과 무관하게 이력적으로 부여돼 있어
         * (강북1=5815, 강북4=5816, 강남1=5817) 목록이 뒤죽박죽으로 읽혔다. 셀렉터에서는 찾기 쉬움이
         * 우선이라 전체를 이름순으로 정렬한다.
         *
         * 상위 그룹(사업부/영업부) 을 화면에 표시할 수 있게 되면 "그룹 단위 정렬 + 그룹 안에서 이름순"
         * 으로 되돌린다 — 그때는 조직 계층과 가독성을 함께 만족할 수 있다.
         *
         * 한글 정렬은 [Collator] (KOREAN) 기준이라 DB collation 에 의존하지 않는다. 동명이 있으면
         * 지점 코드로 tie-break 해 순서가 요청마다 흔들리지 않게 한다.
         */
        val NAME_ORDER: Comparator<BranchResponse> = run {
            val collator = Collator.getInstance(Locale.KOREAN)
            compareBy(collator) { branch: BranchResponse -> branch.branchName }
                .thenBy { it.branchCode }
        }
    }
}
