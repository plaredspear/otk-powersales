package com.otoki.powersales.domain.sales.materialize

/**
 * ORORA 매출이력 적재 시 거래처 코드 범위를 청크로 분할하는 값 객체 (Spec #855).
 *
 * 레거시 `IF_REST_ORORA_ReceiveMonthlySalesHistory` / `Queueable_OroraDailySalesHistory_M1` 의
 * `From_cust`/`To_cust` 범위 + chunk 분할 동등. ORORA view 의 거래처 코드는 선행 `000` 포함
 * 원본 형식(예: `0001000000`) 이므로, 숫자 범위를 청크로 나눈 뒤 `000` prefix 를 붙여 문자열 경계로 변환한다.
 *
 * @property fromInclusive 거래처 코드 시작 (숫자, 포함)
 * @property toInclusive 거래처 코드 끝 (숫자, 포함)
 * @property chunkSize 청크당 거래처 코드 폭
 */
data class OroraAccountRange(
    val fromInclusive: Long,
    val toInclusive: Long,
    val chunkSize: Long,
) {
    init {
        require(fromInclusive <= toInclusive) { "fromInclusive($fromInclusive) <= toInclusive($toInclusive) 위배" }
        require(chunkSize > 0) { "chunkSize($chunkSize) > 0 위배" }
    }

    /**
     * 거래처 코드 범위를 [chunkSize] 폭의 청크 경계 (fromCode, toCode) 쌍 리스트로 분할.
     *
     * 각 경계는 ORORA view 원본 형식(선행 `000`) 문자열로 반환된다. 레거시는 `'000' + String.valueOf(i)`
     * 로 prefix 를 붙였다 (`Batch_OroraDailySalesHistory_M1 cls:26-27`).
     */
    fun toChunks(): List<Pair<String, String>> {
        val chunks = mutableListOf<Pair<String, String>>()
        var start = fromInclusive
        while (start <= toInclusive) {
            val end = minOf(start + chunkSize - 1, toInclusive)
            chunks += withPrefix(start) to withPrefix(end)
            start += chunkSize
        }
        return chunks
    }

    /**
     * [toChunks] 가 만드는 전체 청크 개수.
     *
     * 운영자 수동 트리거가 "전체 N개 청크 중 몇 번째" 를 선택할 수 있도록 청크 총수를 노출한다.
     */
    fun chunkCount(): Int {
        val span = toInclusive - fromInclusive + 1
        return ((span + chunkSize - 1) / chunkSize).toInt()
    }

    /**
     * `chunkIndex`(0-based) 번째 청크 1개만 담은 새 [OroraAccountRange] 를 반환.
     *
     * 거래처별 chunk 단위 수동 적재용 — 전체 범위를 도는 대신 선택된 청크 1개의 거래처 구간만
     * 적재하도록, 그 청크의 (from, to) 를 새 range 로 좁혀 [toChunks] 가 단일 청크를 반환하게 한다.
     *
     * @throws IndexOutOfBoundsException `chunkIndex` 가 `[0, chunkCount)` 밖일 때
     */
    fun singleChunk(chunkIndex: Int): OroraAccountRange {
        val count = chunkCount()
        if (chunkIndex < 0 || chunkIndex >= count) {
            throw IndexOutOfBoundsException("chunkIndex($chunkIndex) 는 [0, $count) 범위여야 합니다")
        }
        val from = fromInclusive + chunkIndex.toLong() * chunkSize
        val to = minOf(from + chunkSize - 1, toInclusive)
        return OroraAccountRange(from, to, chunkSize)
    }

    private fun withPrefix(code: Long): String = ACCOUNT_CODE_PREFIX + code.toString()

    companion object {
        /** ORORA view 거래처 코드의 선행 prefix (레거시 `'000'` 동등). */
        const val ACCOUNT_CODE_PREFIX = "000"
    }
}
