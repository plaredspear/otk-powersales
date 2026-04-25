package com.otoki.powersales.sap.outbound.sender

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.repository.PPTMasterRepository
import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.outbound.client.SapOutboundClient
import com.otoki.powersales.sap.outbound.dto.SapOutboundRequest
import com.otoki.powersales.sap.outbound.dto.SapOutboundResponse
import com.otoki.powersales.sap.outbound.dto.SapPPTMasterOutboundDto
import com.otoki.powersales.sap.outbound.exception.SapOutboundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
@DisplayName("SapPPTMasterSender 테스트")
class SapPPTMasterSenderTest {

    @Mock
    private lateinit var pptMasterRepository: PPTMasterRepository

    @Mock
    private lateinit var sapOutboundClient: SapOutboundClient

    private lateinit var sender: SapPPTMasterSender
    private val today: LocalDate = LocalDate.of(2026, 4, 16)
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    @BeforeEach
    fun setUp() {
        val fixedClock = Clock.fixed(
            today.atStartOfDay(zone).toInstant(),
            zone
        )
        sender = SapPPTMasterSender(pptMasterRepository, sapOutboundClient, fixedClock)
    }

    private fun createMaster(id: Long, isConfirmed: Boolean = true): ProfessionalPromotionTeamMaster {
        val employee = Employee(id = id, employeeCode = "E$id", name = "직원$id").also { it.status = "재직" }
        val account = Account(id = id.toInt(), name = "거래처$id").also { it.externalKey = "K$id" }
        return ProfessionalPromotionTeamMaster(
            id = id,
            employeeId = employee.id,
            accountId = account.id,
            teamType = "라면세일조",
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
            isConfirmed = isConfirmed,
            employee = employee,
            account = account
        )
    }

    @Test
    @DisplayName("당월 첫째날~마지막날 기준으로 repository 조회를 호출한다")
    fun queriesRepository_withMonthRange() {
        whenever(pptMasterRepository.findSapOutboundTargets(any(), any())).thenReturn(emptyList())
        whenever(sapOutboundClient.send(eq("SD03300"), any())).thenReturn(
            SapOutboundResponse(resultCode = "200", resultMsg = "SKIPPED_EMPTY")
        )

        sender.send()

        val firstCaptor = argumentCaptor<LocalDate>()
        val lastCaptor = argumentCaptor<LocalDate>()
        verify(pptMasterRepository).findSapOutboundTargets(firstCaptor.capture(), lastCaptor.capture())
        assertThat(firstCaptor.firstValue).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(lastCaptor.firstValue).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @Test
    @DisplayName("정상 전송 - 3건 -> 1배치, requestCount=3, batchCount=1")
    fun sendsAllItems_inSingleBatch() {
        whenever(pptMasterRepository.findSapOutboundTargets(any(), any())).thenReturn(
            (1L..3L).map { createMaster(it) }
        )
        whenever(sapOutboundClient.send(eq("SD03300"), any())).thenReturn(
            SapOutboundResponse(resultCode = "200", resultMsg = "SUCCESS")
        )

        val result = sender.send()

        assertThat(result.requestCount).isEqualTo(3)
        assertThat(result.batchCount).isEqualTo(1)
        assertThat(result.resultCode).isEqualTo("200")
        verify(sapOutboundClient, times(1)).send(eq("SD03300"), any())
    }

    @Test
    @DisplayName("배치 분할 - 250건 -> 100/100/50 = 3배치")
    fun splitsBatchesOf100() {
        whenever(pptMasterRepository.findSapOutboundTargets(any(), any())).thenReturn(
            (1L..250L).map { createMaster(it) }
        )
        whenever(sapOutboundClient.send(eq("SD03300"), any())).thenReturn(
            SapOutboundResponse(resultCode = "200", resultMsg = "SUCCESS")
        )

        val captor = argumentCaptor<SapOutboundRequest<*>>()
        val result = sender.send()

        verify(sapOutboundClient, times(3)).send(eq("SD03300"), captor.capture())
        assertThat(result.requestCount).isEqualTo(250)
        assertThat(result.batchCount).isEqualTo(3)
        assertThat(captor.allValues.map { it.reqItemList.size }).containsExactly(100, 100, 50)
    }

    @Test
    @DisplayName("대상 없음 - 빈 SapOutboundRequest로 한 번 호출, requestCount=0/batchCount=0")
    fun emptyTargets_sendsEmptyRequestOnce() {
        whenever(pptMasterRepository.findSapOutboundTargets(any(), any())).thenReturn(emptyList())
        whenever(sapOutboundClient.send(eq("SD03300"), any())).thenReturn(
            SapOutboundResponse(resultCode = "200", resultMsg = "SKIPPED_EMPTY")
        )

        val captor = argumentCaptor<SapOutboundRequest<*>>()
        val result = sender.send()

        verify(sapOutboundClient, times(1)).send(eq("SD03300"), captor.capture())
        assertThat(captor.firstValue.reqItemList).isEmpty()
        assertThat(result.requestCount).isEqualTo(0)
        assertThat(result.batchCount).isEqualTo(0)
        assertThat(result.resultMsg).isEqualTo("SKIPPED_EMPTY")
    }

    @Test
    @DisplayName("SAP 송신 실패 - SapOutboundException 전파")
    fun sapFailure_propagatesException() {
        whenever(pptMasterRepository.findSapOutboundTargets(any(), any())).thenReturn(
            listOf(createMaster(1L))
        )
        whenever(sapOutboundClient.send(eq("SD03300"), any())).thenThrow(SapOutboundException("HTTP 500"))

        assertThatThrownBy { sender.send() }
            .isInstanceOf(SapOutboundException::class.java)
    }

    @Test
    @DisplayName("DTO 변환 - reqItemList의 첫 항목이 17개 필드 매핑을 만족한다")
    fun convertsToOutboundDto() {
        whenever(pptMasterRepository.findSapOutboundTargets(any(), any())).thenReturn(
            listOf(createMaster(42L))
        )
        whenever(sapOutboundClient.send(eq("SD03300"), any())).thenReturn(
            SapOutboundResponse(resultCode = "200", resultMsg = "SUCCESS")
        )

        val captor = argumentCaptor<SapOutboundRequest<*>>()
        sender.send()

        verify(sapOutboundClient).send(eq("SD03300"), captor.capture())
        val item = captor.firstValue.reqItemList.first() as SapPPTMasterOutboundDto
        assertThat(item.name).isEqualTo("42")
        assertThat(item.yearMonth).isEqualTo("202604")
        assertThat(item.startDate).isEqualTo("2026-01-01")
        assertThat(item.endDate).isEqualTo("2026-12-31")
    }
}
