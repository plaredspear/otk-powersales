package com.otoki.internal.repository

import com.otoki.internal.entity.Suggestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 제안 Repository
 */
@Repository
interface SuggestionRepository : JpaRepository<Suggestion, Long> {

    /**
     * 사용자별 제안 목록 조회
     * 최신순으로 정렬
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Suggestion>
}
