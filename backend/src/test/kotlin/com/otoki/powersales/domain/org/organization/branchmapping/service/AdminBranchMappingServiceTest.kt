package com.otoki.powersales.domain.org.organization.branchmapping.service

import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.branchmapping.dto.response.BranchMappingType
import com.otoki.powersales.domain.org.organization.branchmapping.entity.BranchMapping
import com.otoki.powersales.domain.org.organization.branchmapping.repository.BranchMappingRepository
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AdminBranchMappingService 테스트")
class AdminBranchMappingServiceTest {

    private val branchMappingRepository: BranchMappingRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val branchCodeExpander: BranchCodeExpander = mockk()

    private val service = AdminBranchMappingService(
        branchMappingRepository,
        organizationRepository,
        branchCodeExpander,
    )

    /**
     * 운영 실측 데이터 기반 fixture — SF `BranchMapping__mdt` 74건 중 유형별 대표 5건.
     * 확장 결과는 `BranchCodeExpander` 가 CSV 를 split + 자기 자신 합집합한 결과와 동일하게 stub 한다.
     */
    private fun stubFixture(
        mappings: List<BranchMapping>,
        expansions: Map<String, Set<String>>,
        orgNames: Map<String, String>,
        cache: Map<String, Set<String>> = expansions,
    ) {
        every { branchMappingRepository.findAllByOrderByBranchCodeAsc() } returns mappings
        every { branchCodeExpander.snapshot() } returns cache
        expansions.forEach { (code, expanded) ->
            every { branchCodeExpander.expand(setOf(code)) } returns expanded
        }
        every { organizationRepository.findOrgNamesByAnyOrgCodeLevel(any()) } returns orgNames
    }

    @Nested
    @DisplayName("유형 판정")
    inner class ClassifyTests {

        @Test
        @DisplayName("확장이 자기 자신뿐 → 없음 (울산2지점 5767)")
        fun classify_none() {
            stubFixture(
                mappings = listOf(BranchMapping("5767", "5767", "울산2지점")),
                expansions = mapOf("5767" to setOf("5767")),
                orgNames = mapOf("5767" to "울산2지점"),
            )

            val item = service.getBranchMappings(null).content.single()

            assertThat(item.type).isEqualTo(BranchMappingType.NONE)
            assertThat(item.typeLabel).isEqualTo("없음")
            assertThat(item.expandedCount).isEqualTo(1)
            assertThat(item.unresolvedCount).isZero()
        }

        @Test
        @DisplayName("자기 자신 미포함 → 롤업 (retail3영업부 5829 → 5826,5827,5828)")
        fun classify_rollup_selfAbsent() {
            stubFixture(
                mappings = listOf(BranchMapping("5829", "5826,5827,5828", "retail3영업부")),
                expansions = mapOf("5829" to setOf("5829", "5826", "5827", "5828")),
                orgNames = mapOf(
                    "5826" to "인천1지점", "5827" to "인천2지점", "5828" to "인천3지점",
                ),
            )

            val item = service.getBranchMappings(null).content.single()

            // expand 는 입력 자신을 항상 합집합에 넣으므로 isSelf 는 존재하나,
            // 자기 자신 외 현행 조직 3건이 있어 롤업으로 판정된다.
            assertThat(item.type).isEqualTo(BranchMappingType.ROLLUP)
            assertThat(item.orgName).isNull()
            assertThat(item.expandedCodes.filter { it.orgName != null }).hasSize(3)
        }

        @Test
        @DisplayName("자기 자신 외 현행 조직 다수 → 롤업 (cvs전략 5694 → 5691~5694)")
        fun classify_rollup_siblings() {
            stubFixture(
                mappings = listOf(BranchMapping("5694", "5691,5692,5693,5694", "cvs전략")),
                expansions = mapOf("5694" to setOf("5691", "5692", "5693", "5694")),
                orgNames = mapOf(
                    "5691" to "CVS1팀", "5692" to "CVS2팀", "5693" to "CVS3팀", "5694" to "CVS전략팀",
                ),
            )

            val item = service.getBranchMappings(null).content.single()

            assertThat(item.type).isEqualTo(BranchMappingType.ROLLUP)
            assertThat(item.typeLabel).isEqualTo("롤업")
            assertThat(item.unresolvedCount).isZero()
            assertThat(item.expandedCodes.single { it.isSelf }.code).isEqualTo("5694")
        }

        @Test
        @DisplayName("E 접두 쌍만 존재 → 이중코드 (E-BIZ1팀 E5706 → 5706,E5706)")
        fun classify_dualCode() {
            stubFixture(
                mappings = listOf(BranchMapping("E5706", "5706,E5706", "E-BIZ1팀")),
                expansions = mapOf("E5706" to setOf("5706", "E5706")),
                orgNames = mapOf("5706" to "E-BIZ1팀"),
            )

            val item = service.getBranchMappings(null).content.single()

            // 5706 이 현행 조직으로 해석되지만 E5706 과 동일 조직의 별칭이므로 롤업이 아니다.
            assertThat(item.type).isEqualTo(BranchMappingType.DUAL_CODE)
            assertThat(item.typeLabel).isEqualTo("이중코드")
        }

        @Test
        @DisplayName("자기 자신 + 미해석 코드 → 이력 (강북1지점 5815 → 5452,5815)")
        fun classify_legacy() {
            stubFixture(
                mappings = listOf(BranchMapping("5815", "5452,5815", "강북1지점")),
                expansions = mapOf("5815" to setOf("5452", "5815")),
                // 5452 는 조직 개편으로 폐기되어 현행 Organization 에 없다.
                orgNames = mapOf("5815" to "강북1지점"),
            )

            val item = service.getBranchMappings(null).content.single()

            assertThat(item.type).isEqualTo(BranchMappingType.LEGACY)
            assertThat(item.typeLabel).isEqualTo("이력")
            assertThat(item.unresolvedCount).isEqualTo(1)
            assertThat(item.expandedCodes.single { it.code == "5452" }.orgName).isNull()
        }
    }

