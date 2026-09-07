package com.otoki.powersales.user.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.organization.repository.dto.OrganizationCacheDto
import com.otoki.powersales.platform.auth.repository.UserRoleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 사원의 조직명 + 직책으로 SF `UserRole` 을 산출하는 도메인 서비스.
 *
 * 레거시 매핑: SF `AppointmentTriggerHanlder.cls:233-398` `updateUser(@future)` 의 UserRole 배정 블록.
 * 동작 요약 (입력 / 분기 / 외부 호출 / 부수 효과):
 *  - 입력: `Employee.orgName` + `Employee.jikchak` + `Employee.costCenterCode` + 조직 cascade 결과
 *  - 외부 호출: `UserRoleRepository.findAll()` — 이름 → id 맵 (호출자가 [loadNameIndex] 로 1회 적재해 전달)
 *  - 분기: 직책 접미사 4종 → 특수 조직 prefix 2종 → 이름 해석 cascade 4단
 *  - 부수 효과: 없음 (순수 산출). `User.userRoleId` 대입은 호출자 책임.
 *
 * ## 왜 필요한가
 * `user_role_id` 는 SF sharing rule 의 `ROLE` / `ROLE_AND_SUBORDINATES` 타겟 매칭과 role hierarchy
 * 부여의 **유일한 연결 고리**다. 비어 있으면 그 사용자는 조직도 밖에 놓여, OWD 가 `Private` 인 SObject
 * (Account 등) 조회가 소유 레코드 외 전부 차단된다 (`SharingRulePolicyEvaluator`).
 * 신규 이식 시 본 블록이 누락되어 마이그레이션 이후 생성된 사용자가 전부 NULL 이었다.
 *
 * ## 레거시 이탈 2건
 * 1. **미매칭 시 `null` 미저장** — SF 는 최종 조회 실패 시 `UserRoleId = null` 을 그대로 써서 기존
 *    배정까지 지운다. 신규는 [resolveUserRoleId] 가 null 을 반환하고 **호출자가 기존 값을 유지**한다.
 *    (SF 의 이 동작이 곧 위 "조직도 밖" 상태를 만든다 — 재현 가치가 없다.)
 * 2. **정규화 키 충돌 시 결정적 선택** — SF 는 `Map.put` last-wins 인데 SOQL 에 `ORDER BY` 가 없어
 *    어느 UserRole 이 이길지 비결정적이다. 신규는 첫 행을 고정 채택하고 충돌을 경고 로그로 남긴다.
 *    (운영 실측 273행에 충돌 0건 — 방어적 처리.)
 */
