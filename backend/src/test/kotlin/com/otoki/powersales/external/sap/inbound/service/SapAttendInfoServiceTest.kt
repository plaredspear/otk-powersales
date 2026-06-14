package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.inbound.dto.attendance.AttendInfoRequestItem
import com.otoki.powersales.external.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.external.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.external.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import com.otoki.powersales.domain.activity.schedule.service.AttendInfoInsertService
import com.otoki.powersales.domain.activity.schedule.service.dto.AttendInfoInsertCommand
import com.otoki.powersales.domain.activity.schedule.service.dto.AttendInfoInsertFailedRow
import com.otoki.powersales.domain.activity.schedule.service.dto.AttendInfoInsertResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SapAttendInfoService 어댑터 테스트")
class SapAttendInfoServiceTest {

    private val attendInfoInsertService: AttendInfoInsertService = mockk()
    private val auditService: SapInboundAuditService = mockk()
    private val scheduleConverter: AttendInfoToScheduleConverter = mockk()

    private lateinit var service: SapAttendInfoService

    @BeforeEach
    fun setUp() {
        every { scheduleConverter.convert(any<List<AttendInfo>>()) } returns ScheduleConversionSummary.ZERO
        every { auditService.record(any()) } answers { firstArg<SapInboundAudit>() }
        val helper = ChunkedUpsertHelper()
        service = SapAttendInfoService(
            attendInfoInsertService = attendInfoInsertService,
            chunkedUpsertHelper = helper,
            auditService = auditService,
            scheduleConverter = scheduleConverter,
            chunkSize = 3,
            maxRows = 100
        )
    }

    private fun item(
        employeeCode: String? = "100123",
        startDate: String? = "20260427",
        endDate: String? = "20260427",
        attendType: String? = "14",
        status: String? = "정상"
    ): AttendInfoRequestItem = AttendInfoRequestItem(
        employeeCode = employeeCode,
        startDate = startDate,
        endDate = endDate,
        attendType = attendType,
        status = status
    )

    private fun savedAttend(employeeCode: String = "100123", startDate: String = "20260427") =
        AttendInfo(
            id = 1L,
            employeeCode = employeeCode,
            startDate = startDate,
            endDate = startDate,
            attendType = "14",
            status = "정상"
        )

    private fun mockDomainSuccess(saved: List<AttendInfo>) {
        every { attendInfoInsertService.insert(any()) } answers {
            val cmds = firstArg<List<AttendInfoInsertCommand>>()
            AttendInfoInsertResult(
                successCount = cmds.size,
                failureCount = 0,
                failures = emptyList(),
                savedAttendInfos = saved
            )
        }
    }

    @Nested
    @DisplayName("insert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 단일 청크, 도메인 결과 + 변환 호출 + SCHEDULE_CONVERSION audit 1회")
        fun happy_singleChunkSuccess() {
            mockDomainSuccess(listOf(savedAttend()))

            val detail = service.insert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_SUCCESS)
            assertThat(detail.scheduleConversion).isEqualTo(ScheduleConversionSummary.ZERO)
            assertThat(detail.chunkCount).isEqualTo(1)

