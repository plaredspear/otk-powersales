package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertCommand
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("SystemCodeMasterUpsertService 테스트")
class SystemCodeMasterUpsertServiceTest {

    private val systemCodeMasterRepository: SystemCodeMasterRepository = mockk()

    // 행 단위 트랜잭션 빈 — 테스트에서는 실제 빈을 mock repository 로 구성 (REQUIRES_NEW 는 단위 테스트 무관).
    private val rowUpsertService = SystemCodeMasterRowUpsertService(systemCodeMasterRepository)

    private val service = SystemCodeMasterUpsertService(rowUpsertService)

    private fun command(
        companyCode: String? = "1000",
        groupCode: String? = "H10010",
        detailCode: String? = "10",
        groupCodeName: String? = null,
        detailCodeName: String? = null,
        seq: String? = null
    ) = SystemCodeMasterUpsertCommand(
        companyCode = companyCode,
        groupCode = groupCode,
        detailCode = detailCode,
        groupCodeName = groupCodeName,
        detailCodeName = detailCodeName,
        seq = seq
    )

    private fun stubSaveAndFlushCapture(): CapturingSlot<SystemCodeMaster> {
        val slot = slot<SystemCodeMaster>()
        every { systemCodeMasterRepository.saveAndFlush(capture(slot)) } answers { firstArg<SystemCodeMaster>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 - INSERT, externalKey = company;group;detail")
        fun upsert_insertNew() {
            every { systemCodeMasterRepository.findByExternalKey("1000;H10010;10") } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(command(detailCodeName = "재직", seq = "1")))

            val saved = savedSlot.captured
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
            val savedSlot = stubSaveAndFlushCapture()

            service.upsert(listOf(command(detailCodeName = "신규이름")))

            val saved = savedSlot.captured
            assertThat(saved).isSameAs(existing)
            assertThat(saved.detailCodeName).isEqualTo("신규이름")
        }
    }

    @Nested
    @DisplayName("upsert - 레거시 정합 (세 코드 필수 검증 제거 — nillable raw 적재)")
    inner class UpsertLegacyAlignment {

        @Test
        @DisplayName("CompanyCode 누락 - 검증 없이 raw 적재, externalKey 는 빈 회사코드로 합성")
        fun upsert_missingCompanyCode_rawStored() {
            // externalKey = ";H10010;10"
            every { systemCodeMasterRepository.findByExternalKey(";H10010;10") } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(command(companyCode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.externalKey).isEqualTo(";H10010;10")
            assertThat(savedSlot.captured.companyCode).isNull()
        }

        @Test
        @DisplayName("세 코드 모두 누락 - externalKey = ';;' 로 적재 (레거시 빈 키 동등)")
        fun upsert_allCodesMissing_rawStored() {
            every { systemCodeMasterRepository.findByExternalKey(";;") } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(command(companyCode = null, groupCode = null, detailCode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(savedSlot.captured.externalKey).isEqualTo(";;")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path (external_key UNIQUE 충돌 행 격리)")
    inner class UpsertError {

        @Test
        @DisplayName("external_key UNIQUE 충돌 - 그 행만 failures, 트랜잭션 전체 롤백 안 함")
        fun upsert_uniqueViolation_isolatedAsFailure() {
            every { systemCodeMasterRepository.findByExternalKey(any()) } returns null
            every { systemCodeMasterRepository.saveAndFlush(match { it.externalKey == "1000;H10010;10" }) } answers { firstArg() }
            every { systemCodeMasterRepository.saveAndFlush(match { it.externalKey == "1000;H10010;20" }) } throws
                DataIntegrityViolationException("duplicate key value violates unique constraint")

            val result = service.upsert(
                listOf(
                    command(detailCode = "10"),
                    command(detailCode = "20")
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("1000;H10010;20")
            assertThat(result.failures.single().reason).contains("적재 실패")
        }
    }
}
