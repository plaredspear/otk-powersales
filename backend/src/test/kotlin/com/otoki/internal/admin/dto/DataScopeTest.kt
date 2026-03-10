package com.otoki.internal.admin.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("DataScope 테스트")
class DataScopeTest {

    @Nested
    @DisplayName("effectiveBranchCodes")
    inner class EffectiveBranchCodes {

        @Test
        @DisplayName("전체 조회 권한 + 요청 코드 없음 - All 반환")
        fun allBranchesWithNoRequest() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val result = scope.effectiveBranchCodes(null)

            assertThat(result).isEqualTo(EffectiveBranchResult.All)
        }

        @Test
        @DisplayName("전체 조회 권한 + 특정 코드 요청 - 해당 코드로 필터링")
        fun allBranchesWithSpecificRequest() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val result = scope.effectiveBranchCodes("B001")

            assertThat(result).isEqualTo(EffectiveBranchResult.Filtered(listOf("B001")))
        }

        @Test
        @DisplayName("제한 권한 + 허용된 코드 요청 - 해당 코드로 필터링")
        fun limitedBranchesWithAllowedRequest() {
            val scope = DataScope(branchCodes = listOf("B001", "B002"), isAllBranches = false)

            val result = scope.effectiveBranchCodes("B001")

            assertThat(result).isEqualTo(EffectiveBranchResult.Filtered(listOf("B001")))
        }

        @Test
        @DisplayName("제한 권한 + 비허용 코드 요청 - NoAccess 반환")
        fun limitedBranchesWithDeniedRequest() {
            val scope = DataScope(branchCodes = listOf("B001"), isAllBranches = false)

            val result = scope.effectiveBranchCodes("B999")

            assertThat(result).isEqualTo(EffectiveBranchResult.NoAccess)
        }

        @Test
        @DisplayName("제한 권한 + 요청 코드 없음 - 전체 허용 코드로 필터링")
        fun limitedBranchesWithNoRequest() {
            val scope = DataScope(branchCodes = listOf("B001", "B002"), isAllBranches = false)

            val result = scope.effectiveBranchCodes(null)

            assertThat(result).isEqualTo(EffectiveBranchResult.Filtered(listOf("B001", "B002")))
        }

        @Test
        @DisplayName("빈 허용 목록 + 요청 코드 없음 - NoAccess 반환")
        fun emptyBranchesWithNoRequest() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)

            val result = scope.effectiveBranchCodes(null)

            assertThat(result).isEqualTo(EffectiveBranchResult.NoAccess)
        }
    }

    @Nested
    @DisplayName("validateAccess")
    inner class ValidateAccess {

        @Test
        @DisplayName("전체 조회 권한 - 항상 true")
        fun allBranchesAlwaysTrue() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            assertThat(scope.validateAccess("B001")).isTrue()
        }

        @Test
        @DisplayName("허용 목록에 포함된 코드 - true")
        fun allowedCodeReturnsTrue() {
            val scope = DataScope(branchCodes = listOf("B001"), isAllBranches = false)

            assertThat(scope.validateAccess("B001")).isTrue()
        }

        @Test
        @DisplayName("허용 목록에 없는 코드 - false")
        fun deniedCodeReturnsFalse() {
            val scope = DataScope(branchCodes = listOf("B001"), isAllBranches = false)

            assertThat(scope.validateAccess("B999")).isFalse()
        }

        @Test
        @DisplayName("null 코드 - false")
        fun nullCodeReturnsFalse() {
            val scope = DataScope(branchCodes = listOf("B001"), isAllBranches = false)

            assertThat(scope.validateAccess(null)).isFalse()
        }
    }
}
