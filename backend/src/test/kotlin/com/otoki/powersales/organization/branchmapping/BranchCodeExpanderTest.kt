package com.otoki.powersales.organization.branchmapping

import com.otoki.powersales.organization.branchmapping.entity.BranchMapping
import com.otoki.powersales.organization.branchmapping.repository.BranchMappingRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BranchCodeExpander")
class BranchCodeExpanderTest {

    private lateinit var repository: BranchMappingRepository
    private lateinit var expander: BranchCodeExpander

    @BeforeEach
    fun setUp() {
        repository = mockk()
        every { repository.findAll() } returns listOf(
            BranchMapping(branchCode = "5849", includedBranchCodes = "5479,5849", label = "부산1지점"),
            BranchMapping(branchCode = "5694", includedBranchCodes = "5691,5692,5693,5694", label = "cvs전략"),
            // KAM1 — 공백 + 중복 원본
            BranchMapping(branchCode = "5721", includedBranchCodes = "5721,E5721, 5466, 5693,5721,5466", label = "KAM1부장"),
            // 이력 없는 단일값
            BranchMapping(branchCode = "4148", includedBranchCodes = "4148", label = "진주2지점"),
        )
        expander = BranchCodeExpander(repository)
        expander.init()
    }

    @Test
    @DisplayName("매칭 BranchMapping 있을 때 — 자기 자신 + included 합집합")
    fun expandWithMatch() {
        val result = expander.expand(listOf("5849"))
        assertThat(result).containsExactlyInAnyOrder("5849", "5479")
    }

    @Test
    @DisplayName("매칭 BranchMapping 없을 때 — pass-through (자기 자신만)")
    fun expandWithoutMatch() {
        val result = expander.expand(listOf("9999"))
        assertThat(result).containsExactly("9999")
    }

    @Test
    @DisplayName("빈 입력 → 빈 Set")
    fun expandEmpty() {
        assertThat(expander.expand(emptyList())).isEmpty()
    }

    @Test
    @DisplayName("다중 입력 중 일부만 매칭")
    fun expandPartialMatch() {
        val result = expander.expand(listOf("5849", "9999", "5694"))
        assertThat(result).containsExactlyInAnyOrder(
            "5849", "5479",                       // 5849 → 5479,5849
            "9999",                                // pass-through
            "5694", "5691", "5692", "5693",       // 5694 → 5691,5692,5693,5694
        )
    }

    @Test
    @DisplayName("공백 + 중복 IBC (KAM1) — trim + Set normalize")
    fun expandKam1WithWhitespaceAndDuplicates() {
        val result = expander.expand(listOf("5721"))
        // "5721,E5721, 5466, 5693,5721,5466" → split + trim + Set
        assertThat(result).containsExactlyInAnyOrder("5721", "E5721", "5466", "5693")
    }

    @Test
    @DisplayName("이력 없는 단일값 (4148)")
    fun expandSingleValue() {
        val result = expander.expand(listOf("4148"))
        assertThat(result).containsExactly("4148")
    }

    @Test
    @DisplayName("reload — 빈 DB 부팅 후 Stage1 적재 시뮬레이션 (stale 캐시 갱신)")
    fun reloadAfterEmptyBoot() {
        // 빈 DB 로 부팅 (Stage1 적재 전) — 캐시 빈 상태
        val emptyRepo = mockk<BranchMappingRepository>()
        every { emptyRepo.findAll() } returns emptyList()
        val freshExpander = BranchCodeExpander(emptyRepo)
        freshExpander.init()
        assertThat(freshExpander.expand(listOf("5849"))).containsExactly("5849") // pass-through (캐시 비어 있음)

        // Stage1 적재 후 데이터가 채워진 상태로 reload
        every { emptyRepo.findAll() } returns listOf(
            BranchMapping(branchCode = "5849", includedBranchCodes = "5479,5849", label = "부산1지점"),
        )
        freshExpander.reload()
        assertThat(freshExpander.expand(listOf("5849"))).containsExactlyInAnyOrder("5849", "5479")
    }

    @Test
    @DisplayName("splitIncluded — 빈 토큰 / 공백 제거")
    fun splitIncluded() {
        assertThat(BranchCodeExpander.splitIncluded("a,b,c"))
            .containsExactlyInAnyOrder("a", "b", "c")
        assertThat(BranchCodeExpander.splitIncluded("a, b , c"))
            .containsExactlyInAnyOrder("a", "b", "c")
        assertThat(BranchCodeExpander.splitIncluded("a,,b"))
            .containsExactlyInAnyOrder("a", "b")
    }
}
