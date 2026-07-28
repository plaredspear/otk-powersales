package com.otoki.powersales.domain.sales.service

import com.otoki.pos.repository.ElectronicSalesCustomerRow
import com.otoki.pos.repository.LiveTotSalesDailyRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.account.service.AccountCategoryLookup
import com.otoki.powersales.domain.foundation.product.dto.response.ProductLookupFilterOptions
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.service.AdminProductService
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.platform.common.config.CacheConfig
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.domain.sales.dto.request.ElectronicSalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardDetailResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardFilterOptionsResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListItem
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesProductAdvancedItem
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesProductAdvancedResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesProductLookupItem
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 전산실적 대시보드 admin 조회 service.
 *
 * ## 데이터 source
 * POS DB `public.live_tot_sales_dh` ([com.otoki.pos.entity.LiveTotSalesDaily]) 직 SELECT.
 * 거래처 코드는 [Account.externalKey] 에 레거시 패딩 `"000" + accountCode` ([CUST_CD_PREFIX]) 적용.
 *
 * ## 레거시 매핑
 * 레거시 「월 매출 조회(전산)」 = `GET /sales/abcMain` → `promotion/month/abcmain.jsp`.
 * 입력: 기간 + 거래처 → 제품별 `SUM(SALES_RAMT/RQTY)` 집계. SF chain 없음 (POS DB 직조회).
 * 본 service 는 그 화면을 web admin 으로 확장 — 권한 범위 거래처 N건의 기간 전산매출 합계 명세 +
 * 거래처별 제품 상세. [MonthlySalesAdminQueryService] (물류배부) 와 동일한 권한/필터 패턴.
 *
 * ## 기간 산출
 * 레거시 daterangepicker 정합 — 시작일~종료일 일 단위 (`startDate`/`endDate`). 최대 3개월.
 *
 * ## 필터 해소 위치 (외부 POS DB 보호)
 * - 메인 DB(Account): 지점 / 거래처 keyword / 유통형태 / 거래처유형 → POS 의 기존 `CUST_CD IN` 값 축소
 * - 메인 DB(Product): 선택 제품 / 중분류 / 소분류 → 소비자 바코드 목록으로 해소 후 POS `UPC_CD IN`
 *   1개 predicate 로만 합류. 바코드 목록은 [BARCODE_CHUNK_SIZE] 단위 청크 분할로 IN 비대화를 방지.
 *
 * ## graceful fallback
 * POS DB 도달 불가(미설정/VPN 장애) 시 빈 결과로 응답 — [ElectronicSalesService] / ORORA gateway 동일 정책.
 *
 * 부수 효과: 없음 (조회 전용).
 */
