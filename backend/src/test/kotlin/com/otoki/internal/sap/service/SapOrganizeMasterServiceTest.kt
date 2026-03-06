package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapOrganizeMasterRequest.ReqItem
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapOrganizeMasterService 테스트")
class SapOrganizeMasterServiceTest {

    @Mock
    private lateinit var orgRepository: OrgRepository

    @InjectMocks
    private lateinit var sapOrganizeMasterService: SapOrganizeMasterService

    @Nested
    @DisplayName("sync - 조직마스터 동기화")
    inner class SyncTests {

        @Test
        @DisplayName("정상 동기화 - 전체 삭제 후 Insert, successCount = 요청 건수")
        fun sync_success() {
            val items = listOf(
                createReqItem(ccCd2 = "1000", orgCd2 = "O100", orgNm2 = "오뚜기"),
                createReqItem(ccCd2 = "1000", orgCd3 = "O110", orgNm3 = "영업본부")
            )
            whenever(orgRepository.saveAll(any<List<Org>>())).thenAnswer { it.getArgument<List<Org>>(0) }

            val result = sapOrganizeMasterService.sync(items)

            verify(orgRepository).deleteAllInBatch()
            verify(orgRepository).saveAll(any<List<Org>>())
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(0)
        }

        @Test
        @DisplayName("단일 건 동기화 - 1건 Insert 성공")
        fun sync_singleItem() {
            val items = listOf(createReqItem(ccCd2 = "1000"))
            whenever(orgRepository.saveAll(any<List<Org>>())).thenAnswer { it.getArgument<List<Org>>(0) }

            val result = sapOrganizeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
        }

        @Test
        @DisplayName("null 필드 포함 - Level2만 있고 나머지 null인 레코드 처리")
        fun sync_partialFields() {
            val items = listOf(
                ReqItem(ccCd2 = "1000", orgCd2 = "O100", orgNm2 = "오뚜기")
            )
            whenever(orgRepository.saveAll(any<List<Org>>())).thenAnswer { it.getArgument<List<Org>>(0) }

            val result = sapOrganizeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
        }
    }

    private fun createReqItem(
        ccCd2: String? = null,
        orgCd2: String? = null,
        orgNm2: String? = null,
        ccCd3: String? = null,
        orgCd3: String? = null,
        orgNm3: String? = null,
        ccCd4: String? = null,
        orgCd4: String? = null,
        orgNm4: String? = null,
        ccCd5: String? = null,
        orgCd5: String? = null,
        orgNm5: String? = null
    ) = ReqItem(ccCd2, orgCd2, orgNm2, ccCd3, orgCd3, orgNm3, ccCd4, orgCd4, orgNm4, ccCd5, orgCd5, orgNm5)
}
