package com.otoki.powersales.organization.branchmapping

import com.otoki.powersales.organization.branchmapping.entity.BranchMapping
import com.otoki.powersales.organization.branchmapping.repository.BranchMappingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.transaction.support.TransactionTemplate
import java.util.Optional

@DisplayName("BranchMappingSyncRunner")
class BranchMappingSyncRunnerTest {

    private lateinit var repository: BranchMappingRepository
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var runner: BranchMappingSyncRunner

    @BeforeEach
    fun setUp() {
        repository = mockk()
        transactionTemplate = mockk()
        every { transactionTemplate.executeWithoutResult(any()) } answers {
            val action = arg<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>(0)
            action.accept(mockk(relaxed = true))
        }
        runner = BranchMappingSyncRunner(repository, transactionTemplate)
    }

    @Test
    @DisplayName("빈 DB 부팅 — SoT 74건 모두 INSERT")
    fun insertsAllOnEmptyDb() {
        every { repository.findById(any<String>()) } returns Optional.empty()
        every { repository.save(any<BranchMapping>()) } answers { firstArg() }

        runner.run(DefaultApplicationArguments())

        verify(exactly = BranchMappingMatrix.ALL.size) { repository.save(any<BranchMapping>()) }
    }

    @Test
    @DisplayName("동일 DB 부팅 — 변경 없으면 save 호출 없음")
    fun noOpOnIdenticalDb() {
        for (entry in BranchMappingMatrix.ALL) {
            every { repository.findById(entry.branchCode) } returns Optional.of(
                BranchMapping(
                    branchCode = entry.branchCode,
                    includedBranchCodes = entry.includedBranchCodes,
                    label = entry.label,
                ),
            )
        }

        runner.run(DefaultApplicationArguments())

        verify(exactly = 0) { repository.save(any<BranchMapping>()) }
    }

    @Test
    @DisplayName("DB 의 IBC 가 SoT 와 다르면 UPDATE")
    fun updatesWhenIbcChanged() {
        val target = BranchMappingMatrix.ALL.first { it.branchCode == "5849" }
        for (entry in BranchMappingMatrix.ALL) {
            val ibc = if (entry.branchCode == "5849") "stale,value" else entry.includedBranchCodes
            every { repository.findById(entry.branchCode) } returns Optional.of(
                BranchMapping(entry.branchCode, ibc, entry.label),
            )
        }
        val captured = slot<BranchMapping>()
        every { repository.save(capture(captured)) } answers { firstArg() }

        runner.run(DefaultApplicationArguments())

        verify(exactly = 1) { repository.save(any<BranchMapping>()) }
        assertThat(captured.captured.branchCode).isEqualTo("5849")
        assertThat(captured.captured.includedBranchCodes).isEqualTo(target.includedBranchCodes)
    }

    @Test
    @DisplayName("DB-only row 보존 — SoT 에 없어도 DELETE 호출 없음")
    fun preservesDbOnlyRows() {
        every { repository.findById(any<String>()) } returns Optional.empty()
        every { repository.save(any<BranchMapping>()) } answers { firstArg() }

        runner.run(DefaultApplicationArguments())

        verify(exactly = 0) { repository.delete(any<BranchMapping>()) }
        verify(exactly = 0) { repository.deleteById(any<String>()) }
        verify(exactly = 0) { repository.deleteAll(any<Iterable<BranchMapping>>()) }
    }
}
