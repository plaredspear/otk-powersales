package com.otoki.powersales.user.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.organization.repository.dto.OrganizationCacheDto
import com.otoki.powersales.platform.auth.entity.UserRole
import com.otoki.powersales.platform.auth.repository.UserRoleRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserRoleAssignmentResolver 테스트")
class UserRoleAssignmentResolverTest {

    private val userRoleRepository: UserRoleRepository = mockk(relaxed = true)
    private val resolver = UserRoleAssignmentResolver(userRoleRepository)

    /** 운영 실측 UserRole 이름 (인천1지점 3종 + Retail 영업부 충돌쌍 + 정규화 대상). */
    private fun stubRoles(vararg pairs: Pair<Long, String>) {
        every { userRoleRepository.findAll() } returns pairs.map { (id, name) -> role(id, name) }
    }

    private fun role(id: Long, name: String): UserRole = UserRole(id = id, name = name)

    private fun employee(
        orgName: String? = "인천1지점",
        jikchak: String? = "판매조장",
        costCenterCode: String? = "5826",
    ): Employee = Employee(employeeCode = "20250156", name = "김정현").apply {
        this.orgName = orgName
        this.jikchak = jikchak
        this.costCenterCode = costCenterCode
    }

    private fun org(
        orgCodeLevel3: String? = "3000",
        orgNameLevel3: String? = "Retail사업부",
        orgNameLevel4: String? = "3영업부",
        orgCodeLevel5: String? = "5826",
    ) = OrganizationCacheDto(
        orgCodeLevel3 = orgCodeLevel3,
        orgNameLevel3 = orgNameLevel3,
        orgNameLevel4 = orgNameLevel4,
        costCenterLevel3 = "3000",
        orgCodeLevel5 = orgCodeLevel5,
    )

    @Nested
    @DisplayName("직책별 배정 — 운영 실측 인천1지점 (51 지점 / 52 조장 / 53 영업사원)")
    inner class ByJikchak {

        private fun index() = resolver.loadNameIndex()

        @Test
        @DisplayName("판매조장 -> 인천1지점_조장 (52)")
        fun teamLeader() {
            stubRoles(51L to "인천1지점", 52L to "인천1지점_조장", 53L to "인천1지점_영업사원")
            assertThat(resolver.resolveUserRoleId(employee(jikchak = "판매조장"), org(), index()))
                .isEqualTo(52L)
        }

        @Test
        @DisplayName("지점장 -> 접미사 없이 인천1지점 (51)")
        fun branchManager() {
            stubRoles(51L to "인천1지점", 52L to "인천1지점_조장", 53L to "인천1지점_영업사원")
            assertThat(resolver.resolveUserRoleId(employee(jikchak = "지점장"), org(), index()))
                .isEqualTo(51L)
        }

        @Test
        @DisplayName("그 외 직책 -> 인천1지점_영업사원 (53)")
        fun salesRep() {
            stubRoles(51L to "인천1지점", 52L to "인천1지점_조장", 53L to "인천1지점_영업사원")
            assertThat(resolver.resolveUserRoleId(employee(jikchak = "사원"), org(), index()))
                .isEqualTo(53L)
        }

        @Test
        @DisplayName("직책별 역할이 없으면 접미사를 떼고 재조회 (SF 2단계)")
        fun fallsBackToBareName() {
            stubRoles(51L to "인천1지점")
            assertThat(resolver.resolveUserRoleId(employee(jikchak = "판매조장"), org(), index()))
                .isEqualTo(51L)
        }

        @Test
        @DisplayName("그래도 없으면 사업부명 prefix 로 재조회 (SF 3단계)")
        fun fallsBackToDivisionPrefix() {
            stubRoles(300L to "Retail사업부_주문센터")
            val e = employee(orgName = "주문센터", jikchak = "사원")
            assertThat(resolver.resolveUserRoleId(e, org(), index())).isEqualTo(300L)
        }

        @Test
        @DisplayName("끝내 미매칭이면 null — 호출자가 기존 값 유지 (레거시 이탈: SF 는 null 저장)")
        fun returnsNullWhenUnresolved() {
            stubRoles(51L to "인천1지점")
            val e = employee(orgName = "없는지점", jikchak = "사원")
            assertThat(resolver.resolveUserRoleId(e, org(), index())).isNull()
        }
    }

