package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.response.DistributionChannelOption
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.platform.common.config.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * 거래처유형마스터 1건의 캐시 표현 — 코드 + 이름.
 *
 * 엔티티([com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster])를 그대로
 * 캐시하지 않는다: `BaseEntity` + LAZY 연관(ownerUser/ownerGroup/createdBy)이 붙어 있어 Redis JSON
 * 직렬화가 프록시를 물고 실패한다. 캐시에는 화면이 필요로 하는 두 값만 담는다.
 */
data class AccountCategoryOption(
    val code: String,
    val name: String,
)

/**
 * 거래처유형마스터 전량(운영 18행)의 Redis 캐시 보관소.
 *
 * `@Cacheable` 은 Spring AOP 프록시 경유 호출에만 적용되므로, 같은 빈 안에서 [options] 를 부르는
 * 파생 API 를 두면 캐시가 조용히 무시된다. 그래서 캐시 메서드만 별도 빈으로 분리하고, 파생 뷰는
 * [AccountCategoryLookup] 이 이 빈을 주입받아 제공한다.
 */
@Service
class AccountCategoryCatalog(
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository,
) {

    /**
     * 거래처유형마스터 전량 — 코드 오름차순, 미삭제 + 코드·이름 모두 보유한 행만.
     *
     * 원천은 SAP 거래처 카테고리 마스터 인바운드(`POST /api/v1/sap/account-category`)로만 갱신되는
     * 전역 코드 체계라 Organization 캐시군과 동일한 "24h TTL + 적재 직후 @CacheEvict" 패턴을 쓴다.
     * 무효화 지점은 [AccountCategoryUpsertService.upsert].
     *
     * 코드는 `"01"`~`"20"` 처럼 zero-padded 고정폭이라 사전순 정렬 = 숫자순 정렬이다.
     */
    @Cacheable(value = [CacheConfig.CACHE_ACCOUNT_CATEGORY_MASTER], key = "'ALL'")
    fun options(): List<AccountCategoryOption> =
        accountCategoryMasterRepository.findAll()
            .filter { it.isDeleted != true }
            .mapNotNull { master ->
                val code = master.accountCode?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = master.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AccountCategoryOption(code = code, name = name)
            }
            .sortedBy { it.code }
}

/**
 * 유통형태(거래처유형) 라벨·필터의 단일 진실원천.
 *
 * ## 배경 — 왜 Account 단독으로 못 만드는가
 *
 * 거래처유형코드는 `Account` 에 없다. 거래처(`Account.accountType`)는 거래처유형마스터의 **이름**만
 * raw 로 보관하고(SAP `AccountType` 그대로), 코드는 마스터(`AccountCategoryMaster.accountCode`)에만
 * 있다. 레거시 SF 도 `Account.Type` ↔ `AccountCategoryMaster__c.Name` 이름 매칭으로 코드를 얻는다
 * (`SalesComparisonSearchController.cls:91-106`, `MonthlyInputAdequacyController.cls:252-256`).
 *
 * 과거 구현은 `거래처상태코드 + 거래처유형명` 을 유통형태로 표기했는데, 거래처상태코드는 값이
 * 01/02/03 세 종류뿐인 **거래처 상태** 축이라 유형코드와 무관하다. 그 결과 같은 유형이 상태코드별로
 * 3개 항목으로 쪼개져(예 `01 슈퍼` / `02 슈퍼` / `03 슈퍼`) 하나만 고르면 나머지가 조용히 누락됐고,
 * 붙은 숫자도 마스터 코드(슈퍼 = 06)와 달랐다. 본 클래스가 그 규칙을 대체한다.
 *
 * ## 사용법
 *
 * 목록 렌더처럼 행마다 라벨이 필요하면 [directory] 로 조회 사전을 **한 번** 만들어 재사용한다.
 * 행마다 [label] 을 부르면 18행짜리 map 을 행 수만큼 다시 만든다.
 */
@Service
class AccountCategoryLookup(
    private val catalog: AccountCategoryCatalog,
) {

    /** 행 단위 라벨/코드 해소용 조회 사전. 목록 1회 조회당 한 번만 만들어 재사용한다. */
    fun directory(): AccountCategoryDirectory = AccountCategoryDirectory(catalog.options())

    /** 조회조건 드롭다운 옵션 — 코드 오름차순 `{코드, "{코드} {이름}"}`. */
    fun options(): List<DistributionChannelOption> =
        catalog.options().map { DistributionChannelOption(code = it.code, label = it.label()) }

    /** 단건 라벨 해소 — 반복 호출 시에는 [directory] 를 쓸 것. */
    fun label(accountType: String?): String? = directory().label(accountType)

    /** 선택된 거래처유형코드 → `Account.accountType` 매칭용 이름 목록. 미등록 코드는 무시된다. */
    fun namesOf(codes: Collection<String>): List<String> = directory().namesOf(codes)
}

/**
 * 거래처유형마스터 스냅샷 기반 조회 사전 — 이름→라벨 / 코드→이름 두 방향 해소.
 *
 * 이름·코드 모두 마스터에서 unique 하므로 단건 map 으로 다룬다.
 */
class AccountCategoryDirectory(
    val options: List<AccountCategoryOption>,
) {
    private val labelByName: Map<String, String> = options.associate { it.name to it.label() }
    private val nameByCode: Map<String, String> = options.associate { it.code to it.name }
    private val codeByName: Map<String, String> = options.associate { it.name to it.code }

    /**
     * `Account.accountType`(= 마스터 이름) → `"{코드} {이름}"` 라벨.
     *
     * 유형 미지정(null) 거래처와 마스터 미등록 유형은 null 을 돌려준다 — 표시용 폴백("-" 등)은
     * 기존과 동일하게 호출 측(web/엑셀)이 처리한다.
     */
    fun label(accountType: String?): String? {
        val name = accountType?.takeIf { it.isNotBlank() } ?: return null
        return labelByName[name]
    }

    /** 거래처유형코드 목록 → `Account.accountType` 매칭용 이름 목록. 미등록 코드는 결과에서 빠진다. */
    fun namesOf(codes: Collection<String>): List<String> = codes.mapNotNull { nameByCode[it] }

    /** `Account.accountType`(= 마스터 이름) → 거래처유형코드. 종속 매핑 key 를 코드 축으로 맞출 때 쓴다. */
    fun codeOf(accountType: String?): String? {
        val name = accountType?.takeIf { it.isNotBlank() } ?: return null
        return codeByName[name]
    }
}

/** `"{거래처유형코드} {이름}"` — 드롭다운 옵션과 목록 컬럼이 공유하는 유일한 라벨 규칙. */
private fun AccountCategoryOption.label(): String = "$code $name"
