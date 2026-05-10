package com.otoki.powersales.agreement.service

import com.otoki.powersales.agreement.dto.request.AdminAgreementWordCreateRequest
import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.common.repository.AgreementWordRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminAgreementWordService 테스트 (Spec #658 P1-B)")
class AdminAgreementWordServiceTest {

    @Mock
    private lateinit var agreementWordRepository: AgreementWordRepository

    @InjectMocks
    private lateinit var service: AdminAgreementWordService

    private val futureDate: LocalDate = LocalDate.now().plusMonths(6)

    private fun seedSavedEntity(
        active: Boolean? = false,
        activeDate: LocalDate? = null,
        afterActiveDate: LocalDate? = futureDate,
        name: String = "AGR-2026-001",
        contents: String = "위치정보 수집·이용 동의서"
    ): AgreementWord = AgreementWord(
        id = 12,
        name = name,
        contents = contents,
        active = active,
        activeDate = activeDate,
        afterActiveDate = afterActiveDate,
        isDeleted = false
    )

    @Test
    @DisplayName("U1 정상 등록 — Service 단 fallback 으로 active=false / activeDate=null 강제 적재")
    fun u1_normalCreate() {
        whenever(agreementWordRepository.save(any<AgreementWord>())).thenReturn(seedSavedEntity())

        val response = service.createAgreementWord(
            AdminAgreementWordCreateRequest(
                name = "AGR-2026-001",
                contents = "위치정보 수집·이용 동의서",
                afterActiveDate = futureDate
            )
        )

        val captor = argumentCaptor<AgreementWord>()
        verify(agreementWordRepository).save(captor.capture())
        val saved = captor.firstValue
        assertThat(saved.active).isFalse
        assertThat(saved.activeDate).isNull()
        assertThat(saved.afterActiveDate).isEqualTo(futureDate)
        assertThat(saved.name).isEqualTo("AGR-2026-001")
        assertThat(saved.isDeleted).isFalse

        assertThat(response.agreementWordId).isEqualTo(12)
        assertThat(response.active).isFalse
        assertThat(response.activeDate).isNull()
    }

    @Test
    @DisplayName("U2 active=true 입력 시 fallback — DTO 검증 우회 케이스에서도 Service 단이 false 강제")
    fun u2_activeTrueFallback() {
        whenever(agreementWordRepository.save(any<AgreementWord>())).thenReturn(seedSavedEntity())

        service.createAgreementWord(
            AdminAgreementWordCreateRequest(
                name = "AGR-2026-001",
                contents = "위치정보 수집·이용 동의서",
                afterActiveDate = futureDate,
                active = true
            )
        )

        val captor = argumentCaptor<AgreementWord>()
        verify(agreementWordRepository).save(captor.capture())
        assertThat(captor.firstValue.active).isFalse
    }

    @Test
    @DisplayName("U3 activeDate 입력 시 fallback — DTO 검증 우회 케이스에서도 Service 단이 null 강제")
    fun u3_activeDateFallback() {
        whenever(agreementWordRepository.save(any<AgreementWord>())).thenReturn(seedSavedEntity())

        service.createAgreementWord(
            AdminAgreementWordCreateRequest(
                name = "AGR-2026-001",
                contents = "위치정보 수집·이용 동의서",
                afterActiveDate = futureDate,
                activeDate = LocalDate.now()
            )
        )

        val captor = argumentCaptor<AgreementWord>()
        verify(agreementWordRepository).save(captor.capture())
        assertThat(captor.firstValue.activeDate).isNull()
    }

    @Test
    @DisplayName("U4 활성 약관 조회 — 존재 시 매핑 정합")
    fun u4_activeFound() {
        val today = LocalDate.now()
        val active = AgreementWord(
            id = 11,
            name = "AGR-2025-002",
            contents = "위치정보 수집·이용 동의서",
            active = true,
            activeDate = today,
            afterActiveDate = today.plusMonths(6),
            isDeleted = false
        )
        whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
            .thenReturn(Optional.of(active))

        val response = service.getActiveAgreementWord()

        assertThat(response).isNotNull
        assertThat(response!!.agreementWordId).isEqualTo(11)
        assertThat(response.name).isEqualTo("AGR-2025-002")
        assertThat(response.activeDate).isEqualTo(today)
        assertThat(response.afterActiveDate).isEqualTo(today.plusMonths(6))
    }

    @Test
    @DisplayName("U5 활성 약관 조회 — 부재 시 null 반환")
    fun u5_activeAbsent() {
        whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
            .thenReturn(Optional.empty())

        val response = service.getActiveAgreementWord()

        assertThat(response).isNull()
    }
}