@Service
@Transactional(readOnly = true)
class ElectronicSalesAdminQueryService(
    private val accountRepository: AccountRepository,
    private val liveTotSalesDailyRepository: LiveTotSalesDailyRepository,
    private val productRepository: ProductRepository,
    /** 제품 고급 검색 필터 옵션의 분류 트리 재사용 — 제품 관리 화면과 동일한 조립 규칙을 공유한다. */
    private val adminProductService: AdminProductService,
    /** 유통형태 라벨/옵션 정본 (거래처유형마스터 캐시). */
    private val accountCategoryLookup: AccountCategoryLookup,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 거래처별 전산매출 명세 조회 — 페이징 + 정렬 + 필터.
     *
     * 권한 범위 거래처 N건의 기간 전산매출(POS) 합계를 거래처 1행으로 반환. 제품/분류 필터가
     * 지정되면 해당 제품군만 합산. 응답에 조회 결과 전체(페이징 무관) 금액/수량 합계를 포함.
     * 페이징은 조회된 거래처 list 를 메모리에서 sort + slice.
     *
     * @throws AdminForbiddenException 권한 범위와 입력 costCenterCodes 의 교집합이 비어있을 때
     */
    fun getList(scope: DataScope, request: ElectronicSalesDashboardListRequest): ElectronicSalesDashboardListResponse {
        validateParams(request.startDate, request.endDate, request.costCenterCodes)
        val effectiveCodes = applyScope(scope, request.costCenterCodes)

        val items = buildListItems(effectiveCodes, request)
        val sorted = sortItems(items, request.sort)
        val totalElements = sorted.size.toLong()
        val pageSize = request.size.coerceIn(1, 100)
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + pageSize - 1) / pageSize).toInt()
        val fromIndex = (request.page * pageSize).coerceAtLeast(0)
        val toIndex = (fromIndex + pageSize).coerceAtMost(sorted.size)
        val pageItems = if (fromIndex >= sorted.size) emptyList() else sorted.subList(fromIndex, toIndex)

        return ElectronicSalesDashboardListResponse(
            startDate = request.startDate,
            endDate = request.endDate,
            totalSalesAmount = sorted.sumOf { it.salesAmount },
            totalSalesQuantity = sorted.sumOf { it.salesQuantity },
            items = pageItems,
            pageInfo = ElectronicSalesDashboardListResponse.PageInfo(
                page = request.page,
                size = pageSize,
                totalElements = totalElements,
                totalPages = totalPages,
            ),
        )
    }

    /**
     * 엑셀 export 용 전체 명세 산출 (페이징 미적용).
     */
    fun getListForExport(scope: DataScope, request: ElectronicSalesDashboardListRequest): List<ElectronicSalesDashboardListItem> {
        validateParams(request.startDate, request.endDate, request.costCenterCodes)
        val effectiveCodes = applyScope(scope, request.costCenterCodes)
        return sortItems(buildListItems(effectiveCodes, request), request.sort).take(EXPORT_MAX_ROWS)
    }

    /**
     * 단건 거래처 상세 — 제품별 전산매출 명세 (레거시 `SelectAbcData` 동등).
     *
     * 목록과 동일한 기간/제품/분류 필터를 반영해 목록 행 합계와 상세 합계가 정합하도록 한다.
     *
     * @throws AdminForbiddenException 거래처가 권한 범위 밖일 때
     */
    fun getDetail(
        scope: DataScope,
        customerId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        productIds: List<Long> = emptyList(),
        category2: String? = null,
        category3: String? = null,
    ): ElectronicSalesDashboardDetailResponse {
        validateDateRange(startDate, endDate)
        val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
            ?: throw BusinessException(
                errorCode = "ACCOUNT_NOT_FOUND",
                message = "거래처를 찾을 수 없습니다: $customerId",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        if (!scope.validateAccess(account.branchCode)) throw AdminForbiddenException()

        val custCd = toCustCd(account.externalKey)
        val hasProductFilter =
            productIds.isNotEmpty() || !category2.isNullOrBlank() || !category3.isNullOrBlank()
        val barcodes = if (hasProductFilter) resolveBarcodes(productIds, category2, category3) else emptyList()

        val from = startDate.format(DATE_FORMATTER)
        val to = endDate.format(DATE_FORMATTER)
        val products = when {
            custCd == null -> emptyList()
            !hasProductFilter -> runCatching {
                liveTotSalesDailyRepository.aggregateByProduct(custCd, from, to).map { row ->
                    ElectronicSalesDashboardDetailResponse.ProductSales(
                        productCode = row.getItemCd(),
                        productName = row.getItemNm() ?: "",
                        amount = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong(),
                        quantity = (row.getSalesQty() ?: BigDecimal.ZERO).toLong(),
                    )
                }
            }.getOrElse { e ->
                log.warn("POS 전산매출 상세 조회 실패 (custCd={}, {}~{}): {}", custCd, from, to, e.message)
                emptyList()
            }
            barcodes.isEmpty() -> emptyList() // 필터 지정했으나 매칭 바코드 없음 → 매출 없음
            else -> aggregateDetailByBarcodes(custCd, from, to, barcodes)
        }

        return ElectronicSalesDashboardDetailResponse(
            customerId = account.id,
            customerName = account.name,
            sapAccountCode = account.externalKey,
            startDate = startDate,
            endDate = endDate,
            totalAmount = products.sumOf { it.amount },
            totalQuantity = products.sumOf { it.quantity },
            items = products,
        )
    }

    /**
     * 조회 조건 옵션 — 유통형태 / 거래처유형 / 제품 중·소분류 (모두 메인 DB distinct).
     *
     * 옵션 소스(Account 유통형태·거래처유형 + Product 카테고리)는 모두 SAP inbound 하루 1회 적재로만
     * 갱신되는 전역 코드 체계라, 매 화면 진입 시 반복되는 distinct 스캔을 Redis 캐시(24h TTL)로 대체한다.
     * 무인자라 고정 key('ALL'). Account/Product 적재 직후 각 UpsertService 의 @CacheEvict 가 즉시 무효화한다.
     */
    @Cacheable(value = [CacheConfig.CACHE_ELECTRONIC_SALES_FILTER_OPTIONS], key = "'ALL'")
    fun getFilterOptions(): ElectronicSalesDashboardFilterOptionsResponse {
        // 유통형태 목록은 거래처유형마스터가 정본이라 Account distinct 로 만들지 않는다 (코드가 Account 에 없음).
        // 반면 거래처유형(ABC) 목록과 종속 매핑(유통형태 → 실재 거래처유형)은 어떤 조합이 실제로
        // 존재하는지가 Account 에만 있으므로 동시출현 pairs 스캔을 유지한다.
        val distributionChannels = accountCategoryLookup.options()
        val accountCategories = accountCategoryLookup.directory()
        val pairs = accountRepository.findDistinctDistributionAbcPairs()
        val accountTypeSet = sortedSetOf<String>()
        val dependent = linkedMapOf<String, MutableSet<String>>()
        for (pair in pairs) {
            val abcLabel = Account.abcTypeLabel(pair.abcTypeCode, pair.abcType)
            if (abcLabel != null) accountTypeSet.add(abcLabel)
            // 종속 매핑 key 는 유통형태 코드 — 화면이 되돌려 보내는 값과 동일 축으로 맞춘다.
            val distCode = accountCategories.codeOf(pair.accountType)
            if (distCode != null && abcLabel != null) {
                dependent.getOrPut(distCode) { sortedSetOf() }.add(abcLabel)
            }
        }
        val accountTypes = accountTypeSet.toList()
        val dependentAccountTypes = dependent.mapValues { it.value.toList() }
        val categories = productRepository.findCategoryGroups()
            .groupBy { it.category2 }
            .toSortedMap()
            .map { (c2, rows) ->
                ElectronicSalesDashboardFilterOptionsResponse.CategoryGroup(
                    category2 = c2,
                    category3s = rows.mapNotNull { it.category3 }.distinct().sorted(),
                )
            }
        return ElectronicSalesDashboardFilterOptionsResponse(
            distributionChannels = distributionChannels,
            accountTypes = accountTypes,
            categories = categories,
            dependentAccountTypes = dependentAccountTypes,
        )
    }

    /**
     * 조회 조건의 제품 검색 — 제품명/제품코드/소비자 바코드 부분일치 (바코드 보유 제품 한정).
     */
    fun searchProducts(keyword: String): List<ElectronicSalesProductLookupItem> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()
        return productRepository.searchForElectronicSales(trimmed, PRODUCT_LOOKUP_LIMIT)
            .map {
                ElectronicSalesProductLookupItem(
                    productId = it.productId,
                    name = it.name,
                    productCode = it.productCode,
                    barcode = it.barcode ?: "",
                )
            }
    }

    /**
     * 조회 조건의 제품 고급 검색 — 키워드 + 대/중/소분류 + 제품상태 조합 (페이징).
     *
     * [searchProducts] (드롭다운 빠른 검색) 이 상위 [PRODUCT_LOOKUP_LIMIT] 건만 반환해 결과가 많은
     * 키워드에서 뒤쪽 제품에 도달할 수 없는 문제를 해소한다 — 행사마스터 제품 고급 검색과 동일 목적.
     *
     * 대상 집합은 드롭다운과 동일하게 소비자 바코드 보유 제품 한정 이다. 바코드가 없으면
     * POS `UPC_CD IN` 필터에 사용할 수 없어 선택해도 항상 매출 0 건이 되므로, 헛선택을 원천 차단한다.
     *
     * 대분류/제품상태는 목록 조회(`/list`) 의 필터 축에는 없지만, 고급 검색은 필터 자체가 아니라
     * 선택 결과(productIds) 만 목록에 전달하므로 축 불일치 문제가 없다.
     *
     * 응답에 대표 바코드를 포함해 화면이 드롭다운과 동일한 제품 라벨을 만들 수 있게 한다.
     */
    fun searchProductsAdvanced(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        page: Int,
        size: Int,
    ): ElectronicSalesProductAdvancedResponse {
        val pageable = PageRequest.of(page, size)
        val productPage = productRepository.searchForElectronicSalesAdvanced(
            keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
            category1 = category1,
            category2 = category2,
            category3 = category3,
            productStatus = productStatus,
            pageable = pageable,
        )
        return ElectronicSalesProductAdvancedResponse(
            content = productPage.content.map { row ->
                val p = row.product
                ElectronicSalesProductAdvancedItem(
                    id = p.id,
                    productCode = p.productCode,
                    name = p.name,
                    barcode = row.barcode,
                    category1 = p.productCategory1,
                    category2 = p.productCategory2,
                    category3 = p.productCategory3,
                    standardUnitPrice = p.standardUnitPrice,
                    unit = p.unit,
                    storageCondition = p.storageCondition?.displayName,
                    // 저장값이 아니라 화면 표시명 — 값이 없으면 "판매중", `출고중지` 는 "단종".
                    productStatus = p.productStatus?.label ?: ProductStatus.DEFAULT_LABEL,
                    launchDate = p.launchDate?.toString(),
                )
            },
            page = page,
            size = size,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages,
        )
    }

    /**
     * 제품 고급 검색 모달의 필터 드롭다운 옵션 — 대/중/소분류 트리 + 제품상태.
     *
     * 기존 [getFilterOptions] 의 `categories` 는 중/소분류 2단이라 고급 검색의 대분류 필터를 채울 수
     * 없어 별도로 공급한다. `/api/v1/admin/products/lookup-filter-options` 는 promotion.READ 가드라
     * 재사용 불가 — 본 화면 도메인 권한(`monthly_sales_history`) 으로 가드된 endpoint 가 필요하다.
     */
    fun getProductLookupFilterOptions(): ProductLookupFilterOptions {
        return ProductLookupFilterOptions(
            categories = adminProductService.getCategories(),
            productStatuses = ProductStatus.labels(),
        )
    }

    // ------------------- helpers -------------------

    private fun buildListItems(
        effectiveCodes: List<String>,
        request: ElectronicSalesDashboardListRequest,
    ): List<ElectronicSalesDashboardListItem> {
        val accounts = findAccounts(effectiveCodes, request)
        if (accounts.isEmpty()) return emptyList()
        validateAccountCount(accounts.size)

        // 제품/분류 필터 → 소비자 바코드 해소 (메인 DB). 필터 지정 + 매칭 바코드 0건이면
        // POS 를 조회하지 않고 전 거래처 0 으로 응답 (매칭 제품 없음 = 매출 없음).
        val hasProductFilter = request.hasProductFilter()
        val barcodes = if (hasProductFilter) {
            resolveBarcodes(request.productIds, request.category2, request.category3)
        } else {
            emptyList()
        }

        val from = request.startDate.format(DATE_FORMATTER)
        val to = request.endDate.format(DATE_FORMATTER)

        // custCd(legacy 패딩) → account 역매핑으로 POS 합계를 거래처에 결합
        val custCdToAccount = accounts.mapNotNull { acc ->
            toCustCd(acc.externalKey)?.let { it to acc }
        }.toMap()

        val salesByCustCd: Map<String, Pair<Long, Long>> = when {
            custCdToAccount.isEmpty() -> emptyMap()
            hasProductFilter && barcodes.isEmpty() -> emptyMap()
            else -> runCatching {
                aggregateCustomers(custCdToAccount.keys.toList(), from, to, barcodes)
            }.getOrElse { e ->
                log.warn("POS 전산매출 명세 조회 실패 ({}~{}): {}", from, to, e.message)
                emptyMap()
            }
        }

        // 유통형태 라벨 사전 — 행마다 만들지 않도록 목록 1회 조립당 한 번만 생성 (거래처유형마스터 캐시 스냅샷).
        val categories = accountCategoryLookup.directory()
        return accounts.map { account ->
            val custCd = toCustCd(account.externalKey)
            val (amount, quantity) = custCd?.let { salesByCustCd[it] } ?: (0L to 0L)
            ElectronicSalesDashboardListItem(
                accountId = account.id,
                accountName = account.name,
                sapAccountCode = account.externalKey,
                distributionChannel = categories.label(account.accountType),
                accountType = account.abcTypeLabel(),
                branchCode = account.branchCode,
                branchName = account.branchName,
                salesAmount = amount,
                salesQuantity = quantity,
            )
        }
    }

    /**
     * 거래처별 POS 합계 집계 — custCd 목록을 [CUST_CD_CHUNK_SIZE] 청크로 분할 실행 후 병합.
     *
     * 지점별 거래처 목록이 전 지점 선택 시 수만 건이 될 수 있어, 외부(POS) DB 에 전달하는
     * `CUST_CD IN` 크기를 청크로 상한 (bind parameter 비대/플랜 붕괴 방지). 바코드 필터가 있으면
     * [BARCODE_CHUNK_SIZE] 청크와 교차 실행하고 custCd 단위로 병합한다 — custCd 는 청크 간
     * 중복이 없고, 동일 custCd 의 바코드 청크 분할분은 merge 로 합산된다.
     */
    private fun aggregateCustomers(
        custCds: List<String>,
        from: String,
        to: String,
        barcodes: List<String>,
    ): Map<String, Pair<Long, Long>> {
        val merged = mutableMapOf<String, Pair<Long, Long>>()
        fun accumulate(rows: List<ElectronicSalesCustomerRow>) = rows.forEach { row ->
            val amt = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong()
            val qty = (row.getSalesQty() ?: BigDecimal.ZERO).toLong()
            merged.merge(row.getCustCd(), amt to qty) { prev, next ->
                (prev.first + next.first) to (prev.second + next.second)
            }
        }
        custCds.chunked(CUST_CD_CHUNK_SIZE).forEach { custChunk ->
            if (barcodes.isEmpty()) {
                accumulate(liveTotSalesDailyRepository.aggregateByCustomer(custChunk, from, to))
            } else {
                barcodes.chunked(BARCODE_CHUNK_SIZE).forEach { chunk ->
                    accumulate(
                        liveTotSalesDailyRepository.aggregateByCustomerAndBarcodes(custChunk, from, to, chunk),
                    )
                }
            }
        }
        return merged
    }

    /**
     * 상세(제품별) POS 집계 — 바코드 청크 분할 실행 후 제품코드 단위 병합.
     * (`aggregateByProductBarcodes` 는 UPC_CD 단위까지 분해하므로 제품코드로 재합산해 목록 화면
     * 의 제품 단위 표기와 정합.)
     */
    private fun aggregateDetailByBarcodes(
        custCd: String,
        from: String,
        to: String,
        barcodes: List<String>,
    ): List<ElectronicSalesDashboardDetailResponse.ProductSales> = runCatching {
        data class Acc(var name: String, var amount: Long, var quantity: Long)

        val merged = linkedMapOf<String, Acc>()
        barcodes.chunked(BARCODE_CHUNK_SIZE).forEach { chunk ->
            liveTotSalesDailyRepository.aggregateByProductBarcodes(custCd, from, to, chunk)
                .forEach { row ->
                    val acc = merged.getOrPut(row.getItemCd()) { Acc(row.getItemNm() ?: "", 0L, 0L) }
                    acc.amount += (row.getSalesAmt() ?: BigDecimal.ZERO).toLong()
                    acc.quantity += (row.getSalesQty() ?: BigDecimal.ZERO).toLong()
                }
        }
        merged.map { (code, acc) ->
            ElectronicSalesDashboardDetailResponse.ProductSales(
                productCode = code,
                productName = acc.name,
                amount = acc.amount,
                quantity = acc.quantity,
            )
        }.sortedByDescending { it.amount }
    }.getOrElse { e ->
        log.warn("POS 전산매출 상세(제품 필터) 조회 실패 (custCd={}, {}~{}): {}", custCd, from, to, e.message)
        emptyList()
    }

    /** 제품/분류 필터 → 소비자 바코드 목록 해소 (메인 DB Product/ProductBarcode). */
    private fun resolveBarcodes(productIds: List<Long>, category2: String?, category3: String?): List<String> =
        productRepository.findBarcodesForElectronicSales(productIds, category2, category3)

    private fun sortItems(items: List<ElectronicSalesDashboardListItem>, sort: String?): List<ElectronicSalesDashboardListItem> {
        if (sort.isNullOrBlank()) return items.sortedByDescending { it.salesAmount }
        val parts = sort.split(",")
        val field = parts[0].trim()
        val desc = parts.getOrNull(1)?.trim()?.equals("desc", ignoreCase = true) == true
        val comparator: Comparator<ElectronicSalesDashboardListItem> = when (field) {
            "accountName" -> compareBy(nullsLast()) { it.accountName }
            "salesAmount" -> compareBy { it.salesAmount }
            "salesQuantity" -> compareBy { it.salesQuantity }
            else -> compareBy(nullsLast()) { it.accountName }
        }
        return if (desc) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
    }

    private fun findAccounts(
        effectiveCodes: List<String>,
        request: ElectronicSalesDashboardListRequest,
    ): List<Account> {
        val candidates = if (request.accountGroup != null) {
            effectiveCodes.flatMap { code ->
                accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
                    branchCode = code,
                    accountGroups = listOf(request.accountGroup),
                    isDeleted = true,
                )
            }
        } else {
            accountRepository.findByBranchCodeIn(effectiveCodes)
        }
        return candidates
            .let { all ->
                if (request.accountIds.isEmpty()) all else all.filter { it.id in request.accountIds }
            }
            .filter { acc ->
                request.customerKeyword.isNullOrBlank() ||
                    acc.name?.contains(request.customerKeyword, ignoreCase = true) == true
            }
            // 유통형태(거래처유형마스터 코드) / 거래처유형(ABC유형) 필터 — 메인 DB 해소분.
            // 유통형태는 선택 코드를 마스터 이름으로 되돌려 `accountType` 직접 매칭한다 (레거시
            // `Account__r.Type IN :categoryMap.keySet()` 동등). 유형 미지정 거래처는 자연히 제외된다.
            .let { filtered ->
                if (request.distributionChannels.isEmpty()) {
                    filtered
                } else {
                    val names = accountCategoryLookup.namesOf(request.distributionChannels).toSet()
                    filtered.filter { acc -> acc.accountType in names }
                }
            }
            .filter { acc ->
                request.accountTypes.isEmpty() || acc.abcTypeLabel() in request.accountTypes
            }
    }

    /** [Account.externalKey] → POS `CUST_CD` (legacy 패딩 `"000" + accountCode`). blank → null. */
    private fun toCustCd(externalKey: String?): String? =
        if (externalKey.isNullOrBlank()) null else "$CUST_CD_PREFIX$externalKey"

    internal fun applyScope(scope: DataScope, costCenterCodes: List<String>): List<String> {
        if (scope.isAllBranches) return costCenterCodes
        val allowed = scope.branchCodes.toSet()
        val intersect = costCenterCodes.filter { it in allowed }
        if (intersect.isEmpty()) throw AdminForbiddenException()
        return intersect
    }

    /**
     * count 기반 외부 DB 보호 가드 — POS 를 조회하기 전에, 메인 DB 에서 이미 확보한 거래처 목록의
     * 개수(추가 쿼리 없음)로 외부 DB 에 나갈 조회 규모를 확인하고 상한 초과 시 조건 좁힘을 안내한다.
     * 상한까지만 잘라 조회하면 합계가 틀어지므로(silent truncation) 초과분 절단은 하지 않는다.
     */
    private fun validateAccountCount(count: Int) {
        if (count > MAX_ACCOUNT_COUNT) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "조회 대상 거래처가 ${count}건입니다. 최대 ${MAX_ACCOUNT_COUNT}건까지 조회할 수 있습니다. " +
                    "지점/유통형태/거래처유형 조건을 좁혀주세요.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validateParams(startDate: LocalDate, endDate: LocalDate, costCenterCodes: List<String>) {
        validateDateRange(startDate, endDate)
        if (costCenterCodes.isEmpty()) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "cost_center_codes는 필수입니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validateDateRange(startDate: LocalDate, endDate: LocalDate) {
        if (startDate.year !in 2019..2099 || endDate.year !in 2019..2099) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "조회 기간은 2019~2099 범위여야 합니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        if (startDate.isAfter(endDate)) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "시작일은 종료일보다 이후일 수 없습니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        // 외부(POS) DB 일별 테이블 스캔 보호 — 시작일 기준 3개월(포함) 초과 금지.
        if (endDate.isAfter(startDate.plusMonths(MAX_RANGE_MONTHS).minusDays(1))) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "조회 기간은 최대 ${MAX_RANGE_MONTHS}개월까지 가능합니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /** 레거시 `abcmain.jsp` 의 `CUST_CD = "000" + accountCode` 패딩 정합. */
        private const val CUST_CD_PREFIX = "000"

        /** 엑셀 export 최대 행 수 — 초과분은 절단 (web 단 totalCount 경고와 정합). */
        private const val EXPORT_MAX_ROWS = 50_000

        /** 조회 기간 상한 (개월, 시작일 포함 기준). 외부 POS DB 스캔 보호. */
        private const val MAX_RANGE_MONTHS = 3L

        /** POS `UPC_CD IN` 청크 크기 — 외부 DB 에 전달하는 IN 목록 비대화 방지. */
        private const val BARCODE_CHUNK_SIZE = 1_000

        /**
         * POS `CUST_CD IN` 청크 크기 — 쿼리 1회의 집계 부하를 레거시 단일 거래처 조회의 50배
         * 이내로 상한 (실측 기준 1거래처×1달 ≈ 500행 → 쿼리 1회 ≈ 2.5만 행 상당).
         */
        private const val CUST_CD_CHUNK_SIZE = 50

        /**
         * 1회 조회의 거래처 수 상한 — 메인 DB 에서 확보한 거래처 목록 count 로 사전 확인 (추가
         * 쿼리 없음). 초과 시 POS 미조회 + 400 조건 좁힘 안내. 청크 50 기준 최악 50회 쿼리로 상한.
         */
        private const val MAX_ACCOUNT_COUNT = 2_500

        /** 제품 검색 최대 반환 건수. */
        private const val PRODUCT_LOOKUP_LIMIT = 50L
    }
}
