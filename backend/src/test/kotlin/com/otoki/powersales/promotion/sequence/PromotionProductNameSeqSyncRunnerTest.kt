package com.otoki.powersales.promotion.sequence

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("PromotionProductNameSeqSyncRunner")
class PromotionProductNameSeqSyncRunnerTest {

    private lateinit var entityManager: EntityManager
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var runner: PromotionProductNameSeqSyncRunner

    @BeforeEach
    fun setUp() {
        entityManager = mockk()
        transactionTemplate = mockk()
        every { transactionTemplate.executeWithoutResult(any()) } answers {
            val action = arg<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>(0)
            action.accept(mockk(relaxed = true))
        }
        runner = PromotionProductNameSeqSyncRunner(entityManager, transactionTemplate)
    }

    private fun mockQueries(maxSuffix: Long, currentSeq: Long): Query {
        val maxQuery = mockk<Query>()
        every { maxQuery.singleResult } returns maxSuffix
        val seqQuery = mockk<Query>()
        every { seqQuery.singleResult } returns currentSeq

        every {
            entityManager.createNativeQuery(match<String> { it.contains("MAX(SUBSTRING(name FROM 3)") })
        } returns maxQuery
        every {
            entityManager.createNativeQuery(match<String> { it.contains("SELECT last_value") })
        } returns seqQuery

        // setval 호출 모킹 (사용되지 않는 케이스에서는 verify 0회)
        val setvalQuery = mockk<Query>(relaxed = true)
        every {
            entityManager.createNativeQuery(match<String> { it.contains("setval") })
        } returns setvalQuery
        every { setvalQuery.setParameter(any<String>(), any()) } returns setvalQuery
        return setvalQuery
    }

    @Test
    @DisplayName("sequence > max name suffix — no-op")
    fun noOpWhenSeqAlreadyAhead() {
        val setvalQuery = mockQueries(maxSuffix = 100L, currentSeq = 500L)

        runner.run(DefaultApplicationArguments())

        verify(exactly = 0) { setvalQuery.singleResult }
    }

    @Test
    @DisplayName("sequence == max name suffix — no-op")
    fun noOpWhenSeqEqualsMax() {
        val setvalQuery = mockQueries(maxSuffix = 100L, currentSeq = 100L)

        runner.run(DefaultApplicationArguments())

        verify(exactly = 0) { setvalQuery.singleResult }
    }

    @Test
    @DisplayName("sequence < max name suffix — setval 호출")
    fun setvalWhenSeqBehind() {
        val setvalQuery = mockQueries(maxSuffix = 127052L, currentSeq = 1L)

        runner.run(DefaultApplicationArguments())

        verify(exactly = 1) { setvalQuery.singleResult }
        verify { setvalQuery.setParameter("v", 127052L) }
    }
}
