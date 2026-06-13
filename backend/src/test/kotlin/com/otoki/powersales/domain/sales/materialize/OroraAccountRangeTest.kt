package com.otoki.powersales.domain.sales.materialize

import com.otoki.powersales.domain.sales.materialize.OroraAccountRange
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [OroraAccountRange] 청크 분할 검증 (Spec #855).
 */
@DisplayName("OroraAccountRange 청크 분할")
class OroraAccountRangeTest {

    @Test
    @DisplayName("범위를 chunkSize 폭으로 분할하고 선행 000 prefix 를 붙인다")
    fun chunks() {
        val range = OroraAccountRange(fromInclusive = 1000000, toInclusive = 1006000, chunkSize = 2000)

        val chunks = range.toChunks()

        // 1000000~1006000 / 2000 = 4 chunk (1000000-1001999, 1002000-1003999, 1004000-1005999, 1006000-1006000)
        assertThat(chunks).containsExactly(
            "0001000000" to "0001001999",
            "0001002000" to "0001003999",
            "0001004000" to "0001005999",
            "0001006000" to "0001006000",
        )
    }

    @Test
    @DisplayName("범위가 chunkSize 보다 작으면 단일 chunk")
    fun singleChunk() {
        val range = OroraAccountRange(fromInclusive = 1000000, toInclusive = 1000500, chunkSize = 2000)

        val chunks = range.toChunks()

        assertThat(chunks).containsExactly("0001000000" to "0001000500")
    }

    @Test
    @DisplayName("from > to 면 예외")
    fun invalidRange() {
        assertThatThrownBy { OroraAccountRange(fromInclusive = 1100000, toInclusive = 1000000, chunkSize = 2000) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("chunkSize 0 이하면 예외")
    fun invalidChunkSize() {
        assertThatThrownBy { OroraAccountRange(fromInclusive = 1000000, toInclusive = 1100000, chunkSize = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