            val auditCaptor = slot<SapInboundAudit>()
            verify(exactly = 1) { auditService.record(capture(auditCaptor)) }
            assertThat(auditCaptor.captured.eventType).isEqualTo(SapInboundAuditEventType.SCHEDULE_CONVERSION)
        }

        @Test
        @DisplayName("청크 분할: 5건 / chunkSize=3 → 2 chunks (3+2), 도메인 호출 2회, 변환 2회, SCHEDULE_CONVERSION audit 1회 (집계)")
        fun chunkSplit_callsDomainAndConverterPerChunk() {
            val items = (1..5).map { item(startDate = "2026042$it", endDate = "2026042$it") }
            mockDomainSuccess(listOf(savedAttend()))

            val detail = service.insert(items)

            assertThat(detail.successCount).isEqualTo(5)
            assertThat(detail.chunks).hasSize(2)
            verify(exactly = 2) { attendInfoInsertService.insert(any()) }
            verify(exactly = 2) { scheduleConverter.convert(any<List<AttendInfo>>()) }
            verify(exactly = 1) { auditService.record(any()) }
        }

        @Test
        @DisplayName("savedAttendInfos 비어있을 시 변환 호출 없음, audit 0회")
        fun emptySaved_noConverterCall() {
            every { attendInfoInsertService.insert(any()) } returns
                AttendInfoInsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(AttendInfoInsertFailedRow(null, "EmployeeCode 필수")),
                    savedAttendInfos = emptyList()
                )

            val detail = service.insert(listOf(item(employeeCode = null)))

            assertThat(detail.scheduleConversion).isNull()
            verify(exactly = 0) { scheduleConverter.convert(any<List<AttendInfo>>()) }
            verify(exactly = 0) { auditService.record(any()) }
        }

        @Test
        @DisplayName("변환 실패: SCHEDULE_CONVERSION_FAILED audit 1회, INSERT 결과는 유지")
        fun conversionFailure_auditFailedAndKeepInsert() {
            mockDomainSuccess(listOf(savedAttend()))
            every { scheduleConverter.convert(any<List<AttendInfo>>()) } throws RuntimeException("DB error")

            val detail = service.insert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.scheduleConversion).isEqualTo(ScheduleConversionSummary.ZERO)
            val auditCaptor = slot<SapInboundAudit>()
            verify(exactly = 1) { auditService.record(capture(auditCaptor)) }
            assertThat(auditCaptor.captured.eventType).isEqualTo(SapInboundAuditEventType.SCHEDULE_CONVERSION_FAILED)
            assertThat(auditCaptor.captured.reason).contains("DB error")
        }

        @Test
        @DisplayName("변환 결과 합산: 두 청크 결과가 SCHEDULE_CONVERSION audit 에 합쳐서 노출")
        fun aggregatesAcrossChunks() {
            mockDomainSuccess(listOf(savedAttend()))
            every { scheduleConverter.convert(any<List<AttendInfo>>()) } returns
                ScheduleConversionSummary(
                    convertedScheduleCount = 2,
                    deletedScheduleCount = 1,
                    skippedEmployeeNotFound = 0,
                    skippedJobFilter = 1,
                    skippedAttendTypeFilter = 0,
                    skippedIdempotent = 0
                )
            val items = (1..5).map { item(startDate = "2026042$it", endDate = "2026042$it") }

            val detail = service.insert(items)

            assertThat(detail.scheduleConversion).isEqualTo(
                ScheduleConversionSummary(
                    convertedScheduleCount = 4,
                    deletedScheduleCount = 2,
                    skippedEmployeeNotFound = 0,
                    skippedJobFilter = 2,
                    skippedAttendTypeFilter = 0,
                    skippedIdempotent = 0
                )
            )
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑, chunk PARTIAL")
        fun partialFailure_chunkPartial() {
            every { attendInfoInsertService.insert(any()) } returns
                AttendInfoInsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(AttendInfoInsertFailedRow(null, "EmployeeCode 필수")),
                    savedAttendInfos = listOf(savedAttend())
                )

            val detail = service.insert(listOf(item(employeeCode = null), item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_PARTIAL)
        }

        @Test
        @DisplayName("청크 commit 실패: throw → chunk FAILED, 변환 호출 없음, audit 0회")
        fun chunkCommitFailure() {
            every { attendInfoInsertService.insert(any()) } throws RuntimeException("DB connection lost")

            val detail = service.insert(listOf(item()))

            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_FAILED)
            assertThat(detail.failureCount).isEqualTo(1)
            verify(exactly = 0) { scheduleConverter.convert(any<List<AttendInfo>>()) }
            verify(exactly = 0) { auditService.record(any()) }
        }

        @Test
        @DisplayName("DTO 매핑: AttendInfoRequestItem → AttendInfoInsertCommand")
        fun dtoMapping_itemToCommand() {
            mockDomainSuccess(listOf(savedAttend()))

            service.insert(listOf(item(employeeCode = "100123", startDate = "20260427", attendType = "10")))

            val captor = slot<List<AttendInfoInsertCommand>>()
            verify { attendInfoInsertService.insert(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.employeeCode).isEqualTo("100123")
            assertThat(command.startDate).isEqualTo("20260427")
            assertThat(command.attendType).isEqualTo("10")
            assertThat(command.status).isEqualTo("정상")
        }
    }

    @Nested
    @DisplayName("insert - 어댑터 정책")
    inner class AdapterPolicies {

        @Test
        @DisplayName("행 수 한도 초과 - SapPayloadTooLargeException, 도메인 호출 없음")
        fun insert_payloadTooLarge() {
            val items = (1..101).map { item() }

            assertThatThrownBy { service.insert(items) }
                .isInstanceOf(SapPayloadTooLargeException::class.java)
            verify(exactly = 0) { attendInfoInsertService.insert(any()) }
        }
    }
}
