package com.otoki.powersales.sales.sfsync

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.sales.repository.SalesProgressRateMasterRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SalesProgressRateMasterSyncService 테스트")
class SalesProgressRateMasterSyncServiceTest {

    private val fetchClient: SalesProgressRateMasterFetchClient = mockk()
    private val repository: SalesProgressRateMasterRepository = mockk()
    private val accountRepository: AccountRepository = mockk()

    private val service = SalesProgressRateMasterSyncService(
        fetchClient = fetchClient,
        repository = repository,
        accountRepository = accountRepository,
    )

    private val savedSlot = slot<List<SalesProgressRateMaster>>()

    @BeforeEach
    fun setUp() {
        every { repository.saveAll(capture(savedSlot)) } answers { firstArg<List<SalesProgressRateMaster>>() }
        every { accountRepository.findByExternalKeyIn(any()) } returns emptyList()
        every { repository.findByExternalKeyIn(any()) } returns emptyList()
    }

    @Test
    @DisplayName("빈 fetch 결과는 no-op (saveAll 미호출)")
    fun emptyFetchIsNoop() {
        val result = service.syncRecords(emptyList())

        assertThat(result.fetched).isEqualTo(0)
        verify(exactly = 0) { repository.saveAll(any<List<SalesProgressRateMaster>>()) }
    }

    @Test
    @DisplayName("기존 row 가 없으면 INSERT — 4채널 목표/실적/지점 컬럼이 적재된다")
    fun insertsNewRecord() {
        val dto = dto(
            externalKey = "202631025008",
            year = "2026", month = "3", accountCode = "1025008",
            rt = 100.0, fr = 200.0, rm = 300.0, fo = 400.0,
            current = 500.0, previous = 450.0, businessRate = 60.0,
        )
        every { fetchClient.fetch() } returns listOf(dto)

        val result = service.sync()

        assertThat(result.inserted).isEqualTo(1)
        assertThat(result.updated).isEqualTo(0)
        val saved = savedSlot.captured.single()
        assertThat(saved.externalKey).isEqualTo("202631025008")
        assertThat(saved.rtTargetAmount).isEqualTo(100.0)
        assertThat(saved.foTargetAmount).isEqualTo(400.0)
        assertThat(saved.currentMonthSalesAmount).isEqualTo(500.0)
        assertThat(saved.previousMonthSalesAmount).isEqualTo(450.0)
        assertThat(saved.businessRate).isEqualTo(60.0)
    }

    @Test
    @DisplayName("기존 row 가 있으면 UPDATE — 동일 인스턴스의 목표/실적이 갱신된다")
    fun updatesExistingRecord() {
        val existing = SalesProgressRateMaster(
            id = 7L,
            name = "SPR-00000007",
            externalKey = "202631025008",
            targetYear = "2026",
            targetMonth = "3",
            rtTargetAmount = 1.0,
            currentMonthSalesAmount = 1.0,
        )
        every { repository.findByExternalKeyIn(any()) } returns listOf(existing)

        val dto = dto(
            externalKey = "202631025008",
            year = "2026", month = "3", accountCode = "1025008",
            rt = 999.0, current = 888.0,
        )

        val result = service.syncRecords(listOf(dto))

        assertThat(result.updated).isEqualTo(1)
        assertThat(result.inserted).isEqualTo(0)
        val saved = savedSlot.captured.single()
        assertThat(saved.id).isEqualTo(7L)
        assertThat(saved.rtTargetAmount).isEqualTo(999.0)
        assertThat(saved.currentMonthSalesAmount).isEqualTo(888.0)
    }

    @Test
    @DisplayName("거래처코드로 Account FK 를 resolve 하여 연결한다")
    fun resolvesAccountByCode() {
        val account = Account(id = 100, sfid = "001AAAAAAAAAAAAAAA", name = "GS25 역삼점").also {
            it.externalKey = "1025008"
        }
        every { accountRepository.findByExternalKeyIn(listOf("1025008")) } returns listOf(account)

        val dto = dto(externalKey = "202631025008", year = "2026", month = "3", accountCode = "1025008")

        service.syncRecords(listOf(dto))

        val saved = savedSlot.captured.single()
        assertThat(saved.account).isSameAs(account)
        assertThat(saved.accountSfid).isEqualTo("001AAAAAAAAAAAAAAA")
    }

