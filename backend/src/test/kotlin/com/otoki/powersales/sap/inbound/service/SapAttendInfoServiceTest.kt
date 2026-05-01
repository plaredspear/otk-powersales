package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoRequestItem
import com.otoki.powersales.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.schedule.repository.AttendInfoRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SapAttendInfoService 테스트")
class SapAttendInfoServiceTest {

    @Mock
    private lateinit var attendInfoRepository: AttendInfoRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @Mock
    private lateinit var scheduleConverter: AttendInfoToScheduleConverter

    private lateinit var service: SapAttendInfoService

    @BeforeEach
    fun setUp() {
        whenever(scheduleConverter.convert(any<List<AttendInfo>>()))
            .thenReturn(ScheduleConversionSummary.ZERO)
        val helper = ChunkedUpsertHelper()
        service = SapAttendInfoService(
            attendInfoRepository = attendInfoRepository,
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

    private fun mockSaveAll() {
        whenever(attendInfoRepository.saveAll(any<List<AttendInfo>>()))
            .thenAnswer { it.getArgument<List<AttendInfo>>(0) }
    }

    @Nested
    @DisplayName("insert - Happy Path")
    inner class InsertHappy {

        @Test
        @DisplayName("정상 1건 - INSERT, success_count=1, chunk success")
        fun insert_success() {
            mockSaveAll()

            val detail = service.insert(listOf(item()))

            val captor = argumentCaptor<List<AttendInfo>>()
            verify(attendInfoRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.startDate).isEqualTo("20260427")
            assertThat(saved.endDate).isEqualTo("20260427")
            assertThat(saved.attendType).isEqualTo("14")
            assertThat(saved.status).isEqualTo("정상")
            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_SUCCESS)
            assertThat(detail.scheduleConversion).isEqualTo(ScheduleConversionSummary.ZERO)
            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService, times(2)).record(auditCaptor.capture())
            assertThat(auditCaptor.allValues.map { it.eventType }).containsExactly(
                SapInboundAuditEventType.REQUEST_ACCEPTED,
                SapInboundAuditEventType.SCHEDULE_CONVERSION
            )
        }

        @Test
        @DisplayName("AttendType 미매칭 - 원본 코드 그대로 저장 (D3 결정), 거부 안 함")
        fun insert_unknownAttendType() {
            mockSaveAll()

            val detail = service.insert(listOf(item(attendType = "ZZ_UNKNOWN")))

            val captor = argumentCaptor<List<AttendInfo>>()
            verify(attendInfoRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().attendType).isEqualTo("ZZ_UNKNOWN")
            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("청크 분할 - 5건, chunkSize=3 → 2 chunks (3+2)")
        fun insert_chunkSplit() {
            val items = (1..5).map { item(startDate = "2026042$it", endDate = "2026042$it") }
            mockSaveAll()

            val detail = service.insert(items)

            assertThat(detail.successCount).isEqualTo(5)
            assertThat(detail.chunks).hasSize(2)
            assertThat(detail.chunks[0].count).isEqualTo(3)
            assertThat(detail.chunks[1].count).isEqualTo(2)
            assertThat(detail.chunks).allMatch { it.status == ChunkResult.STATUS_SUCCESS }
        }
    }

    @Nested
    @DisplayName("insert - Error Path")
    inner class InsertError {

        @Test
        @DisplayName("행 수 한도 초과 - SapPayloadTooLargeException")
        fun insert_payloadTooLarge() {
            val items = (1..101).map { item() }

            assertThatThrownBy { service.insert(items) }
                .isInstanceOf(SapPayloadTooLargeException::class.java)
            verify(attendInfoRepository, never()).saveAll(any<List<AttendInfo>>())
        }

        @Test
        @DisplayName("EmployeeCode 누락 - 청크 partial, 행 failed")
        fun insert_missingEmployeeCode() {
            mockSaveAll()

            val detail = service.insert(listOf(item(employeeCode = null), item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("EmployeeCode 필수")
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_PARTIAL)
        }

        @Test
        @DisplayName("StartDate 누락 - failures")
        fun insert_missingStartDate() {
            val detail = service.insert(listOf(item(startDate = null)))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("StartDate 필수")
        }

        @Test
        @DisplayName("EndDate 누락 - failures")
        fun insert_missingEndDate() {
            val detail = service.insert(listOf(item(endDate = null)))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("EndDate 필수")
        }

        @Test
        @DisplayName("StartDate 형식 오류 - failures")
        fun insert_invalidStartDate() {
            val detail = service.insert(listOf(item(startDate = "2026-04-27")))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("StartDate YYYYMMDD 형식 오류")
        }

        @Test
        @DisplayName("청크 commit 실패 - 해당 청크 failed, 다른 청크는 success")
        fun insert_chunkCommitFailure() {
            val items = (1..5).map { item(startDate = "2026042$it", endDate = "2026042$it") }
            var callCount = 0
            doAnswer {
                callCount++
                if (callCount == 1) throw RuntimeException("DB connection lost")
                @Suppress("UNCHECKED_CAST")
                it.arguments[0] as List<AttendInfo>
            }.whenever(attendInfoRepository).saveAll(any<List<AttendInfo>>())

            val detail = service.insert(items)

            assertThat(detail.chunks).hasSize(2)
            assertThat(detail.chunks[0].status).isEqualTo(ChunkResult.STATUS_FAILED)
            assertThat(detail.chunks[1].status).isEqualTo(ChunkResult.STATUS_SUCCESS)
            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("insert - 일정 변환 연동 (Spec #553)")
    inner class ScheduleConversionWiring {

        @Test
        @DisplayName("저장 0건 - 변환 호출 없음, scheduleConversion=null")
        fun noSavedRows_conversionNotCalled() {
            val detail = service.insert(listOf(item(employeeCode = null)))

            assertThat(detail.scheduleConversion).isNull()
            verify(scheduleConverter, never()).convert(any<List<AttendInfo>>())
            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
        }

        @Test
        @DisplayName("청크별 변환 결과 합산 - 두 청크 결과가 합쳐서 노출")
        fun aggregatesAcrossChunks() {
            mockSaveAll()
            whenever(scheduleConverter.convert(any<List<AttendInfo>>())).thenReturn(
                ScheduleConversionSummary(
                    convertedScheduleCount = 2,
                    deletedScheduleCount = 1,
                    skippedEmployeeNotFound = 0,
                    skippedJobFilter = 1,
                    skippedAttendTypeFilter = 0,
                    skippedIdempotent = 0
                )
            )
            val items = (1..5).map { item(startDate = "2026042$it", endDate = "2026042$it") }

            val detail = service.insert(items)

            verify(scheduleConverter, times(2)).convert(any<List<AttendInfo>>())
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
            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService, times(2)).record(auditCaptor.capture())
            assertThat(auditCaptor.allValues.map { it.eventType }).containsExactly(
                SapInboundAuditEventType.REQUEST_ACCEPTED,
                SapInboundAuditEventType.SCHEDULE_CONVERSION
            )
        }

        @Test
        @DisplayName("변환 실패 - SCHEDULE_CONVERSION_FAILED audit 기록, INSERT 결과는 유지")
        fun conversionFailure_auditFailedAndKeepInsert() {
            mockSaveAll()
            whenever(scheduleConverter.convert(any<List<AttendInfo>>()))
                .thenThrow(RuntimeException("DB error"))

            val detail = service.insert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.scheduleConversion).isEqualTo(ScheduleConversionSummary.ZERO)
            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService, times(2)).record(auditCaptor.capture())
            assertThat(auditCaptor.allValues.map { it.eventType }).containsExactly(
                SapInboundAuditEventType.SCHEDULE_CONVERSION_FAILED,
                SapInboundAuditEventType.REQUEST_ACCEPTED
            )
            assertThat(
                auditCaptor.allValues.first { it.eventType == SapInboundAuditEventType.SCHEDULE_CONVERSION_FAILED }.reason
            ).contains("DB error")
        }
    }
}
