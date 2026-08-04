package com.otoki.powersales.domain.org.organization.branchmapping

/**
 * SF `BranchMapping__mdt` **누락 행 보정** SoT — Stage2 `branch-mapping-supplement` substep 이 적재한다.
 *
 * ## 왜 필요한가
 * `branch_mapping` 은 SF 커스텀메타 74건을 Stage1 CSV 로 적재한 스냅샷이다
 * ([com.otoki.powersales.domain.org.organization.branchmapping.entity.BranchMapping]). 그런데 SF 원본
 * 자체에 행이 빠져 있는 조직이 있고, 그 조직 사용자는 [BranchCodeExpander] 가 확장할 매핑을 못 찾아
 * 자기 코드 하나로만 조회된다 (expander 는 매칭 없으면 입력을 그대로 pass-through).
 *
 * 본 object 는 그 누락분을 **backend 쪽에서 보정**하는 행 목록이다. SF 원본을 고칠 수 있게 되면
 * 해당 행을 SF 에 추가하고 여기서 제거하면 된다 (Stage1 CSV 로 자연 흡수 — 중복 적재는 PK 충돌
 * `DO NOTHING` 이라 무해).
 *
 * ## 현재 보정 대상 — `E5694` (CVS전략팀)
 * | 조직 | 조직코드(셀렉터 산출값) | SF BranchMapping 키 | SF 확장값 |
 * |---|---|---|---|
 * | CVS1팀 | `E5692` | `E5692` ✔ | `5691,5692,5693,5694` |
 * | CVS2팀 | `E5693` | `E5693` ✔ | `5691,5692,5693,5694` |
 * | CVS전략팀 | `E5694` | **`5694`** ✘ | `5691,5692,5693,5694` |
 *
 * CVS 3형제 중 전략팀만 키가 평문 `5694` 로 잡혀 있다 (SF `customMetadata/BranchMapping.cvs.md-meta.xml`).
 * 조직 트리가 산출하는 실제 코드는 `E5694` 라서 `expand("E5694") = {E5694}` 가 되고, CVS전략팀 조장의
 * 거래처/일정 조회가 0건이 된다. SF 에서도 동일하게 확장되지 않지만, SF 거래처 탭은 지점 필터 없이
 * sharing rule (`Account.sharingRules` `CVS` / `cvsjr`) 로만 걸러져 증상이 드러나지 않았다.
 *
 * ## 확장값은 SF 원본 3행(`5691,5692,5693,5694`)이 아니라 **E 코드를 포함한 집합**이다
 * 운영 `account.branch_code` 실측 (2026-08-04) — CVS 거래처는 전량 E 코드로 적재돼 있고 평문 코드로
 * 적재된 거래처는 **0건**이다:
 *
 * | branch_code | 지점명 | 건수 |
 * |---|---|---|
 * | `E5692` | CVS1팀 | 706 |
 * | `E5693` | CVS2팀 | 440 |
 * | `5691`~`5694` | — | 0 |
 *
 * 따라서 SF 원본 3행의 값(`5691,5692,5693,5694`)을 그대로 복제하면 실재하지 않는 코드만 가리켜
 * 여전히 0건이다. 값은 SF 가 **같은 조직에 대해 직접 써 둔** sharing rule 의 코드 집합
 * (`Account.sharingRules` 의 `CVS`(role `CVS`) / `cvsjr`(role and subordinates `CVS_S`) 두 룰 모두
 * `BranchCode__c IN 5691,5692,5693,5694,E5692,E5693,E5694`) 을 따른다. 레거시 하드코딩 SOQL
 * (`TeamMemberListController.cls:41` — CVS 담당 특수 사번 4명)도 같은 집합이라, 두 출처가 공통으로
 * "CVS 담당자가 보는 거래처 = CVS 전 코드" 를 표현한다.
 *
 * 평문 코드도 함께 남긴다 — 현재 매칭되는 거래처는 없지만 SAP 적재가 평문으로 바뀌어도 계속 동작한다.
 *
 * ## CVS1/CVS2 는 손대지 않았다
 * `E5692`/`E5693` 행은 SF 원본이 그대로 있어 각 팀 조장이 **자기 팀 거래처만** 본다 (입력 코드 자신은
 * 항상 확장 결과에 포함되므로). SF sharing rule 은 두 팀에 서로의 거래처까지 열어 주지만, 신규는 지점
 * 셀렉터 필터가 AND 로 더 걸려 본인 팀으로 좁혀진다. 이 범위를 SF 와 맞출지는 운영 정책 결정 사항이라
 * 보류한다 — 넓히려면 두 코드도 본 SoT 에 행으로 추가하면 된다(기존 행을 SoT 값으로 덮어쓴다).
 *
 * ## 기존 `5694` 행은 지우지 않는다
 * 전사 권한자용 34개 고정 지점 목록
 * ([com.otoki.powersales.admin.service.DashboardBranchResolver.DASHBOARD_ALL_BRANCHES]) 이 CVS전략팀을
 * `5694` 로 들고 있어, 그 경로는 여전히 `5694` 를 확장 입력으로 넘긴다. 키를 rename 하면 조장은 고쳐지고
 * 전사 권한자 화면이 깨진다. 두 코드가 같은 집합으로 확장되도록 **행을 추가**하는 것이 정답이다.
 */
object BranchMappingSupplement {

    /**
     * 보정 행 1건.
     *
     * @param branchCode `branch_mapping.branch_code` (PK) — 조직 트리가 산출하는 실제 조직코드.
     * @param includedBranchCodes 확장 대상 코드 CSV. expander 가 입력 코드를 항상 결과에 포함하므로
     *   자기 자신은 없어도 동작하지만, 화면(`시스템 > 지점 코드 맵핑`)에서 확장 결과를 읽을 때
     *   자기 자신이 보이도록 SF 원본 행들과 같은 표기로 함께 적는다.
     * @param label 화면 표기용 라벨 — SF 원본 대응 행의 label 을 따른다.
     */
    data class Row(
        val branchCode: String,
        val includedBranchCodes: String,
        val label: String,
    )

    /**
     * 적재 대상 전량 — **본 목록이 값의 권위 출처**다.
     *
     * substep 은 행이 없으면 INSERT, 있으면 값이 다를 때 UPDATE 한다(SoT 강제). 그래서 값을 고친 뒤
     * 재실행하면 그대로 반영된다. 추가 보정이 필요해지면 근거를 KDoc 에 적고 여기에 행을 더한다.
     */
    val ROWS: List<Row> = listOf(
        Row(
            branchCode = "E5694",
            includedBranchCodes = "5691,5692,5693,5694,E5692,E5693,E5694",
            label = "cvs전략",
        ),
    )
}