    @Nested
    @DisplayName("검색")
    inner class SearchTests {

        private fun stubTwoRows() = stubFixture(
            mappings = listOf(
                BranchMapping("5829", "5826,5827,5828", "retail3영업부"),
                BranchMapping("5815", "5452,5815", "강북1지점"),
            ),
            expansions = mapOf(
                "5829" to setOf("5829", "5826", "5827", "5828"),
                "5815" to setOf("5452", "5815"),
            ),
            orgNames = mapOf(
                "5826" to "인천1지점", "5827" to "인천2지점", "5828" to "인천3지점",
                "5815" to "강북1지점",
            ),
        )

        @Test
        @DisplayName("지점코드 정방향 검색")
        fun search_byBranchCode() {
            stubTwoRows()

            val result = service.getBranchMappings("5829")

            assertThat(result.content).hasSize(1)
            assertThat(result.content.single().branchCode).isEqualTo("5829")
        }

        @Test
        @DisplayName("확장 코드 역방향 검색 — 그 코드를 품은 매핑 행이 잡힌다")
        fun search_byExpandedCode_reverse() {
            stubTwoRows()

            // 5826(인천1지점) 은 어느 행의 branch_code 도 아니고 retail3 의 확장 코드로만 등장한다.
            val result = service.getBranchMappings("5826")

            assertThat(result.content).hasSize(1)
            assertThat(result.content.single().branchCode).isEqualTo("5829")
        }

        @Test
        @DisplayName("조직명으로도 검색된다")
        fun search_byOrgName() {
            stubTwoRows()

            val result = service.getBranchMappings("인천2")

            assertThat(result.content.single().branchCode).isEqualTo("5829")
        }

        @Test
        @DisplayName("검색어 없으면 전건 + 유형별 집계")
        fun search_blank_returnsAllWithCounts() {
            stubTwoRows()

            val result = service.getBranchMappings(null)

            assertThat(result.content).hasSize(2)
            assertThat(result.typeCounts[BranchMappingType.ROLLUP.name]).isEqualTo(1)
            assertThat(result.typeCounts[BranchMappingType.LEGACY.name]).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("캐시 stale 감지")
    inner class CacheTests {

        @Test
        @DisplayName("DB 행은 있는데 런타임 캐시가 비어 있으면 cacheEmpty = true")
        fun cacheEmpty_whenStale() {
            stubFixture(
                mappings = listOf(BranchMapping("5767", "5767", "울산2지점")),
                expansions = mapOf("5767" to setOf("5767")),
                orgNames = mapOf("5767" to "울산2지점"),
                cache = emptyMap(),
            )

            assertThat(service.getBranchMappings(null).cacheEmpty).isTrue()
        }

        @Test
        @DisplayName("캐시가 채워져 있으면 cacheEmpty = false")
        fun cacheEmpty_whenLoaded() {
            stubFixture(
                mappings = listOf(BranchMapping("5767", "5767", "울산2지점")),
                expansions = mapOf("5767" to setOf("5767")),
                orgNames = mapOf("5767" to "울산2지점"),
            )

            assertThat(service.getBranchMappings(null).cacheEmpty).isFalse()
        }
    }
}