    @Nested
    @DisplayName("Retail사업부 영업부 보정 (SF cls:392-397 — null 가드 없음)")
    inner class RetailSalesDeptOverride {

        @Test
        @DisplayName("이미 매칭됐어도 Retail사업부_ prefix 로 덮어쓴다 (이름 충돌 해소)")
        fun overridesEvenAfterMatch() {
            // 운영 실측: `3영업부`(205) 와 `Retail사업부_3영업부`(180) 가 함께 존재.
            stubRoles(205L to "3영업부", 180L to "Retail사업부_3영업부")
            val e = employee(orgName = "3영업부", jikchak = "부장")
            assertThat(resolver.resolveUserRoleId(e, org(), resolver.loadNameIndex()))
                .isEqualTo(180L)
        }

        @Test
        @DisplayName("사업부가 Retail사업부 가 아니면 보정 없음")
        fun skipsForOtherDivision() {
            stubRoles(205L to "3영업부", 180L to "Retail사업부_3영업부")
            val e = employee(orgName = "3영업부", jikchak = "부장")
            val other = org(orgNameLevel3 = "제1사업부")
            assertThat(resolver.resolveUserRoleId(e, other, resolver.loadNameIndex()))
                .isEqualTo(205L)
        }

        @Test
        @DisplayName("이름에 영업부 가 없으면 보정 없음 — 지점명은 그대로")
        fun skipsWithoutKeyword() {
            stubRoles(52L to "인천1지점_조장")
            assertThat(resolver.resolveUserRoleId(employee(), org(), resolver.loadNameIndex()))
                .isEqualTo(52L)
        }
    }

    @Nested
    @DisplayName("UserRole 이름 정규화 (SF cls:243-255 — 맵 키에만 적용)")
    inner class NameNormalization {

        @Test
        @DisplayName("마케팅 공백 제거 — DB '마케팅 1부' 가 사원 조직명 '마케팅1부' 와 매칭")
        fun marketingSpace() {
            stubRoles(266L to "마케팅 1부", 267L to "마케팅 1부_1팀")
            val e = employee(orgName = "마케팅1부", jikchak = "사원", costCenterCode = "9999")
            // 마케팅실(5066) 은 접미사 미부여 분기.
            val marketingOrg = org(orgCodeLevel3 = "5066", orgNameLevel3 = "마케팅실")
            assertThat(resolver.resolveUserRoleId(e, marketingOrg, resolver.loadNameIndex()))
                .isEqualTo(266L)
        }

        @Test
        @DisplayName("e-Biz 공백 제거 + 영업본부_ prefix 제거")
        fun ebiz() {
            assertThat(resolver.normalizeRoleName("e-Biz 1팀")).isEqualTo("e-Biz1팀")
            assertThat(resolver.normalizeRoleName("영업본부_e-Biz사업부")).isEqualTo("e-Biz사업부")
            // 'e-Biz ' 가 없으면 공백 제거 대상 아님
            assertThat(resolver.normalizeRoleName("e-Biz영업부 4팀")).isEqualTo("e-Biz영업부 4팀")
        }

        @Test
        @DisplayName("if/else-if 체인 — 이름 하나에 정규화는 최대 하나만 적용")
        fun singleBranchOnly() {
            // '마케팅 ' 미포함이라 어느 분기에도 안 걸림
            assertThat(resolver.normalizeRoleName("FS마케팅1팀")).isEqualTo("FS마케팅1팀")
            assertThat(resolver.normalizeRoleName("마케팅실")).isEqualTo("마케팅실")
            // 판매기획 -> 판매전략 (현재 운영 데이터엔 0건이나 정합 유지)
            assertThat(resolver.normalizeRoleName("판매기획1팀")).isEqualTo("판매전략1팀")
        }

        @Test
        @DisplayName("정규화 키 충돌 — 첫 행 고정 (SF last-wins 비결정성 제거)")
        fun collisionKeepsFirst() {
            stubRoles(10L to "마케팅 1부", 11L to "마케팅1부")
            assertThat(resolver.loadNameIndex()["마케팅1부"]).isEqualTo(10L)
        }
    }

