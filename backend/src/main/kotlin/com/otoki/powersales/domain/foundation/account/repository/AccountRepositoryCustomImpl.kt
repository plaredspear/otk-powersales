package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.QAccount
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.foundation.account.policy.GeocodeRetryPolicy
import com.otoki.powersales.domain.sales.entity.QMonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.user.entity.QUser.Companion.user
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class AccountRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : AccountRepositoryCustom {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun findAllAccessibleByPolicy(
        policyPredicate: Predicate,
        keyword: String?,
        abcType: String?,
        accountType: String?,
        accountStatusName: String?,
        applyPromotionFilter: Boolean,
        excludeClosedAccount: Boolean,
        coordinatesMissing: Boolean,
        pageable: Pageable,
    ): Page<Account> {
        val builder = BooleanBuilder()

        builder.and(notDeleted())
        builder.and(policyPredicate)
        // "좌표 미수신" 필터 — Naver Geocode batch(#637) 진입 후보와 동일 조건을 AND 합성.
        // 배치가 매번 스캔·재시도하는 거래처를 거래처 화면에서 운영자가 직접 조회하기 위함.
        if (coordinatesMissing) {
            builder.and(coordinatesMissingPredicate())
        }
        // SF AccId__c.lookupFilter 는 Promotion 거래처 선택 Lookup 에만 존재 — 메인 거래처 탭 listView
        // (AllAccounts=Everything) 에는 미적용. 따라서 lookup 진입점만 AND 합성.
        // 폐업 배제(+ 최근 매출 예외) 는 lookupFilter 와 한 절로 합성한다 ([lookupGating] KDoc 참조).
        if (applyPromotionFilter || excludeClosedAccount) {
            builder.and(lookupGating(applyPromotionFilter, excludeClosedAccount))
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
        // 상세 응답이 소유자명(ownerUser.name) / 상위 거래처명(parent.name) 을 노출하므로 fetchJoin 으로
        // 함께 로드한다. bytecode enhancement 환경의 @ManyToOne(LAZY) 는 readOnly tx 안에서도 미초기화
        // 상태로 남아 DTO 매핑 시 null 이 되므로, 단순 leftJoin 이 아닌 fetchJoin 이 필요하다.
        val parentAccount = QAccount("parentAccount")
        return queryFactory
            .selectFrom(account)
            .leftJoin(account.ownerUser, user).fetchJoin()
            .leftJoin(account.parent, parentAccount).fetchJoin()
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
            // 배치 후보 = 좌표 미수신 AND 영구 실패로 마킹되지 않은 것만 (무한 재시도 억제).
            // 화면 필터(coordinatesMissing)는 영구 실패 건도 운영자가 봐야 하므로 이 제외를 적용하지 않는다.
            .where(coordinatesMissingPredicate(), geocodeRetryable())
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

    override fun findDistinctDistributionAbcPairs(): List<AccountDistributionAbcPairRow> {
        return queryFactory
            .select(account.accountStatusCode, account.accountType, account.abcTypeCode, account.abcType)
            .from(account)
            .where(notDeleted())
            .distinct()
            .orderBy(
                account.accountStatusCode.asc(),
                account.accountType.asc(),
                account.abcTypeCode.asc(),
                account.abcType.asc(),
            )
            .fetch()
            .map { tuple ->
                AccountDistributionAbcPairRow(
                    accountStatusCode = tuple.get(account.accountStatusCode),
                    accountType = tuple.get(account.accountType),
                    abcTypeCode = tuple.get(account.abcTypeCode),
                    abcType = tuple.get(account.abcType),
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

    override fun findSnapshotByKeyset(cursor: Long?, limit: Int): List<AccountSnapshotRow> {
        val where = BooleanBuilder()
            // soft delete 제외 (SF 자동 제외 정합 — MFEIS 스냅샷과 동일 규약)
            .and(account.isDeleted.isFalse.or(account.isDeleted.isNull))

        // keyset 커서 — 직전 페이지 마지막 id 초과분만. null 이면 처음부터.
        if (cursor != null) {
            where.and(account.id.gt(cursor))
        }

        // 관계 FK 는 `account.parent.id` 형태로 **FK 컬럼만** select 한다 — 연관 엔티티를 join 하지 않으므로
        // 결과 row 수도 늘지 않고, 호출 측이 entity 의 LAZY 필드에 의존할 필요도 없어진다.
        return queryFactory
            .select(
                account,
                account.ownerUser.id,
                account.createdBy.id,
                account.lastModifiedBy.id,
                account.parent.id,
            )
            .from(account)
            .where(where)
            .orderBy(account.id.asc())
            .limit(limit.toLong())
            .fetch()
            .map { tuple ->
                AccountSnapshotRow(
                    account = tuple.get(account)!!,
                    ownerUserId = tuple.get(account.ownerUser.id),
                    createdById = tuple.get(account.createdBy.id),
                    lastModifiedById = tuple.get(account.lastModifiedBy.id),
                    parentId = tuple.get(account.parent.id),
                )
            }
    }

    /**
     * 고급 검색 필터 드롭다운 distinct 조회의 공통 WHERE — 실제 검색 결과와 동일 게이팅.
     * notDeleted + 지점 스코프(predicate) + promotionLookupFilter + 폐업 제외(최근 매출 예외 포함).
     *
     * 폐업 예외를 [findAllAccessibleByPolicy] 와 동일하게 적용해야 드롭다운과 검색 결과가 어긋나지
     * 않는다 — 예외로 노출된 폐업 거래처가 결과에 있는데 「거래상태」 선택지에 '폐업' 이 없으면
     * 그 거래처를 상태로 좁혀 찾을 수 없다.
     */
    private fun lookupFilterWhere(predicate: Predicate): BooleanBuilder =
        BooleanBuilder()
            .and(notDeleted())
            .and(predicate)
            .and(lookupGating(applyPromotion = true, excludeClosed = true))

    private fun notDeleted() = account.isDeleted.isNull.or(account.isDeleted.eq(false))

    /**
     * Naver Geocode batch(#637) 진입 후보 조건 — 좌표 미수신 거래처.
     *
     * `(latitude IS NULL OR longitude IS NULL) AND address1 IS NOT NULL AND external_key IS NOT NULL`.
     *
     * **레거시 이탈 — 거래처상태 필터 제거** (`legacy-deviation.md` §6 외부 연동).
     * 레거시 SOQL(`Batch_AccountLatLong.cls#start`) 은 `AccountStatusName__c = '거래'` 로 후보를
     * 좁혀 `출고중지` / `폐업` 거래처는 좌표를 영구히 못 받았다. 특히 SAP 인바운드가 주소 변경을
     * 수신하면 [AccountUpsertMapper] 가 상태와 무관하게 좌표를 null 로 무효화하는데, 재취득 배치가
     * `거래` 만 보므로 그 거래처는 좌표 없는 상태로 잔존했다. 상태와 무관하게 좌표를 갱신하도록
     * 조건을 제거한다 — 좌표는 거래 가능 여부와 무관한 위치 속성이고, 출고중지/폐업 거래처도
     * 출근 등록(GPS 거리 검증) / 지도 표시 대상이 될 수 있다.
     *
     * [findCoordinatesMissingAccounts] (배치 후보 조회) 와 [findAllAccessibleByPolicy] 의
     * `coordinatesMissing` 필터(거래처 화면 "좌표 미수신" 조회) 가 동일 조건을 쓰도록 단일 출처로 추출.
     */
    private fun coordinatesMissingPredicate() =
        account.latitude.isNull.or(account.longitude.isNull)
            .and(account.address1.isNotNull)
            .and(account.externalKey.isNotNull)

    /**
     * 좌표변환 실패 상한에 도달하지 않은 거래처 — `geocode_fail_count < GeocodeRetryPolicy.MAX_FAIL_COUNT`.
     *
     * 배치 재조회 후보를 "재시도 가치가 있는 것"으로 좁혀 무한 재시도를 억제한다. 주소로 좌표를 못 찾은
     * 거래처는 [AccountNaverGeocodeService.enrichSingleAccount] 가 카운터를 올리고, 주소(address1)가
     * 바뀌면([AccountUpsertMapper.invalidateCoordinatesIfAddressChanged]) 0 으로 초기화되어 재진입한다.
     */
    private fun geocodeRetryable() =
        account.geocodeFailCount.lt(GeocodeRetryPolicy.MAX_FAIL_COUNT)

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

    /**
     * lookup 진입점 게이팅 — `promotionLookupFilter` + 폐업 배제(최근 매출 예외 포함) 를 한 절로 합성.
     *
     * 두 조건을 따로 AND 하지 않고 함께 조립하는 이유는 **폐업 예외 EXISTS 서브쿼리를 쿼리당 1회로
     * 유지**하기 위해서다. 폐업 예외를 [promotionLookupFilter] 의 면제 항목에도 OR 로 넣으면 EXISTS 가
     * content/count 쿼리마다 2회씩 인라인될 뿐 아니라, `applyPromotionFilter` 만 켠 다른 진입점
     * (물류 클레임 / 유통기한·재고조회 lookup) 까지 폐업 노출이 확대되는 부수 효과가 생긴다.
     *
     * 게이팅 조합별 결과:
     * - [excludeClosed] = true (행사마스터 / 진열사원스케줄 lookup): 폐업은 `distribution` 면제 없이
     *   배제하되 **당월·전월 매출 보유 거래처만 예외 노출** ([recentSalesExists]). `distribution` 면제항은
     *   어차피 뒤이어 폐업 배제로 무효화되므로(`A AND (A OR d)` = `A`) 아예 넣지 않는다.
     * - [applyPromotion] = true 단독 (물류 클레임 / 유통기한·재고조회 lookup): SF lookupFilter 원본 그대로
     *   — 종전 동작 유지 (매출 예외 미적용).
     */
    private fun lookupGating(applyPromotion: Boolean, excludeClosed: Boolean): BooleanExpression {
        if (!excludeClosed) return promotionLookupFilter()

        val notClosedOrHasRecentSales = account.accountStatusName.ne(ACCOUNT_STATUS_CLOSED)
            .or(account.accountStatusName.isNull)
            .or(recentSalesExists())

        return if (applyPromotion) {
            account.accountGroup.`in`(ACCOUNT_GROUP_SALES_VALUES).and(notClosedOrHasRecentSales)
        } else {
            notClosedOrHasRecentSales
        }
    }

    /**
     * 조회 시점 기준 **당월 또는 전월**에 마감실적(> 0)이 있는 월매출 이력 존재 여부 (EXISTS 서브쿼리).
     *
     * 기준월은 행사/스케줄의 대상 기간이 아니라 **조회 시점의 시스템 현재월**이다 — lookup 진입점마다
     * 기준월을 달리 넘기지 않아도 되고, 두 화면(행사마스터 / 진열사원스케줄)이 동일하게 동작한다.
     *
     * `monthly_sales_history` 는 (`sales_year`, `sales_month`) picklist 문자열 2컬럼으로 매출월을
     * 보유하므로, 당월/전월 각각을 (년, 월) 쌍으로 만들어 OR 매칭한다 (연말·연초 경계에서 전월이
     * 전년도가 되는 케이스를 쌍 단위 매칭으로 흡수 — 년 IN × 월 IN 의 cartesian 오매칭이 없다).
     * SF soft-delete row (`is_deleted = true`) 는 제외한다.
     */
    private fun recentSalesExists(): BooleanExpression {
        val today = LocalDate.now()
        val yearMonthPairs = listOf(today, today.minusMonths(1))
            .mapNotNull { date ->
                val year = SalesYear.fromValueOrNull("%04d".format(date.year)) ?: return@mapNotNull null
                val month = SalesMonth.fromValueOrNull("%02d".format(date.monthValue)) ?: return@mapNotNull null
                year to month
            }
        // SalesYear picklist 는 2019~2030 범위라 그 밖의 시스템 시각에서는 후보가 비어 예외가 무효화된다.
        // 조용히 꺼지면 "폐업인데 매출이 있는데도 안 나온다" 로만 보이므로 원인을 로그로 남긴다.
        if (yearMonthPairs.isEmpty()) {
            log.warn("SalesYear picklist 범위 밖 시스템 시각 — 폐업 거래처 최근 매출 예외가 무효화됨: {}", today)
            return Expressions.asBoolean(false).isTrue
        }

        val history = QMonthlySalesHistory.monthlySalesHistory
        val monthMatch = yearMonthPairs
            .map { (year, month) -> history.salesYear.eq(year).and(history.salesMonth.eq(month)) }
            .reduce { acc, expr -> acc.or(expr) }

        return JPAExpressions.selectOne()
            .from(history)
            .where(
                history.account.eq(account),
                history.isDeleted.isNull.or(history.isDeleted.eq(false)),
                monthMatch,
                history.abcClosingSumAmount.coalesce(0.0).add(history.shipClosingSumAmount.coalesce(0.0)).gt(0.0),
            )
            .exists()
    }

    companion object {
        private const val ACCOUNT_STATUS_CLOSED = "폐업"
        private val ACCOUNT_GROUP_SALES_VALUES = listOf("1000", "1010")
    }
}