    @Test
    @DisplayName("INSERT 시 거래처 미매칭이면 account=null 로 적재한다")
    fun insertsWithNullAccountWhenUnmatched() {
        // accountRepository 가 빈 결과 → 미매칭 (setUp 기본 stub)
        val dto = dto(externalKey = "202631025008", year = "2026", month = "3", accountCode = "9999999")

        service.syncRecords(listOf(dto))

        val saved = savedSlot.captured.single()
        assertThat(saved.account).isNull()
        assertThat(saved.accountSfid).isNull()
    }

    @Test
    @DisplayName("UPDATE 시 거래처 미매칭이면 기존 account FK 를 보존한다")
    fun preservesExistingAccountFkWhenUnmatchedOnUpdate() {
        val existingAccount = Account(id = 55, sfid = "001EXISTINGGGGGGGG", name = "기존거래처").also {
            it.externalKey = "1025008"
        }
        val existing = SalesProgressRateMaster(
            id = 7L,
            externalKey = "202631025008",
            targetYear = "2026",
            targetMonth = "3",
        ).also {
            it.account = existingAccount
            it.accountSfid = "001EXISTINGGGGGGGG"
        }
        every { repository.findByExternalKeyIn(any()) } returns listOf(existing)

        // fetch 의 accountCode 가 미매칭 (accountRepository 빈 결과)
        val dto = dto(externalKey = "202631025008", year = "2026", month = "3", accountCode = "9999999", rt = 777.0)

        service.syncRecords(listOf(dto))

        val saved = savedSlot.captured.single()
        assertThat(saved.rtTargetAmount).isEqualTo(777.0)
        // 기존 FK 보존
        assertThat(saved.account).isSameAs(existingAccount)
        assertThat(saved.accountSfid).isEqualTo("001EXISTINGGGGGGGG")
    }

    @Test
    @DisplayName("ExternalKey 가 비어 있으면 연+월+거래처코드 로 재조합한다")
    fun rebuildsExternalKeyWhenBlank() {
        val dto = dto(externalKey = null, year = "2026", month = "3", accountCode = "1025008")

        service.syncRecords(listOf(dto))

        val saved = savedSlot.captured.single()
        assertThat(saved.externalKey).isEqualTo("202631025008")
    }

    @Test
    @DisplayName("ExternalKey 산출 불가(연/월/거래처코드 결손) row 는 skip 한다")
    fun skipsRecordWithoutResolvableKey() {
        val good = dto(externalKey = "202631025008", year = "2026", month = "3", accountCode = "1025008")
        val bad = dto(externalKey = null, year = null, month = "3", accountCode = "1025008")

        val result = service.syncRecords(listOf(good, bad))

        assertThat(result.fetched).isEqualTo(2)
        assertThat(result.inserted).isEqualTo(1)
        assertThat(result.skipped).isEqualTo(1)
        assertThat(savedSlot.captured).hasSize(1)
    }

    @Test
    @DisplayName("동일 ExternalKey 가 fetch 응답에 중복이면 마지막 값으로 dedupe 한다")
    fun dedupesByExternalKey() {
        val first = dto(externalKey = "202631025008", year = "2026", month = "3", accountCode = "1025008", rt = 1.0)
        val last = dto(externalKey = "202631025008", year = "2026", month = "3", accountCode = "1025008", rt = 2.0)

        val result = service.syncRecords(listOf(first, last))

        assertThat(result.inserted).isEqualTo(1)
        assertThat(savedSlot.captured.single().rtTargetAmount).isEqualTo(2.0)
    }

    private fun dto(
        externalKey: String?,
        year: String?,
        month: String?,
        accountCode: String?,
        rt: Double? = null,
        fr: Double? = null,
        rm: Double? = null,
        fo: Double? = null,
        current: Double? = null,
        previous: Double? = null,
        businessRate: Double? = null,
    ) = SalesProgressRateMasterFetchDto(
        sfid = "a01AAAAAAAAAAAAAAA",
        name = "SPR-00000001",
        externalKey = externalKey,
        targetYear = year,
        targetMonth = month,
        accountCode = accountCode,
        rtTargetAmount = rt,
        frTargetAmount = fr,
        rmTargetAmount = rm,
        foTargetAmount = fo,
        targetSumAmount = null,
        currentMonthSalesAmount = current,
        previousMonthSalesAmount = previous,
        businessRate = businessRate,
        accountBranchView = null,
        accountBranchCode = null,
        isDeleted = false,
    )
}
