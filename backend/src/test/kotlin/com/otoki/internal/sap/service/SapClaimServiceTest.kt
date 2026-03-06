package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapClaimRequest
import com.otoki.internal.sap.entity.TmpClaim
import com.otoki.internal.sap.repository.TmpClaimRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapClaimService 테스트")
class SapClaimServiceTest {

    @Mock
    private lateinit var tmpClaimRepository: TmpClaimRepository

    @InjectMocks
    private lateinit var sapClaimService: SapClaimService

    @Nested
    @DisplayName("syncClaim")
    inner class SyncClaimTests {

        @Test
        @DisplayName("성공 - 기존 클레임 필드 업데이트")
        fun `should update existing claim and return success`() {
            // given
            val item = SapClaimRequest.ClaimItem(
                name = "CLM-001",
                claimSequence = "001",
                actionCode = "A",
                claimStatus = "OPEN",
                content = "불량 신고",
                reasonType = "DEFECT",
                cosmosKey = "COS-001"
            )
            val existing = TmpClaim(id = 1).apply {
                claimName = "CLM-001"
            }
            whenever(tmpClaimRepository.findByClaimName("CLM-001")).thenReturn(existing)
            whenever(tmpClaimRepository.save(any<TmpClaim>()))
                .thenAnswer { it.getArgument<TmpClaim>(0) }

            // when
            val response = sapClaimService.syncClaim(item)

            // then
            assertThat(response.resultCode).isEqualTo("S")
            assertThat(response.resultMsg).isEqualTo("성공")
            verify(tmpClaimRepository).save(existing)
            assertThat(existing.claimSequence).isEqualTo("001")
            assertThat(existing.actionCode).isEqualTo("A")
            assertThat(existing.claimStatus).isEqualTo("OPEN")
            assertThat(existing.claimContent).isEqualTo("불량 신고")
            assertThat(existing.reasonType).isEqualTo("DEFECT")
            assertThat(existing.cosmosKey).isEqualTo("COS-001")
            assertThat(existing.updDate).isNotNull()
        }

        @Test
        @DisplayName("클레임 미존재 - error 반환")
        fun `should return error when claim not found`() {
            // given
            val item = SapClaimRequest.ClaimItem(
                name = "CLM-999",
                claimSequence = "001"
            )
            whenever(tmpClaimRepository.findByClaimName("CLM-999")).thenReturn(null)

            // when
            val response = sapClaimService.syncClaim(item)

            // then
            assertThat(response.resultCode).isEqualTo("E")
            assertThat(response.resultMsg).contains("CLM-999")
        }

        @Test
        @DisplayName("name null - error 반환")
        fun `should return error when name is null`() {
            // given
            val item = SapClaimRequest.ClaimItem(
                name = null,
                claimSequence = "001"
            )

            // when
            val response = sapClaimService.syncClaim(item)

            // then
            assertThat(response.resultCode).isEqualTo("E")
            assertThat(response.resultMsg).contains("name")
        }
    }
}
