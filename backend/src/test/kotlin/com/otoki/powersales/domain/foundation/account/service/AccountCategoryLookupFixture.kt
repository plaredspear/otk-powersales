package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import io.mockk.every
import io.mockk.mockk

/**
 * 유통형태 테스트 픽스처 — 운영 거래처유형마스터 18행 그대로.
 *
 * 유통형태 라벨/필터를 다루는 테스트는 마스터 조인이 필요하므로 mock 대신 실제 [AccountCategoryLookup]
 * 에 이 픽스처를 물려 쓴다. 코드↔이름 대응이 운영과 같아야 회귀를 잡을 수 있다
 * (예: 슈퍼는 06 이고, 과거 버그 라벨이던 "02 슈퍼" 는 만들어질 수 없다 — 02 는 체인).
 */
object AccountCategoryLookupFixture {

    /** 운영 마스터 실값 (account_code to name). */
    val MASTER: List<Pair<String, String>> = listOf(
        "01" to "대형마트(3대)",
        "02" to "체인",
        "03" to "백화점",
        "04" to "C.V.S",
        "05" to "농협",
        "06" to "슈퍼",
        "07" to "대리점",
        "08" to "홀세일",
        "09" to "편의점",
        "10" to "식자재",
        "11" to "단체급식",
        "12" to "유지베이커리",
        "13" to "외식",
        "14" to "제조",
        "15" to "군납",
        "16" to "기타",
        "19" to "온라인",
        "20" to "수출",
    )

    /** 운영 마스터 전량이 적재된 lookup. */
    fun lookup(): AccountCategoryLookup {
        val repository = mockk<AccountCategoryMasterRepository>()
        every { repository.findAll() } returns MASTER.map { (code, name) ->
            AccountCategoryMaster(accountCode = code, name = name, isDeleted = false)
        }
        return AccountCategoryLookup(AccountCategoryCatalog(repository))
    }

    /** `"{코드} {이름}"` 라벨 — 기대값을 손으로 적지 않도록. */
    fun label(name: String): String =
        MASTER.first { it.second == name }.let { "${it.first} ${it.second}" }
}