    @Nested
    @DisplayName("특수 조직 prefix (SF cls:367-379)")
    inner class SpecialOrgPrefix {

        @Test
        @DisplayName("유통총괄실(1837) + 코스트센터가 Level5 일치 -> Level4 조직명 prefix")
        fun distributionHq() {
            stubRoles(400L to "유통1팀_주문센터_영업사원")
            val e = employee(orgName = "주문센터", jikchak = "사원", costCenterCode = "1111")
            val hq = org(orgCodeLevel3 = "1837", orgNameLevel4 = "유통1팀", orgCodeLevel5 = "1111")
            assertThat(resolver.resolveUserRoleId(e, hq, resolver.loadNameIndex())).isEqualTo(400L)
        }

        @Test
        @DisplayName("유통총괄실이지만 Level5 불일치 -> prefix 없음")
        fun distributionHqLevelMismatch() {
            stubRoles(401L to "주문센터_영업사원")
            val e = employee(orgName = "주문센터", jikchak = "사원", costCenterCode = "1111")
            val hq = org(orgCodeLevel3 = "1837", orgNameLevel4 = "유통1팀", orgCodeLevel5 = "2222")
            assertThat(resolver.resolveUserRoleId(e, hq, resolver.loadNameIndex())).isEqualTo(401L)
        }

        @Test
        @DisplayName("유통총괄실 + 코스트센터 5638 -> 5638 분기로 넘어가지 않는다 (SF if/else-if 중첩)")
        fun distributionHqDoesNotFallThroughTo5638() {
            val e = employee(orgName = "제품개발팀", jikchak = "사원", costCenterCode = "5638")
            val hq = org(orgCodeLevel3 = "1837", orgNameLevel3 = "제1사업부",
                         orgNameLevel4 = "유통1팀", orgCodeLevel5 = "9999")
            // 1837 분기에 들어갔으나 Level5 불일치 → prefix 없음. `when` fall-through 였다면
            // "제1사업부_제품개발팀_영업사원" 이 되어 어긋난다.
            assertThat(resolver.applyOrgPrefix(e, hq, "제품개발팀_영업사원"))
                .isEqualTo("제품개발팀_영업사원")
        }

        @Test
        @DisplayName("제품개발팀(5638) -> Level3 조직명 prefix")
        fun productDevTeam() {
            stubRoles(402L to "제1사업부_제품개발팀_영업사원")
            val e = employee(orgName = "제품개발팀", jikchak = "사원", costCenterCode = "5638")
            val dev = org(orgNameLevel3 = "제1사업부")
            assertThat(resolver.resolveUserRoleId(e, dev, resolver.loadNameIndex())).isEqualTo(402L)
        }
    }

    @Nested
    @DisplayName("접미사 미부여 조직 (SF ProfileId 분기가 Staff/마케팅으로 빠지는 케이스)")
    inner class NoSuffixOrgs {

        @Test
        @DisplayName("판매전략팀(3472) / BS·SP팀 -> 접미사 없음")
        fun staffOrgs() {
            assertThat(resolver.suffixFor(employee(jikchak = "사원"), org(orgCodeLevel3 = "3472"))).isEmpty()
            assertThat(resolver.suffixFor(employee(jikchak = "사원", costCenterCode = "5397"), org())).isEmpty()
        }

        @Test
        @DisplayName("지원실(3475) — 조장 계열만 접미사, 그 외는 없음")
        fun supportOffice() {
            val support = org(orgCodeLevel3 = "3475")
            assertThat(resolver.suffixFor(employee(jikchak = "사원"), support)).isEmpty()
            assertThat(resolver.suffixFor(employee(jikchak = "판매조장"), support)).isEqualTo("_조장")
        }
    }
}
