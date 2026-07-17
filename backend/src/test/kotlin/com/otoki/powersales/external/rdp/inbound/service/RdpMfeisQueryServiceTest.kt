package com.otoki.powersales.external.rdp.inbound.service

import com.otoki.powersales.domain.activity.schedule.repository.MfeisSnapshotRow
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAudit
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditEventType
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditService
import com.otoki.powersales.external.rdp.auth.exception.RdpInvalidParameterException
import com.otoki.powersales.external.rdp.inbound.config.RdpInboundProperties
import com.otoki.powersales.external.rdp.inbound.dto.MfeisSearchRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RdpMfeisQueryService 테스트")
class RdpMfeisQueryServiceTest {

    private val repository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk()
    private val auditService: RdpInboundAuditService = mockk(relaxed = true)

    private fun service(
        defaultPageSize: Int = 100,
        maxPageSize: Int = 1000,
    ): RdpMfeisQueryService {
        val props = RdpInboundProperties(
            mfeis = RdpInboundProperties.Mfeis(
                defaultPageSize = defaultPageSize,
                maxPageSize = maxPageSize,
            )
        )
        return RdpMfeisQueryService(repository, props, auditService)
    }

    private fun row(id: Long): MfeisSnapshotRow = MfeisSnapshotRow(
        id = id, sfid = null, externalKey = "EK-$id", year = "2026", month = "07",
        costCenterCode = null, orgName = null, employeeCode = null, employeeName = null,
        title = null, accountCode = null, accountName = null, accountBranchName = null,
        accountType = null, abcType = null, workingCategory1 = null, workingCategory3 = null,
        workingCategory4 = null, workingCategory5 = null, numberOfInputs = null,
        equivalentNumberOfWorkingDays = null, convertedHeadcount = null,
    )

    private fun req(
        year: String? = "2026",
        month: String? = "07",
        cursor: Long? = null,
        size: Int? = null,
    ) = MfeisSearchRequest(year = year, month = month, cursor = cursor, size = size)

    @Nested
    @DisplayName("필수 파라미터 검증")
    inner class RequiredParams {

        @Test
        @DisplayName("year 누락 -> RdpInvalidParameterException")
        fun missingYear() {
            assertThatThrownBy { service().search(req(year = null), "c", "127.0.0.1") }
                .isInstanceOf(RdpInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("year 공백 -> RdpInvalidParameterException")
        fun blankYear() {
            assertThatThrownBy { service().search(req(year = "  "), "c", "127.0.0.1") }
                .isInstanceOf(RdpInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("month 누락 -> RdpInvalidParameterException")
        fun missingMonth() {
            assertThatThrownBy { service().search(req(month = null), "c", "127.0.0.1") }
                .isInstanceOf(RdpInvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("size clamp")
    inner class SizeClamp {

        @Test
        @DisplayName("size 미지정 -> 기본값으로 (limit = default + 1) 조회")
        fun defaultSize() {
            val limitSlot = slot<Int>()
            every { repository.findSnapshotByKeyset(any(), any(), any(), capture(limitSlot)) } returns emptyList()

            service(defaultPageSize = 100).search(req(size = null), "c", "127.0.0.1")

            assertThat(limitSlot.captured).isEqualTo(101)
        }

        @Test
        @DisplayName("size 가 max 초과 -> max 로 clamp (limit = max + 1)")
        fun clampToMax() {
            val limitSlot = slot<Int>()
            every { repository.findSnapshotByKeyset(any(), any(), any(), capture(limitSlot)) } returns emptyList()

            service(maxPageSize = 1000).search(req(size = 5000), "c", "127.0.0.1")

            assertThat(limitSlot.captured).isEqualTo(1001)
        }

        @Test
        @DisplayName("size 가 0 이하 -> 기본값 적용")
        fun nonPositiveSize() {
            val limitSlot = slot<Int>()
            every { repository.findSnapshotByKeyset(any(), any(), any(), capture(limitSlot)) } returns emptyList()

            service(defaultPageSize = 100).search(req(size = 0), "c", "127.0.0.1")

            assertThat(limitSlot.captured).isEqualTo(101)
        }
    }

    @Nested
    @DisplayName("keyset 페이지 판정")
    inner class Keyset {

        @Test
        @DisplayName("size+1 건 조회됨 -> hasNext=true, 마지막 1건 버림, nextCursor=마지막 노출 row id")
        fun hasNextTrue() {
            // pageSize=2 요청 → limit=3. repo 가 3건 반환하면 hasNext=true, items 2건.
            every { repository.findSnapshotByKeyset("2026", "07", null, 3) } returns
                listOf(row(10), row(11), row(12))

            val res = service().search(req(size = 2), "c", "127.0.0.1")

            assertThat(res.hasNext).isTrue()
            assertThat(res.items).hasSize(2)
            assertThat(res.items.map { it.id }).containsExactly(10L, 11L)
            assertThat(res.nextCursor).isEqualTo(11L)
        }

        @Test
        @DisplayName("size 이하 조회됨 -> hasNext=false, nextCursor=null, 전건 노출")
        fun hasNextFalse() {
            every { repository.findSnapshotByKeyset("2026", "07", null, 3) } returns
                listOf(row(10), row(11))

            val res = service().search(req(size = 2), "c", "127.0.0.1")

            assertThat(res.hasNext).isFalse()
            assertThat(res.items).hasSize(2)
            assertThat(res.nextCursor).isNull()
        }

        @Test
        @DisplayName("빈 결과 -> hasNext=false, items 빈 목록, nextCursor=null")
        fun empty() {
            every { repository.findSnapshotByKeyset("2026", "07", 999, 101) } returns emptyList()

            val res = service().search(req(cursor = 999), "c", "127.0.0.1")

            assertThat(res.items).isEmpty()
            assertThat(res.hasNext).isFalse()
            assertThat(res.nextCursor).isNull()
        }

        @Test
        @DisplayName("cursor 가 repository 로 그대로 전달됨")
        fun cursorPassthrough() {
            val cursorSlot = slot<Long>()
            every {
                repository.findSnapshotByKeyset(any(), any(), capture(cursorSlot), any())
            } returns emptyList()

            service().search(req(cursor = 12345), "c", "127.0.0.1")

            assertThat(cursorSlot.captured).isEqualTo(12345L)
        }
    }

    @Nested
    @DisplayName("audit")
    inner class Audit {

        @Test
        @DisplayName("정상 조회 시 REQUEST_ACCEPTED 적재 + received_count = 노출 건수")
        fun auditAccepted() {
            every { repository.findSnapshotByKeyset("2026", "07", null, 3) } returns
                listOf(row(10), row(11), row(12))
            val auditSlot = slot<RdpInboundAudit>()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            service().search(req(size = 2), "cli-1", "10.0.0.1")

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(RdpInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditSlot.captured.clientId).isEqualTo("cli-1")
            assertThat(auditSlot.captured.receivedCount).isEqualTo(2)
        }
    }
}
