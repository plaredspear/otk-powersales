package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapAttendInfoRequest.ReqItem
import com.otoki.internal.sap.entity.AttendInfo
import com.otoki.internal.sap.repository.AttendInfoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapAttendInfoService 테스트")
class SapAttendInfoServiceTest {

    @Mock
    private lateinit var attendInfoRepository: AttendInfoRepository

    @InjectMocks
    private lateinit var sapAttendInfoService: SapAttendInfoService

    @Nested
    @DisplayName("sync - 출퇴근 등록")
    inner class SyncTests {

        @Test
        @DisplayName("정상 등록 - 모든 필드 매핑")
        fun sync_success_allFieldsMapped() {
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val items = listOf(createReqItem(
                employeeCode = "100234",
                startDate = "20260301",
                endDate = "20260301",
                attendType = "연차",
                status = "승인"
            ))
            val result = sapAttendInfoService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<AttendInfo>()
            verify(attendInfoRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.employeeCode).isEqualTo("100234")
            assertThat(saved.startDate).isEqualTo("20260301")
            assertThat(saved.endDate).isEqualTo("20260301")
            assertThat(saved.attendType).isEqualTo("연차")
            assertThat(saved.status).isEqualTo("승인")
        }

        @Test
        @DisplayName("중복 호출 - 동일 데이터 2회 -> 2건 Insert")
        fun sync_duplicateCall_insertsAll() {
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val item = createReqItem(employeeCode = "100234", startDate = "20260301")
            val result = sapAttendInfoService.sync(listOf(item, item))

            assertThat(result.successCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("employee_code 누락 - 해당 레코드 실패")
        fun sync_missingEmployeeCode_fails() {
            val items = listOf(createReqItem(employeeCode = null, startDate = "20260301"))
            val result = sapAttendInfoService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("employee_code")
        }

        @Test
        @DisplayName("start_date 누락 - 해당 레코드 실패")
        fun sync_missingStartDate_fails() {
            val items = listOf(createReqItem(employeeCode = "100234", startDate = null))
            val result = sapAttendInfoService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("start_date")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val items = listOf(
                createReqItem(employeeCode = "100001", startDate = "20260301"),
                createReqItem(employeeCode = null, startDate = "20260302"),
                createReqItem(employeeCode = "100003", startDate = "20260303")
            )
            val result = sapAttendInfoService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("sync - 청크 처리")
    inner class ChunkTests {

        @Test
        @DisplayName("CHUNK_SIZE 확인 - 5000")
        fun chunkSize_is5000() {
            assertThat(SapAttendInfoService.CHUNK_SIZE).isEqualTo(5_000)
        }
    }

    private fun createReqItem(
        employeeCode: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        attendType: String? = null,
        status: String? = null
    ) = ReqItem(
        employeeCode = employeeCode,
        startDate = startDate,
        endDate = endDate,
        attendType = attendType,
        status = status
    )
}
