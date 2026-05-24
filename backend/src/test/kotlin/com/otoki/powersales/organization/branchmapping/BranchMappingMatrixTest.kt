package com.otoki.powersales.organization.branchmapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BranchMappingMatrix SoT")
class BranchMappingMatrixTest {

    @Test
    @DisplayName("frozen snapshot 인스턴스 수 = 74")
    fun snapshotCount() {
        assertThat(BranchMappingMatrix.ALL).hasSize(74)
    }

    @Test
    @DisplayName("branchCode 유일성")
    fun branchCodeUnique() {
        val codes = BranchMappingMatrix.ALL.map { it.branchCode }
        assertThat(codes).hasSameSizeAs(codes.toSet())
    }

    @Test
    @DisplayName("모든 includedBranchCodes 가 split 시 1개 이상 + 비어있지 않음")
    fun includedBranchCodesValid() {
        for (entry in BranchMappingMatrix.ALL) {
            val tokens = BranchCodeExpander.splitIncluded(entry.includedBranchCodes)
            assertThat(tokens)
                .withFailMessage("Entry %s 의 IBC '%s' 가 비어있음", entry.branchCode, entry.includedBranchCodes)
                .isNotEmpty
        }
    }

    @Test
    @DisplayName("모든 label 비어있지 않음")
    fun labelNotBlank() {
        for (entry in BranchMappingMatrix.ALL) {
            assertThat(entry.label).isNotBlank()
        }
    }

    @Test
    @DisplayName("샘플 데이터 정합 — SF 원본과 1:1")
    fun sampleEntriesMatchSfSource() {
        val byBc = BranchMappingMatrix.ALL.associateBy { it.branchCode }
        // SF customMetadata 의 대표 샘플 5건 검증
        assertThat(byBc["5849"]!!.includedBranchCodes).isEqualTo("5479,5849")
        assertThat(byBc["5849"]!!.label).isEqualTo("부산1지점")
        assertThat(byBc["5749"]!!.includedBranchCodes).isEqualTo("312,5749")
        assertThat(byBc["4148"]!!.includedBranchCodes).isEqualTo("4148") // 이력 없는 단일값
        assertThat(byBc["5694"]!!.includedBranchCodes).isEqualTo("5691,5692,5693,5694") // 그룹 통합 조회
        assertThat(byBc["5721"]!!.includedBranchCodes).isEqualTo("5721,E5721, 5466, 5693,5721,5466") // 공백+중복 원본 보존
    }
}
