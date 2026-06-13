package com.otoki.powersales.external.sf.inbound.service

import com.otoki.powersales.external.sf.inbound.dto.sales.SfFailureItem
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * SF 인바운드 청크 단위 UPSERT 공통 헬퍼 (Spec #775).
 *
 * SAP 측 [com.otoki.powersales.external.sap.inbound.service.ChunkedUpsertHelper] 와 동일 패턴이지만 SF 패키지
 * 격리를 위해 별도 신설 — `Propagation.REQUIRES_NEW` 로 한 청크를 별도 트랜잭션에서 처리하여
 * 청크 단위 부분 실패를 격리한다.
 */
@Component
class SfChunkedUpsertHelper {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> processChunk(
        chunk: List<T>,
        action: (List<T>) -> SfChunkProcessResult
    ): SfChunkProcessResult = action(chunk)
}

data class SfChunkProcessResult(
    val successCount: Int,
    val failures: List<SfFailureItem>
)
