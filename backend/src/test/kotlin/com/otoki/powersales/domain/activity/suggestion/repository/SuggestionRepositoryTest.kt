package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.data.jpa.repository.Query
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

    /**
     * `nextProposalNumberSeqValue()` 는 PostgreSQL 전용 `nextval('...seq')` 네이티브 쿼리라
     * H2 in-memory 에서는 실행 자체가 불가(SQLGrammarException)하다. DB 실행 대신
     * 쿼리에 박힌 SQL 이 실제 Flyway 마이그레이션이 만드는 시퀀스와 정합함을 검증한다.
     *
     * 1) `@Query` 가 참조하는 시퀀스명(`powersales.suggestion_proposal_number_seq`) 이
     *    2) 마이그레이션(V173)의 `CREATE SEQUENCE` 대상과 동일하고
     *    3) 그 시퀀스가 START WITH 100000 (기존 테스트의 `>= 100000` 기대치의 출처) 임을 확인한다.
     */
    @Test
    @DisplayName("nextProposalNumberSeqValue() 쿼리 SQL 이 Flyway 시퀀스 DDL 과 정합한다 (H2 미실행)")
    fun nextProposalNumberSeqValueSqlMatchesMigration() {
        val query = SuggestionRepository::class.java
            .getMethod("nextProposalNumberSeqValue")
            .getAnnotation(Query::class.java)

        assertThat(query).isNotNull
        assertThat(query.nativeQuery).isTrue()

        val sequenceName = "powersales.suggestion_proposal_number_seq"
        // 쿼리는 정확히 해당 시퀀스를 nextval() 로 읽어야 한다.
        assertThat(query.value.replace(" ", ""))
            .isEqualToIgnoringCase("SELECTnextval('$sequenceName')")

        // 마이그레이션이 동일 시퀀스를 START WITH 100000 으로 생성함을 교차 검증.
        val ddl = ClassPathResource("db/migration/V173__create_suggestion.sql")
            .inputStream.bufferedReader().use { it.readText() }
        val normalized = ddl.replace(Regex("\\s+"), " ")

        assertThat(normalized).contains("CREATE SEQUENCE $sequenceName START WITH 100000")
    }
}
