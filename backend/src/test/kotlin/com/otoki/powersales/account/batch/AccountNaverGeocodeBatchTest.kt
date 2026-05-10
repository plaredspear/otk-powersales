package com.otoki.powersales.account.batch

import com.otoki.powersales.account.service.AccountNaverGeocodeService
import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("AccountNaverGeocodeBatch — Naver Geocode 보강 daily 배치 (#637)")
class AccountNaverGeocodeBatchTest {

    private val service: AccountNaverGeocodeService = mock()
    private val runner = mock<com.otoki.powersales.common.jobrun.ScheduledJobRunner>()
    private val batchSize = 1000
    private val batch = AccountNaverGeocodeBatch(
        service = service,
        scheduledJobRunner = runner,
        batchSize = batchSize
    )

    @Test
    @DisplayName("execute — service 호출 + context metadata 적재")
    fun execute_invokesServiceAndRecordsMetadata() {
        whenever(service.enrichCoordinatesMissingAccounts(eq(batchSize))).thenReturn(
            AccountNaverGeocodeService.GeocodeBatchResult(scanned = 5, succeeded = 4, failed = 1)
        )
        val context = mock<ScheduledJobRunContext>()

        batch.execute(context)

        val captor = argumentCaptor<Map<String, Any?>>()
        verify(context).metadata(captor.capture())
        val metadata = captor.firstValue
        assertThat(metadata["scanned"]).isEqualTo(5)
        assertThat(metadata["succeeded"]).isEqualTo(4)
        assertThat(metadata["failed"]).isEqualTo(1)
    }

    @Test
    @DisplayName("execute — 매칭 0건이어도 service 1회 호출 + metadata 0/0/0 기록")
    fun execute_emptyResultStillRecordsMetadata() {
        whenever(service.enrichCoordinatesMissingAccounts(eq(batchSize))).thenReturn(
            AccountNaverGeocodeService.GeocodeBatchResult(scanned = 0, succeeded = 0, failed = 0)
        )
        val context = mock<ScheduledJobRunContext>()

        batch.execute(context)

        verify(service).enrichCoordinatesMissingAccounts(eq(batchSize))
        val captor = argumentCaptor<Map<String, Any?>>()
        verify(context).metadata(captor.capture())
        assertThat(captor.firstValue["scanned"]).isEqualTo(0)
    }

    @Test
    @DisplayName("execute — context null 인 경우 metadata 호출 없이 정상 종료")
    fun execute_nullContextDoesNotThrow() {
        whenever(service.enrichCoordinatesMissingAccounts(any())).thenReturn(
            AccountNaverGeocodeService.GeocodeBatchResult(scanned = 1, succeeded = 1, failed = 0)
        )

        batch.execute(context = null)

        verify(service).enrichCoordinatesMissingAccounts(eq(batchSize))
    }
}