@Service
class UserRoleAssignmentResolver(
    private val userRoleRepository: UserRoleRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * `UserRole.Name` → id 색인 적재 (SF cls:241-256 `userRoleMap` 동등).
     *
     * SF 는 맵에 넣을 때 이름을 정규화한다 — DB 이름에는 공백이 있고 `Employee.orgName` 에는 없어
     * 생긴 보정이다 (예: UserRole `마케팅 1부` ↔ 사원 조직명 `마케팅1부`). if / else-if 체인이라
     * **이름 하나에 정규화는 최대 하나만** 적용된다.
     *
     * 발령 1건마다 273행을 다시 읽지 않도록 호출자가 1회 적재해 [resolveUserRoleId] 에 넘긴다.
     */
    fun loadNameIndex(): Map<String, Long> {
        val index = mutableMapOf<String, Long>()
        userRoleRepository.findAll()
            .filter { it.name.isNotBlank() }
            .forEach { role ->
                val key = normalizeRoleName(role.name)
                val existing = index.putIfAbsent(key, role.id)
                if (existing != null) {
                    // SF 는 last-wins + SOQL 무순서라 비결정적 — 신규는 첫 행 고정 (레거시 이탈 2).
                    log.warn(
                        "[user-role] 정규화 이름 충돌 — 첫 행 유지: key={}, kept={}, ignored={}({})",
                        key, existing, role.id, role.name,
                    )
                }
            }
        return index
    }

    /** SF cls:243-255 정합 — if / else-if 체인이라 최초 매칭 분기 하나만 적용. */
    internal fun normalizeRoleName(name: String): String = when {
        name.contains("마케팅 ") -> name.replace("마케팅 ", "마케팅")
        // 현재 운영 데이터에 `판매기획` 이름 0건 — SF 조직 개편 전 잔재로 no-op 이나 정합을 위해 유지.
        name.contains("판매기획") -> name.replace("판매기획", "판매전략")
        name.contains("e-Biz") -> name.replace("e-Biz ", "e-Biz").replace("영업본부_", "")
        else -> name
    }

    /**
     * 사원 → `UserRole.id` 산출. 미매칭 시 null (호출자가 기존 값 유지 — 레거시 이탈 1).
     *
     * [org] 는 `Employee.costCenterCode` 의 조직 cascade (Level5→4→3) 결과. SF 는 전체 블록이
     * `if (orgInfoTmp != null)` 가드 안이므로 조직을 못 찾으면 산출 자체를 하지 않는다.
     */
    fun resolveUserRoleId(
        employee: Employee,
        org: OrganizationCacheDto,
        nameIndex: Map<String, Long>,
    ): Long? {
        val orgNameLevel3 = org.orgNameLevel3.orEmpty()

        // ① 기준 문자열 = OrgName + 직책 접미사, 그 앞에 특수 조직 prefix.
        val candidate1 = applyOrgPrefix(
            employee = employee,
            org = org,
            base = employee.orgName.orEmpty() + suffixFor(employee, org),
        )
        nameIndex[candidate1]?.let { return finalize(it, candidate1, orgNameLevel3, nameIndex) }

        // ② 미매칭 → 직책 접미사 제거 후 재조회 (SF "직책별 역할이 나뉘어있지 않은 경우").
        val candidate2 = candidate1.replace(SUFFIX_SALES_REP, "").replace(SUFFIX_TEAM_LEADER, "")
        nameIndex[candidate2]?.let { return finalize(it, candidate2, orgNameLevel3, nameIndex) }

        // ③ 여전히 미매칭 → 사업부명 prefix (SF 주석 "제X사업부 X영업부").
        val candidate3 = "${orgNameLevel3}_$candidate2"
        return finalize(nameIndex[candidate3], candidate3, orgNameLevel3, nameIndex)
    }

    /**
     * SF cls:392-397 — `영업부` 를 포함하고 사업부가 `Retail사업부` 면 prefix 를 덧붙여 **재조회한다**.
     *
     * `userRoleId == null` 가드가 없는 것이 핵심이다. 앞 단계에서 이미 찾았더라도 덮어쓴다 —
     * 이름이 겹치는 `N영업부`(타 사업부)와 `Retail사업부_N영업부` 를 가르는 의도적 보정이기 때문이다
     * (운영 실측: `1~7영업부` 와 `Retail사업부_1~8영업부` 가 함께 존재).
     */
    private fun finalize(
        resolvedId: Long?,
        candidate: String,
        orgNameLevel3: String,
        nameIndex: Map<String, Long>,
    ): Long? {
        if (!candidate.contains(SALES_DEPT_KEYWORD) || orgNameLevel3 != RETAIL_DIVISION) return resolvedId
        val retailCandidate = "${orgNameLevel3}_$candidate"
        return nameIndex[retailCandidate]
    }

    /**
     * 직책 접미사 (SF cls:325-364 ProfileId 분기와 같은 블록에서 결정).
     *
     * 마케팅실 / 지원실(조장 계열 제외) / 판매전략팀 / BS·SP팀 은 분기 자체를 타지 않아 접미사가 없다.
     */
    internal fun suffixFor(employee: Employee, org: OrganizationCacheDto): String {
        val orgCodeLevel3 = org.orgCodeLevel3
        val jikchak = employee.jikchak.orEmpty()
        return when {
            orgCodeLevel3 == ORG_MARKETING -> ""
            orgCodeLevel3 == ORG_SUPPORT && !(jikchak.contains("판매") || jikchak == "조장") -> ""
            orgCodeLevel3 == ORG_SALES_STRATEGY -> ""
            employee.costCenterCode in BS_SP_TEAM_CODES -> ""
            jikchak.contains("조장") || jikchak == "판매팀장" -> SUFFIX_TEAM_LEADER
            jikchak.contains("지점장") || jikchak.contains("팀장") -> ""
            jikchak.contains("부장") -> ""
            else -> SUFFIX_SALES_REP
        }
    }

    /**
     * 특수 조직 prefix (SF cls:367-379).
     *
     * - 유통총괄실(Level3 = 1837): **본인 코스트센터가 Level5 로 정확히 잡힐 때만** Level4 조직명을 앞에
     * - 제1사업부 제품개발팀(코스트센터 5638): Level3 조직명을 앞에
     */
    internal fun applyOrgPrefix(employee: Employee, org: OrganizationCacheDto, base: String): String {
        // 바깥 if / else-if 구조를 그대로 유지한다 — 유통총괄실이면 Level5 불일치로 prefix 를 못 붙여도
        // 5638 분기로 넘어가지 않는다 (`when` 의 fall-through 로 옮기면 이 지점이 어긋난다).
        if (org.orgCodeLevel3 == ORG_DISTRIBUTION_HQ) {
            return if (org.orgCodeLevel5 != null && org.orgCodeLevel5 == employee.costCenterCode) {
                "${org.orgNameLevel4.orEmpty()}_$base"
            } else {
                base
            }
        }
        if (employee.costCenterCode == COST_CENTER_PRODUCT_DEV) {
            return "${org.orgNameLevel3.orEmpty()}_$base"
        }
        return base
    }

    companion object {
        private const val SUFFIX_TEAM_LEADER = "_조장"
        private const val SUFFIX_SALES_REP = "_영업사원"
        private const val SALES_DEPT_KEYWORD = "영업부"
        private const val RETAIL_DIVISION = "Retail사업부"

        // SF cls:327-340 — 접미사 미부여 조직 (ProfileId 분기가 Staff/마케팅으로 빠지는 케이스)
        private const val ORG_MARKETING = "5066"
        private const val ORG_SUPPORT = "3475"
        private const val ORG_SALES_STRATEGY = "3472"
        private val BS_SP_TEAM_CODES = setOf("5397", "5398", "5639")

        // SF cls:371-378 — 특수 prefix 조직
        private const val ORG_DISTRIBUTION_HQ = "1837"
        private const val COST_CENTER_PRODUCT_DEV = "5638"
    }
}
