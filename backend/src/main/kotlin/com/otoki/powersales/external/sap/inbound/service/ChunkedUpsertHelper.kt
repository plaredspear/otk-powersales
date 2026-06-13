package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.inbound.dto.sales.FailureItem
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 청크 단위 UPSERT 공통 헬퍼. (Spec #560)
 *
 * 한 청크를 별도 [Propagation.REQUIRES_NEW] 트랜잭션으로 처리하여 청크 단위 부분 실패를 격리한다.
 * 호출자는 [processChunk] 에 전달한 [action] 안에서 행 단위 검증/변환 + saveAll 을 수행하고
 * 행 단위 실패는 [ChunkProcessResult.failures] 로 반환한다. 청크 commit 자체가 예외로 실패하면
 * 호출자가 캐치하여 청크 전체 failed 로 처리한다.
 */
@Component
class ChunkedUpsertHelper {

    /**
     * 한 청크를 별도 트랜잭션에서 실행한다.
     *
     * @return 청크 내 행 단위 결과 ([ChunkProcessResult.successCount], [ChunkProcessResult.failures])
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> processChunk(
        chunk: List<T>,
        action: (List<T>) -> ChunkProcessResult
    ): ChunkProcessResult = action(chunk)
}

data class ChunkProcessResult(
    val successCount: Int,
    val failures: List<FailureItem>
)
