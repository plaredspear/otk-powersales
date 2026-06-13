package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.common.service.SystemCodeMasterUpsertService
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertCommand
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertFailedRow
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertResult
import com.otoki.powersales.external.sap.inbound.service.SapSystemCodeMasterService
import com.otoki.powersales.external.sap.inbound.dto.product.SystemCodeMasterRequestItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SapSystemCodeMasterService 어댑터 테스트")
class SapSystemCodeMasterServiceTest {

    private val systemCodeMasterUpsertService: SystemCodeMasterUpsertService = mockk()
    private val service = SapSystemCodeMasterService(systemCodeMasterUpsertService)

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 → ProductMasterDetail")
        fun happy_domainResultMapped() {
            every { systemCodeMasterUpsertService.upsert(any()) } returns
                SystemCodeMasterUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())

            val detail = service.upsert(
                listOf(
                    SystemCodeMasterRequestItem(
                        companyCode = "1000",
                        groupCode = "H10010",
                        detailCode = "10",
                        detailCodeName = "재직"
                    )
                )
            )

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑")
        fun partialFailure_failureRowsMapped() {
            every { systemCodeMasterUpsertService.upsert(any()) } returns
                SystemCodeMasterUpsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(SystemCodeMasterUpsertFailedRow(null, "GroupCode 필수"))
                )

            val detail = service.upsert(
                listOf(SystemCodeMasterRequestItem(companyCode = "1000", groupCode = null, detailCode = "10"))
            )

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("GroupCode 필수")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파 (audit 은 Aspect 책임)")
        fun domainThrow_propagated() {
            every { systemCodeMasterUpsertService.upsert(any()) } throws IllegalStateException("DB connection lost")

            assertThatThrownBy {
                service.upsert(
                    listOf(SystemCodeMasterRequestItem(companyCode = "1000", groupCode = "H10010", detailCode = "10"))
                )
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        @DisplayName("DTO 매핑: SystemCodeMasterRequestItem → SystemCodeMasterUpsertCommand")
        fun dtoMapping_itemToCommand() {
            every { systemCodeMasterUpsertService.upsert(any()) } returns
                SystemCodeMasterUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            val items = listOf(
                SystemCodeMasterRequestItem(
                    companyCode = "1000",
                    groupCode = "H10010",
                    detailCode = "10",
                    groupCodeName = "직원 상태",
                    detailCodeName = "재직",
                    seq = "1"
                )
            )

            service.upsert(items)

            val captor = slot<List<SystemCodeMasterUpsertCommand>>()
            verify { systemCodeMasterUpsertService.upsert(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.companyCode).isEqualTo("1000")
            assertThat(command.groupCode).isEqualTo("H10010")
            assertThat(command.detailCode).isEqualTo("10")
            assertThat(command.groupCodeName).isEqualTo("직원 상태")
            assertThat(command.detailCodeName).isEqualTo("재직")
            assertThat(command.seq).isEqualTo("1")
        }
    }
}
