package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterRequestItem
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.organization.repository.OrganizationRepository
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapOrganizeMasterService 테스트")
class SapOrganizeMasterServiceTest {

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var entityManager: EntityManager

    @Mock
    private lateinit var nativeQuery: Query

    @InjectMocks
    private lateinit var service: SapOrganizeMasterService

    private fun stubAdvisoryLock() {
        whenever(entityManager.createNativeQuery(any<String>())).thenReturn(nativeQuery)
        whenever(nativeQuery.setParameter(any<String>(), any())).thenReturn(nativeQuery)
        whenever(nativeQuery.singleResult).thenReturn(1L)
    }

    private fun item(suffix: String): OrganizeMasterRequestItem = OrganizeMasterRequestItem(
        ccCd2 = "10$suffix", orgCd2 = "100$suffix", orgNm2 = "본사",
        ccCd3 = "11$suffix", orgCd3 = "110$suffix", orgNm3 = "사업부",
        ccCd4 = "12$suffix", orgCd4 = "120$suffix", orgNm4 = "팀",
        ccCd5 = "13$suffix", orgCd5 = "130$suffix", orgNm5 = "지점$suffix"
    )

    @Nested
    @DisplayName("replaceAll - 정상")
    inner class ReplaceAllSuccess {

        @Test
        @DisplayName("단일 행 - DELETE all + saveAll 호출")
        fun replaceAll_singleItem() {
            stubAdvisoryLock()
            val items = listOf(item("0"))

            service.replaceAll(items)

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
        }

        @Test
        @DisplayName("다중 행 - 모두 신규 Organization 으로 변환되어 저장")
        fun replaceAll_multipleItems() {
            stubAdvisoryLock()
            val items = listOf(item("1"), item("2"), item("3"))

            service.replaceAll(items)

            val captor = argumentCaptor<List<Organization>>()
            verify(organizationRepository).saveAll(captor.capture())
            assertThat(captor.firstValue).hasSize(3)
            assertThat(captor.firstValue.map { it.orgNameLevel5 })
                .containsExactly("지점1", "지점2", "지점3")
        }

        @Test
        @DisplayName("일부 필드만 채워진 행 - 행 전체 null 만 아니면 통과")
        fun replaceAll_partialFields() {
            stubAdvisoryLock()
            val items = listOf(
                OrganizeMasterRequestItem(
                    ccCd5 = "1111", orgCd5 = "11110", orgNm5 = "서울지점"
                )
            )

            service.replaceAll(items)

            val captor = argumentCaptor<List<Organization>>()
            verify(organizationRepository).saveAll(captor.capture())
            assertThat(captor.firstValue).hasSize(1)
            assertThat(captor.firstValue[0].costCenterLevel5).isEqualTo("1111")
            assertThat(captor.firstValue[0].costCenterLevel2).isNull()
        }
    }

    @Nested
    @DisplayName("replaceAll - 에러")
    inner class ReplaceAllError {

        @Test
        @DisplayName("행 전체 null - INVALID_PAYLOAD 예외 + DELETE/INSERT 미수행")
        fun replaceAll_allNullRow() {
            stubAdvisoryLock()
            val items = listOf(item("1"), OrganizeMasterRequestItem(), item("3"))

            assertThatThrownBy { service.replaceAll(items) }
                .isInstanceOf(SapInvalidPayloadException::class.java)
                .hasMessageContaining("line 2")

            verify(organizationRepository, never()).deleteAllInBatch()
            verify(organizationRepository, never()).saveAll(any<List<Organization>>())
        }

        @Test
        @DisplayName("첫 행이 전체 null - line 1 표시")
        fun replaceAll_firstRowAllNull() {
            stubAdvisoryLock()
            val items = listOf(OrganizeMasterRequestItem(), item("2"))

            assertThatThrownBy { service.replaceAll(items) }
                .isInstanceOf(SapInvalidPayloadException::class.java)
                .hasMessageContaining("line 1")
        }
    }
}
