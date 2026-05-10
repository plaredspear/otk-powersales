package com.otoki.powersales.organization.service

import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.organization.service.dto.OrganizationReplaceCommand
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("OrganizationReplaceService 테스트")
class OrganizationReplaceServiceTest {

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var entityManager: EntityManager

    @Mock
    private lateinit var nativeQuery: Query

    @InjectMocks
    private lateinit var service: OrganizationReplaceService

    private fun stubAdvisoryLock() {
        whenever(entityManager.createNativeQuery(any<String>())).thenReturn(nativeQuery)
        whenever(nativeQuery.setParameter(any<String>(), any())).thenReturn(nativeQuery)
        whenever(nativeQuery.singleResult).thenReturn(1L)
    }

    private fun command(suffix: String): OrganizationReplaceCommand = OrganizationReplaceCommand(
        ccCd2 = "10$suffix", orgCd2 = "100$suffix", orgNm2 = "본사",
        ccCd3 = "11$suffix", orgCd3 = "110$suffix", orgNm3 = "사업부",
        ccCd4 = "12$suffix", orgCd4 = "120$suffix", orgNm4 = "팀",
        ccCd5 = "13$suffix", orgCd5 = "130$suffix", orgNm5 = "지점$suffix"
    )

    private fun stubSaveAllEcho() {
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            invocation.arguments[0] as List<Organization>
        }.whenever(organizationRepository).saveAll(any<List<Organization>>())
    }

    @Nested
    @DisplayName("replaceAll - 정상")
    inner class ReplaceAllSuccess {

        @Test
        @DisplayName("단일 행 - advisory lock + DELETE all + saveAll 호출, replacedCount=1")
        fun replaceAll_singleItem() {
            stubAdvisoryLock()
            stubSaveAllEcho()

            val result = service.replaceAll(listOf(command("0")))

            verify(entityManager).createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
            verify(nativeQuery).setParameter(eq("key"), eq(5560001L))
            verify(organizationRepository).deleteAllInBatch()
            verify(organizationRepository).flush()

            val captor = argumentCaptor<List<Organization>>()
            verify(organizationRepository).saveAll(captor.capture())
            assertThat(captor.firstValue).hasSize(1)
            assertThat(captor.firstValue[0].costCenterLevel2).isEqualTo("100")
            assertThat(captor.firstValue[0].orgCodeLevel5).isEqualTo("1300")
            assertThat(captor.firstValue[0].orgNameLevel5).isEqualTo("지점0")

            assertThat(result.replacedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("다중 행 - 모두 신규 Organization 으로 변환되어 저장, replacedCount 일치")
        fun replaceAll_multipleItems() {
            stubAdvisoryLock()
            stubSaveAllEcho()

            val result = service.replaceAll(listOf(command("1"), command("2"), command("3")))

            val captor = argumentCaptor<List<Organization>>()
            verify(organizationRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.map { it.orgNameLevel5 })
                .containsExactly("지점1", "지점2", "지점3")
            assertThat(result.replacedCount).isEqualTo(3)
        }

        @Test
        @DisplayName("일부 필드만 채워진 행 - 그대로 저장 (검증은 어댑터 책임)")
        fun replaceAll_partialFields() {
            stubAdvisoryLock()
            stubSaveAllEcho()
            val cmd = OrganizationReplaceCommand(
                ccCd2 = null, orgCd2 = null, orgNm2 = null,
                ccCd3 = null, orgCd3 = null, orgNm3 = null,
                ccCd4 = null, orgCd4 = null, orgNm4 = null,
                ccCd5 = "1111", orgCd5 = "11110", orgNm5 = "서울지점"
            )

            val result = service.replaceAll(listOf(cmd))

            val captor = argumentCaptor<List<Organization>>()
            verify(organizationRepository).saveAll(captor.capture())
            assertThat(captor.firstValue[0].costCenterLevel5).isEqualTo("1111")
            assertThat(captor.firstValue[0].costCenterLevel2).isNull()
            assertThat(result.replacedCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("replaceAll - 에러")
    inner class ReplaceAllError {

        @Test
        @DisplayName("saveAll 도중 ConstraintViolation - 예외 재전파 (어댑터에서 catch + 재전파)")
        fun replaceAll_constraintViolation() {
            stubAdvisoryLock()
            whenever(organizationRepository.saveAll(any<List<Organization>>()))
                .thenThrow(RuntimeException("constraint violation"))

            assertThatThrownBy { service.replaceAll(listOf(command("1"))) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("constraint violation")
        }
    }
}
