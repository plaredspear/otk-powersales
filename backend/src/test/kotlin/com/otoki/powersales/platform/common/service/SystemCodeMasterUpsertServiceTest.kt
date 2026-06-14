package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.platform.common.service.SystemCodeMasterUpsertService
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertCommand
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SystemCodeMasterUpsertService 테스트")
class SystemCodeMasterUpsertServiceTest {

    private val systemCodeMasterRepository: SystemCodeMasterRepository = mockk()

    private val service = SystemCodeMasterUpsertService(
        systemCodeMasterRepository,
    )

    private fun stubSaveAllCapture(): CapturingSlot<List<SystemCodeMaster>> {
        val slot = slot<List<SystemCodeMaster>>()
        every { systemCodeMasterRepository.saveAll(capture(slot)) } answers { firstArg<List<SystemCodeMaster>>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 - INSERT, externalKey = company;group;detail")
        fun upsert_insertNew() {
            every { systemCodeMasterRepository.findByExternalKey("1000;H10010;10") } returns null
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(
                listOf(
                    SystemCodeMasterUpsertCommand(
                        companyCode = "1000",
                        groupCode = "H10010",
                        detailCode = "10",
                        groupCodeName = "직원 상태",
                        detailCodeName = "재직",
                        seq = "1"
                    )
                )
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.externalKey).isEqualTo("1000;H10010;10")
            assertThat(saved.companyCode).isEqualTo("1000")
            assertThat(saved.groupCode).isEqualTo("H10010")
            assertThat(saved.detailCode).isEqualTo("10")
            assertThat(saved.detailCodeName).isEqualTo("재직")
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("기존 갱신 - DetailCodeName 변경")
        fun upsert_updateExisting() {
            val existing = SystemCodeMaster(
                companyCode = "1000",
                groupCode = "H10010",
                detailCode = "10",
                externalKey = "1000;H10010;10",
                detailCodeName = "기존이름"
            )
            every { systemCodeMasterRepository.findByExternalKey("1000;H10010;10") } returns existing
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    SystemCodeMasterUpsertCommand(
                        companyCode = "1000",
                        groupCode = "H10010",
                        detailCode = "10",
                        groupCodeName = null,
                        detailCodeName = "신규이름",
                        seq = null
                    )
                )
            )

            val saved = savedSlot.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.detailCodeName).isEqualTo("신규이름")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("CompanyCode 누락 - failures, identifier null")
        fun upsert_missingCompanyCode() {
            val result = service.upsert(
                listOf(
                    SystemCodeMasterUpsertCommand(
                        companyCode = null,
                        groupCode = "H10010",
                        detailCode = "10",
                        groupCodeName = null,
                        detailCodeName = null,
                        seq = null
                    )
                )
            )

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("CompanyCode 필수")
            verify(exactly = 0) { systemCodeMasterRepository.saveAll(any<List<SystemCodeMaster>>()) }
        }

        @Test
        @DisplayName("GroupCode 누락 - failures")
        fun upsert_missingGroupCode() {
            val result = service.upsert(
                listOf(
                    SystemCodeMasterUpsertCommand(
                        companyCode = "1000",
                        groupCode = null,
                        detailCode = "10",
                        groupCodeName = null,
                        detailCodeName = null,
                        seq = null
                    )
                )
            )

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("GroupCode 필수")
        }

        @Test
        @DisplayName("DetailCode 누락 - failures")
        fun upsert_missingDetailCode() {
            val result = service.upsert(
                listOf(
                    SystemCodeMasterUpsertCommand(
                        companyCode = "1000",
                        groupCode = "H10010",
                        detailCode = null,
                        groupCodeName = null,
                        detailCodeName = null,
                        seq = null
                    )
                )
            )

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("DetailCode 필수")
        }
    }
}
