package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("SuggestionRepository 테스트")
class SuggestionRepositoryTest {

    @Autowired
    private lateinit var suggestionRepository: SuggestionRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        suggestionRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persistSuggestion(
        proposalNumber: String,
        title: String = "테스트 제안",
        category: SuggestionCategory = SuggestionCategory.NEW_PRODUCT,
        content: String = "본문",
        isDeleted: Boolean = false
    ): Suggestion {
        val s = Suggestion(
            proposalNumber = proposalNumber,
            title = title,
            content = content,
            category = category,
            status = SuggestionStatus.SUBMITTED,
            isDeleted = isDeleted
        )
        return testEntityManager.persistAndFlush(s)
    }

    @Test
    @DisplayName("저장 + 단건 조회 — soft-delete 제외 필터 동작")
    fun saveAndFindByIdAndIsDeletedFalse() {
        val saved = persistSuggestion(proposalNumber = "S-20260522-100001")

        val found = suggestionRepository.findByIdAndIsDeletedFalse(saved.id)

        assertThat(found).isNotNull
        assertThat(found?.proposalNumber).isEqualTo("S-20260522-100001")
        assertThat(found?.category).isEqualTo(SuggestionCategory.NEW_PRODUCT)
    }

    @Test
    @DisplayName("soft-delete row 는 findByIdAndIsDeletedFalse 결과에서 제외된다")
    fun softDeletedRowExcluded() {
        val saved = persistSuggestion(proposalNumber = "S-20260522-100002", isDeleted = true)

        val found = suggestionRepository.findByIdAndIsDeletedFalse(saved.id)

        assertThat(found).isNull()
    }

    @Test
    @Disabled("PostgreSQL sequence 의존 — H2 in-memory 에서는 미지원. 통합 테스트 (실 PG) 로 보강 예정")
    @DisplayName("nextProposalNumberSeqValue() 는 단조 증가 (race condition free 보장은 DB sequence 위임)")
    fun nextProposalNumberSeqValueMonotonic() {
        val first = suggestionRepository.nextProposalNumberSeqValue()
        val second = suggestionRepository.nextProposalNumberSeqValue()
        val third = suggestionRepository.nextProposalNumberSeqValue()

        assertThat(first).isGreaterThanOrEqualTo(100000)
        assertThat(second).isEqualTo(first + 1)
        assertThat(third).isEqualTo(first + 2)
    }
}
