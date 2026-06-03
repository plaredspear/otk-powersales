package com.otoki.powersales.savedsearch

import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import com.otoki.powersales.savedsearch.repository.SavedSearchRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.ApplicationArguments
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Consumer

@DisplayName("SavedSearchSeedRunner 테스트")
class SavedSearchSeedRunnerTest {

    private val repository: SavedSearchRepository = mockk(relaxed = true)
    private val transactionTemplate = mockk<TransactionTemplate>()
    private val runner = SavedSearchSeedRunner(repository, transactionTemplate)
    private val args = mockk<ApplicationArguments>(relaxed = true)

    init {
        // transactionTemplate.executeWithoutResult { ... } 가 블록을 그대로 실행하도록.
        every { transactionTemplate.executeWithoutResult(any()) } answers {
            firstArg<Consumer<TransactionStatus>>().accept(mockk(relaxed = true))
        }
    }

    @Test
    @DisplayName("기본 공용 프리셋이 없으면 owner=null SHARED 로 생성한다")
    fun seedsWhenAbsent() {
        every {
            repository.existsByResourceKeyAndOwnerIdIsNullAndScopeAndName("promotion", SavedSearchScope.SHARED, "전체 행사 조회")
        } returns false
        val saved = slot<SavedSearch>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        runner.run(args)

        verify { repository.save(any()) }
        assertThat(saved.captured.resourceKey).isEqualTo("promotion")
        assertThat(saved.captured.name).isEqualTo("전체 행사 조회")
        assertThat(saved.captured.scope).isEqualTo(SavedSearchScope.SHARED)
        assertThat(saved.captured.ownerId).isNull()
        assertThat(saved.captured.filters).isEmpty()
    }

    @Test
    @DisplayName("이미 존재하면 생성하지 않는다 (멱등 — 운영자 수정 보호)")
    fun idempotentWhenExists() {
        every {
            repository.existsByResourceKeyAndOwnerIdIsNullAndScopeAndName("promotion", SavedSearchScope.SHARED, "전체 행사 조회")
        } returns true

        runner.run(args)

        verify(exactly = 0) { repository.save(any()) }
    }
}
