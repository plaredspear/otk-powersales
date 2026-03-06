package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapSystemCodeMasterRequest.ReqItem
import com.otoki.internal.sap.entity.SystemCodeMaster
import com.otoki.internal.sap.repository.SystemCodeMasterRepository
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
@DisplayName("SapSystemCodeMasterService 테스트")
class SapSystemCodeMasterServiceTest {

    @Mock
    private lateinit var systemCodeMasterRepository: SystemCodeMasterRepository

    @InjectMocks
    private lateinit var sapSystemCodeMasterService: SapSystemCodeMasterService

    @Nested
    @DisplayName("sync - 신규 시스템코드 등록")
    inner class NewSystemCodeTests {

        @Test
        @DisplayName("정상 등록 - external_key 생성 확인")
        fun sync_newSystemCode_creates() {
            val items = listOf(createReqItem(
                companyCode = "1000", groupCode = "H10010", detailCode = "1",
                groupCodeName = "재직상태", detailCodeName = "재직", seq = "1"
            ))
            whenever(systemCodeMasterRepository.findByExternalKey("1000;H10010;1")).thenReturn(null)
            whenever(systemCodeMasterRepository.save(any<SystemCodeMaster>())).thenAnswer { it.getArgument<SystemCodeMaster>(0) }

            val result = sapSystemCodeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            val captor = argumentCaptor<SystemCodeMaster>()
            verify(systemCodeMasterRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.externalKey).isEqualTo("1000;H10010;1")
            assertThat(saved.companyCode).isEqualTo("1000")
            assertThat(saved.groupCode).isEqualTo("H10010")
            assertThat(saved.detailCode).isEqualTo("1")
            assertThat(saved.groupCodeName).isEqualTo("재직상태")
            assertThat(saved.detailCodeName).isEqualTo("재직")
            assertThat(saved.seq).isEqualTo("1")
            assertThat(saved.createdAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("sync - 기존 시스템코드 업데이트")
    inner class ExistingSystemCodeTests {

        @Test
        @DisplayName("기존 시스템코드 업데이트 - detail_code_name 변경")
        fun sync_existingSystemCode_updates() {
            val existing = SystemCodeMaster(
                id = 1, companyCode = "1000", groupCode = "H10010", detailCode = "1",
                externalKey = "1000;H10010;1", groupCodeName = "재직상태", detailCodeName = "기존"
            )
            val items = listOf(createReqItem(
                companyCode = "1000", groupCode = "H10010", detailCode = "1",
                groupCodeName = "재직상태", detailCodeName = "변경됨"
            ))
            whenever(systemCodeMasterRepository.findByExternalKey("1000;H10010;1")).thenReturn(existing)
            whenever(systemCodeMasterRepository.save(any<SystemCodeMaster>())).thenAnswer { it.getArgument<SystemCodeMaster>(0) }

            val result = sapSystemCodeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existing.detailCodeName).isEqualTo("변경됨")
            assertThat(existing.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("company_code 누락 - 해당 레코드 실패")
        fun sync_missingCompanyCode_fails() {
            val items = listOf(createReqItem(companyCode = null, groupCode = "H10010", detailCode = "1"))

            val result = sapSystemCodeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("company_code")
        }

        @Test
        @DisplayName("group_code 누락 - 해당 레코드 실패")
        fun sync_missingGroupCode_fails() {
            val items = listOf(createReqItem(companyCode = "1000", groupCode = null, detailCode = "1"))

            val result = sapSystemCodeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("group_code")
        }

        @Test
        @DisplayName("detail_code 누락 - 해당 레코드 실패")
        fun sync_missingDetailCode_fails() {
            val items = listOf(createReqItem(companyCode = "1000", groupCode = "H10010", detailCode = null))

            val result = sapSystemCodeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("detail_code")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            val items = listOf(
                createReqItem(companyCode = "1000", groupCode = "G1", detailCode = "1"),
                createReqItem(companyCode = null, groupCode = "G2", detailCode = "2"),
                createReqItem(companyCode = "1000", groupCode = "G3", detailCode = "3")
            )
            whenever(systemCodeMasterRepository.findByExternalKey("1000;G1;1")).thenReturn(null)
            whenever(systemCodeMasterRepository.findByExternalKey("1000;G3;3")).thenReturn(null)
            whenever(systemCodeMasterRepository.save(any<SystemCodeMaster>())).thenAnswer { it.getArgument<SystemCodeMaster>(0) }

            val result = sapSystemCodeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    private fun createReqItem(
        companyCode: String? = null,
        groupCode: String? = null,
        detailCode: String? = null,
        groupCodeName: String? = null,
        detailCodeName: String? = null,
        seq: String? = null
    ) = ReqItem(
        companyCode = companyCode,
        groupCode = groupCode,
        detailCode = detailCode,
        groupCodeName = groupCodeName,
        detailCodeName = detailCodeName,
        seq = seq
    )
}
