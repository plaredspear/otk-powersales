package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.user.entity.QUser.Companion.user
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class AccountRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : AccountRepositoryCustom {

    override fun findAllAccessibleByPolicy(
        policyPredicate: Predicate,
        keyword: String?,
        abcType: String?,
        accountType: String?,
        accountStatusName: String?,
        applyPromotionFilter: Boolean,
        excludeClosedAccount: Boolean,
        pageable: Pageable,
    ): Page<Account> {
        val builder = BooleanBuilder()

        builder.and(notDeleted())
        builder.and(policyPredicate)
        // SF AccId__c.lookupFilter 는 Promotion 거래처 선택 Lookup 에만 존재 — 메인 거래처 탭 listView
        // (AllAccounts=Everything) 에는 미적용. 따라서 lookup 진입점만 AND 합성.
        if (applyPromotionFilter) {
            builder.and(promotionLookupFilter())
        }
        // 진열사원스케줄 마스터 등록 거래처 lookup 전용 — 폐업 거래처는 등록이 차단되므로 조회에서도
        // 완전 제외 (promotionLookupFilter 의 distribution 면제 노출과 무관하게 AND 로 폐업 배제).
        if (excludeClosedAccount) {
            builder.and(account.accountStatusName.ne(ACCOUNT_STATUS_CLOSED).or(account.accountStatusName.isNull))
        }

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            // SF 고급 검색(Enhanced Lookup)은 검색창 1개로 결과 그리드의 여러 컬럼을 relevance 매칭한다.
            // 신규는 keyword 단일 입력을 거래처명/SAP코드/전화/대표자명/주소/거래처지점명 OR 매칭으로 근사.
            builder.and(
                account.externalKey.lower().like(lowerPattern)
                    .or(account.name.lower().like(lowerPattern))
                    .or(account.phone.lower().like(lowerPattern))
                    .or(account.representative.lower().like(lowerPattern))
                    .or(account.address1.lower().like(lowerPattern))
                    .or(account.branchName.lower().like(lowerPattern))
            )
        }

        if (!abcType.isNullOrBlank()) {
            builder.and(account.abcType.eq(abcType))
        }

        if (!accountType.isNullOrBlank()) {
            builder.and(account.accountType.eq(accountType))
        }

        if (!accountStatusName.isNullOrBlank()) {
            builder.and(account.accountStatusName.eq(accountStatusName))
        }

        val content = queryFactory
            .selectFrom(account)
            // policyPredicate 의 owner/hierarchy 절이 ownerUser 를 참조하므로 명시 leftJoin 으로
            // 선언해 암묵 INNER JOIN 을 차단한다. 누락 시 owner_user_id NULL 행이 OR 의 다른
            // 절(cost_center_code 등)로 통과해야 함에도 전부 누락된다.
            // fetchJoin 으로 소유자(ownerUser)를 함께 로드 — AccountListItem.ownerName 이 LAZY
            // 접근 시 유발하던 N+1 을 제거 (고급 검색 결과 그리드 소유자 컬럼용).
            .leftJoin(account.ownerUser, user).fetchJoin()
            .where(builder)
            .orderBy(account.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(account.count())
            .from(account)
            .leftJoin(account.ownerUser, user)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findAccessibleByPolicyAndId(policyPredicate: Predicate, id: Long): Account? {
        return queryFactory
            .selectFrom(account)
            .leftJoin(account.ownerUser, user)
            .where(
                notDeleted(),
                policyPredicate,
                account.id.eq(id)
            )
            .fetchOne()
    }

    override fun findCoordinatesMissingAccounts(limit: Int): List<Account> {
        return queryFactory
            .selectFrom(account)
            .where(
                account.latitude.isNull.or(account.longitude.isNull),
                account.address1.isNotNull,
                account.externalKey.isNotNull,
                account.accountStatusName.eq(ACCOUNT_STATUS_ACTIVE)
            )
            .limit(limit.toLong())
            .fetch()
    }

    override fun existsActiveByName(name: String): Boolean {
        val found = queryFactory
            .selectOne()
            .from(account)
            .where(account.name.eq(name), notDeleted())
            .fetchFirst()
        return found != null
    }

    override fun findActiveById(id: Long): Account? {
        return queryFactory
            .selectFrom(account)
            .where(account.id.eq(id), notDeleted())
            .fetchOne()
    }

    override fun existsActiveByNameAndIdNot(name: String, id: Long): Boolean {
        val found = queryFactory
            .selectOne()
            .from(account)
            .where(account.name.eq(name), account.id.ne(id), notDeleted())
            .fetchFirst()
        return found != null
    }

    override fun findByBranchCodeInAndExternalKeyIn(
        branchCodes: Collection<String>,
        externalKeys: Collection<String>
    ): List<Account> {
        if (branchCodes.isEmpty() || externalKeys.isEmpty()) return emptyList()
        return queryFactory
            .selectFrom(account)
            .where(
                account.branchCode.`in`(branchCodes),
                account.externalKey.`in`(externalKeys)
            )
            .fetch()
    }

    override fun findDistinctDistributionChannelParts(): List<AccountLabelPartsRow> {
        return queryFactory
            .select(account.accountStatusCode, account.accountType)
            .from(account)
            .where(notDeleted())
            .distinct()
            .orderBy(account.accountStatusCode.asc(), account.accountType.asc())
            .fetch()
            .map { tuple ->
                AccountLabelPartsRow(
                    code = tuple.get(account.accountStatusCode),
                    name = tuple.get(account.accountType),
                )
            }
    }

    override fun findDistinctAbcTypeParts(): List<AccountLabelPartsRow> {
        return queryFactory
            .select(account.abcTypeCode, account.abcType)
            .from(account)
            .where(notDeleted())
            .distinct()
            .orderBy(account.abcTypeCode.asc(), account.abcType.asc())
            .fetch()
            .map { tuple ->
                AccountLabelPartsRow(
                    code = tuple.get(account.abcTypeCode),
                    name = tuple.get(account.abcType),
                )
            }
    }

    override fun findDistinctAccountTypes(predicate: Predicate): List<String> {
        return queryFactory
            .select(account.accountType)
            .from(account)
            .where(lookupFilterWhere(predicate).and(account.accountType.isNotNull).and(account.accountType.ne("")))
            .distinct()
            .orderBy(account.accountType.asc())
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctAccountStatusNames(predicate: Predicate): List<String> {
        return queryFactory
            .select(account.accountStatusName)
            .from(account)
            .where(lookupFilterWhere(predicate).and(account.accountStatusName.isNotNull).and(account.accountStatusName.ne("")))
            .distinct()
            .orderBy(account.accountStatusName.asc())
            .fetch()
            .filterNotNull()
    }

    /**
     * 고급 검색 필터 드롭다운 distinct 조회의 공통 WHERE — 실제 검색 결과와 동일 게이팅.
     * notDeleted + 지점 스코프(predicate) + promotionLookupFilter + 폐업 제외.
     */
    private fun lookupFilterWhere(predicate: Predicate): BooleanBuilder =
        BooleanBuilder()
            .and(notDeleted())
            .and(predicate)
            .and(promotionLookupFilter())
            .and(account.accountStatusName.ne(ACCOUNT_STATUS_CLOSED).or(account.accountStatusName.isNull))

    private fun notDeleted() = account.isDeleted.isNull.or(account.isDeleted.eq(false))

    /**
     * SF `DKRetail__Promotion__c.AccId__c.lookupFilter` 동등 비즈니스 필터.
     *
     * booleanFilter `1 AND 2 AND (3 OR (4 AND 5))` 원본:
     * 1. AccountGroup__c equals 1000,1010
     * 2. AccountGroup__c notEqual ""
     * 3. AccountStatusName__c notEqual "폐업"
     * 4. Distribution__c notEqual ""
     * 5. AccountStatusName__c equals "폐업"
     *
     * 정규화: `accountGroup ∈ {1000,1010} AND (accountStatusName != '폐업' OR distribution NON-EMPTY)`
     * (조건 2 는 1 에 흡수)
     */
    private fun promotionLookupFilter() = account.accountGroup.`in`(ACCOUNT_GROUP_SALES_VALUES)
        .and(
            account.accountStatusName.ne(ACCOUNT_STATUS_CLOSED)
                .or(account.accountStatusName.isNull)
                .or(
                    account.distribution.isNotNull
                        .and(account.distribution.ne(""))
                )
        )

    companion object {
        private const val ACCOUNT_STATUS_ACTIVE = "거래"
        private const val ACCOUNT_STATUS_CLOSED = "폐업"
        private val ACCOUNT_GROUP_SALES_VALUES = listOf("1000", "1010")
    }
}
