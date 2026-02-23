package com.otoki.internal.entity

import java.io.Serializable

/**
 * SafetyCheckItem 복합 키
 *
 * safetycheck_list 테이블의 (question_num, seq_num) 복합 PK.
 */
class SafetyCheckItemId(
    val questionNum: Int = 0,
    val seqNum: Int = 0
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SafetyCheckItemId) return false
        return questionNum == other.questionNum && seqNum == other.seqNum
    }

    override fun hashCode(): Int {
        var result = questionNum
        result = 31 * result + seqNum
        return result
    }
}
