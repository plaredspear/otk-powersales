package com.otoki.powersales.domain.org.organization.repository

import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * [OrganizationRepositoryCustom.findOrgNamesByAnyOrgCodeLevel] 검증 — 지점 코드 맵핑 화면의 조직명 해석.
 *
 * QueryDSL Tuple 인덱스로 `(org_cd, org_nm)` 쌍을 훑는 구현이라 인덱스가 어긋나면 이름이 밀려 붙는다.
 * 레벨별 매칭 / 미매칭 코드 누락 / 깊은 레벨 우선을 실제 쿼리로 고정한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("Organization 조직코드 → 조직명 일괄 해석")
class OrganizationOrgNameLookupTest {

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @BeforeEach
    fun setUp() {
        organizationRepository.deleteAll()
        organizationRepository.save(
            Organization(
                orgCodeLevel2 = "1000", orgNameLevel2 = "영업본부",
                orgCodeLevel3 = "5800", orgNameLevel3 = "Retail사업부",
                orgCodeLevel4 = "5814", orgNameLevel4 = "1영업부",
                orgCodeLevel5 = "5815", orgNameLevel5 = "강북1지점",
            )
        )
        organizationRepository.save(
            Organization(
                orgCodeLevel2 = "1000", orgNameLevel2 = "영업본부",
                orgCodeLevel3 = "5690", orgNameLevel3 = "CVS사업부",
                orgCodeLevel4 = "5694", orgNameLevel4 = "CVS전략팀",
                // Level5 부재 조직 (영업지원2팀·CVS전략팀 계열) — Level4 로 해석되어야 한다.
                orgCodeLevel5 = null, orgNameLevel5 = null,
            )
        )
    }

    @Test
    @DisplayName("Level5 / Level4 / Level3 코드가 각각 해당 레벨 이름으로 해석된다")
    fun resolvesEachLevel() {
        val result = organizationRepository.findOrgNamesByAnyOrgCodeLevel(
            listOf("5815", "5814", "5800", "5694")
        )

        assertThat(result["5815"]).isEqualTo("강북1지점")
        assertThat(result["5814"]).isEqualTo("1영업부")
        assertThat(result["5800"]).isEqualTo("Retail사업부")
        // Level5 가 없는 조직은 Level4 이름으로 해석.
        assertThat(result["5694"]).isEqualTo("CVS전략팀")
    }

    @Test
    @DisplayName("현행 조직에 없는 코드는 결과에 키 자체가 없다 (= 폐기된 이력 코드 판별 근거)")
    fun omitsUnknownCodes() {
        val result = organizationRepository.findOrgNamesByAnyOrgCodeLevel(listOf("5815", "5452"))

        assertThat(result).containsKey("5815")
        // 5452 는 2025-05-01 조직 개편으로 폐기된 코드 — 매칭되지 않아야 한다.
        assertThat(result).doesNotContainKey("5452")
    }

    @Test
    @DisplayName("빈 입력 / 공백 코드는 쿼리 없이 빈 결과")
    fun emptyInput() {
        assertThat(organizationRepository.findOrgNamesByAnyOrgCodeLevel(emptyList())).isEmpty()
        assertThat(organizationRepository.findOrgNamesByAnyOrgCodeLevel(listOf("", "  "))).isEmpty()
    }

    @Test
    @DisplayName("soft-delete 조직은 제외된다")
    fun excludesDeleted() {
        organizationRepository.save(
            Organization(
                orgCodeLevel4 = "9999", orgNameLevel4 = "폐지된팀",
            ).apply { isDeleted = true }
        )

        val result = organizationRepository.findOrgNamesByAnyOrgCodeLevel(listOf("9999"))

        assertThat(result).doesNotContainKey("9999")
    }
}
