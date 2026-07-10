package com.otoki.powersales.domain.sales.service

import com.otoki.pos.repository.LivePosSalesDailyRepository
import com.otoki.pos.repository.PosCustomerSalesRow
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.sales.dto.request.PosSalesAccountListRequest
import com.otoki.powersales.domain.sales.dto.request.PosSalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.PosSalesAccountItem
import com.otoki.powersales.domain.sales.dto.response.PosSalesAccountListResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesDashboardListItem
import com.otoki.powersales.domain.sales.dto.response.PosSalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * POS매출 대시보드 admin 조회 service — 거래처별 POS매출 합계 명세 + 거래처별 제품 상세.
 *
 * ## 데이터 source
 * POS DB `public.live_pos_sales_dh` ([com.otoki.pos.entity.LivePosSalesDaily]) 직 SELECT.
 * 거래처 코드는 [Account.externalKey] 에 레거시 패딩 `"000" + accountCode` ([CUST_CD_PREFIX]) 적용.
 *
 * ## 레거시 매핑
 * 레거시 「POS매출 조회」 = `GET /sales/posMain` → `promotion/month/posmain.jsp`.
 * 입력: 거래처 1곳 + 기간(최대 31일) → 제품별 `SUM(SALES_AMT/QTY)` 집계. SF chain 없음.
 * 본 service 는 그 화면을 거래처별 명세로 확장 — 전산실적 [ElectronicSalesAdminQueryService] 와
 * 동일한 권한/필터 패턴. 단일 거래처 제품별 조회(mobile)는 [PosSalesService] 가 그대로 담당.
 *
 * ## 2단 조회 (외부 POS DB 부하 축소)
 * POS 는 메인 RDS 와 분리된 외부 read-only DB 라, 지점 전 거래처를 매 조회마다 집계하면 왕복이
 * 급증한다. 이를 막기 위해 조회를 2단으로 분리한다:
 * - **1단 [getAccounts]**: 메인 DB Account 만으로 조건에 맞는 거래처 목록 반환 (POS 미접촉 → 즉시 응답).
 * - **2단 [getList]**: 운영자가 선택한 거래처(accountIds, 최대 [MAX_SELECTABLE_ACCOUNTS])만 외부 POS
 *   DB 로 집계. 상단 합계도 선택 거래처 기준. → POS `CUST_CD IN` 왕복이 최대 1회로 축소.
 *
 * ## 기간 산출
 * 레거시 daterangepicker `maxSpan: { days: 31 }` 정합 — 시작일~종료일 일 단위, 두 끝점 일수 차이
 * 최대 [MAX_RANGE_DAYS]. 거래처 N건 × 일별 스캔이라 전산실적(3개월)보다 보수적 상한을 유지한다.
 *
 * ## 필터 해소 위치 (외부 POS DB 보호)
 * - 1단 메인 DB(Account): 지점 / 거래처 keyword / 유통형태 / 거래처유형 → 거래처 목록 축소
 * - 2단 메인 DB(Product): 선택 제품 / 중분류 / 소분류 → 소비자 바코드 목록으로 해소 후 POS 의 기존
 *   `BARCODE IN` predicate 로만 합류. 바코드 목록은 [BARCODE_CHUNK_SIZE] 단위 청크 분할로 IN
 *   비대화를 방지. 유통형태/거래처유형/중·소분류 옵션과 제품 검색은 전산실적 endpoint
 *   (`/api/v1/admin/sales/electronic/filter-options`, `/product-lookup`) 를 재사용 (동일 권한 entity).
 *
 * ## graceful fallback
 * POS DB 도달 불가(미설정/VPN 장애) 시 빈 결과로 응답 — [PosSalesService] 와 동일 정책.
 *
 * 부수 효과: 없음 (조회 전용).
 */
@Service
@Transactional(readOnly = true)
class PosSalesAdminQueryService(
    private val accountRepository: AccountRepository,
    private val livePosSalesDailyRepository: LivePosSalesDailyRepository,
    private val productRepository: ProductRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 1단 — 조건에 맞는 거래처 목록 조회 (외부 POS DB 미접촉).
     *
     * 지점/거래처명/유통형태/거래처유형 필터를 메인 DB Account 에만 적용해 거래처 목록을 반환한다.
     * POS 집계는 수행하지 않아 즉시 응답한다. 운영자는 이 목록에서 거래처를 선택해 [getList] 로
     * 2단 POS 조회를 실행한다. [MAX_ACCOUNT_COUNT] 초과 시 400 (조건 좁힘 안내).
     *
     * @throws AdminForbiddenException 권한 범위와 입력 costCenterCodes 의 교집합이 비어있을 때
     * @throws BusinessException costCenterCodes 미지정(400) 또는 거래처 수가 [MAX_ACCOUNT_COUNT] 초과(400)일 때
     */
    fun getAccounts(scope: DataScope, request: PosSalesAccountListRequest): PosSalesAccountListResponse {
        if (request.costCenterCodes.isEmpty()) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "cost_center_codes는 필수입니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        val effectiveCodes = applyScope(scope, request.costCenterCodes)
        val accounts = findAccounts(
            effectiveCodes,
            customerKeyword = request.customerKeyword,
            distributionChannels = request.distributionChannels,
            accountTypes = request.accountTypes,
        )
        validateAccountCount(accounts.size)
        val items = accounts
            .map { account ->
                PosSalesAccountItem(
                    accountId = account.id,
                    accountName = account.name,
                    sapAccountCode = account.externalKey,
                    distributionChannel = account.distributionChannelLabel(),
                    accountType = account.abcTypeLabel(),
                    branchCode = account.branchCode,
                    branchName = account.branchName,
                )
            }
            .sortedWith(compareBy(nullsLast()) { it.accountName })
        return PosSalesAccountListResponse(totalElements = items.size, items = items)
    }

    /**
     * 2단 — 선택 거래처별 POS매출 명세 조회 (페이징 + 정렬 + 제품 필터).
     *
     * 1단에서 운영자가 선택한 거래처(accountIds, 최대 [MAX_SELECTABLE_ACCOUNTS])의 기간 POS매출
     * 합계를 거래처 1행으로 반환. 제품/분류 필터가 지정되면 해당 제품군만 합산. 응답에 조회 결과
     * 전체(페이징 무관) 금액/수량 합계 포함 — 선택 거래처 기준. 페이징은 메모리에서 sort + slice.
     *
     * @throws AdminForbiddenException 선택 거래처 중 권한 범위 밖 거래처가 포함되어 있을 때
     */
    fun getList(scope: DataScope, request: PosSalesDashboardListRequest): PosSalesDashboardListResponse {
        validateDateRange(request.startDate, request.endDate)
        val accounts = resolveSelectedAccounts(scope, request.accountIds)

        val items = buildListItems(accounts, request)
        val sorted = sortItems(items, request.sort)
        val totalElements = sorted.size.toLong()
        val pageSize = request.size.coerceIn(1, 100)
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + pageSize - 1) / pageSize).toInt()
        val fromIndex = (request.page * pageSize).coerceAtLeast(0)
        val toIndex = (fromIndex + pageSize).coerceAtMost(sorted.size)
        val pageItems = if (fromIndex >= sorted.size) emptyList() else sorted.subList(fromIndex, toIndex)

        return PosSalesDashboardListResponse(
            startDate = request.startDate,
            endDate = request.endDate,
            totalSalesAmount = sorted.sumOf { it.salesAmount },
            totalSalesQuantity = sorted.sumOf { it.salesQuantity },
            items = pageItems,
            pageInfo = PosSalesDashboardListResponse.PageInfo(
                page = request.page,
                size = pageSize,
                totalElements = totalElements,
                totalPages = totalPages,
            ),
        )
    }

    /**
     * 엑셀 export 용 선택 거래처 명세 산출 (페이징 미적용).
     */
    fun getListForExport(scope: DataScope, request: PosSalesDashboardListRequest): List<PosSalesDashboardListItem> {
        validateDateRange(request.startDate, request.endDate)
        val accounts = resolveSelectedAccounts(scope, request.accountIds)
        return sortItems(buildListItems(accounts, request), request.sort).take(EXPORT_MAX_ROWS)
    }

    /**
     * 단건 거래처 상세 — 제품별 POS매출 명세 (레거시 `selectPosData` 동등).
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
    ): PosSalesRangeResponse {
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
                livePosSalesDailyRepository.aggregateByProduct(custCd, from, to).map { row ->
                    PosSalesResponse.ProductSales(
                        productCode = row.getItemCd(),
                        productName = row.getItemNm() ?: "",
                        barcode = row.getBarcode(),
                        amount = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong(),
                        quantity = (row.getSalesQty() ?: BigDecimal.ZERO).toLong(),
                    )
                }
            }.getOrElse { e ->
                log.warn("POS매출 상세 조회 실패 (custCd={}, {}~{}): {}", custCd, from, to, e.message)
                emptyList()
            }
            barcodes.isEmpty() -> emptyList() // 필터 지정했으나 매칭 바코드 없음 → 매출 없음
            else -> aggregateDetailByBarcodes(custCd, from, to, barcodes)
        }

        return PosSalesRangeResponse(
            customerId = account.id,
            customerName = account.name ?: "",
            sapAccountCode = account.externalKey ?: "",
            startDate = from,
            endDate = to,
            totalAmount = products.sumOf { it.amount },
            totalQuantity = products.sumOf { it.quantity },
            items = products,
        )
    }

    // ------------------- helpers -------------------

    private fun buildListItems(
        accounts: List<Account>,
        request: PosSalesDashboardListRequest,
    ): List<PosSalesDashboardListItem> {
        if (accounts.isEmpty()) return emptyList()

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
                log.warn("POS매출 명세 조회 실패 ({}~{}): {}", from, to, e.message)
                emptyMap()
            }
        }

        return accounts.map { account ->
            val custCd = toCustCd(account.externalKey)
            val (amount, quantity) = custCd?.let { salesByCustCd[it] } ?: (0L to 0L)
            PosSalesDashboardListItem(
                accountId = account.id,
                accountName = account.name,
                sapAccountCode = account.externalKey,
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
        fun accumulate(rows: List<PosCustomerSalesRow>) = rows.forEach { row ->
            val amt = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong()
            val qty = (row.getSalesQty() ?: BigDecimal.ZERO).toLong()
            merged.merge(row.getCustCd(), amt to qty) { prev, next ->
                (prev.first + next.first) to (prev.second + next.second)
            }
        }
        custCds.chunked(CUST_CD_CHUNK_SIZE).forEach { custChunk ->
            if (barcodes.isEmpty()) {
                accumulate(livePosSalesDailyRepository.aggregateByCustomer(custChunk, from, to))
            } else {
                barcodes.chunked(BARCODE_CHUNK_SIZE).forEach { chunk ->
                    accumulate(
                        livePosSalesDailyRepository.aggregateByCustomerAndBarcodes(custChunk, from, to, chunk),
                    )
                }
            }
        }
        return merged
    }

    /**
     * 상세(제품별) POS 집계 — 바코드 청크 분할 실행 후 제품코드 단위 병합.
     * (`aggregateByProductAndBarcodes` 는 청크 간 동일 제품이 중복될 수 있어 제품코드로 재합산.)
     */
    private fun aggregateDetailByBarcodes(
        custCd: String,
        from: String,
        to: String,
        barcodes: List<String>,
    ): List<PosSalesResponse.ProductSales> = runCatching {
        data class Acc(var name: String, var barcode: String?, var amount: Long, var quantity: Long)

        val merged = linkedMapOf<String, Acc>()
        barcodes.chunked(BARCODE_CHUNK_SIZE).forEach { chunk ->
            livePosSalesDailyRepository.aggregateByProductAndBarcodes(custCd, from, to, chunk)
                .forEach { row ->
                    val acc = merged.getOrPut(row.getItemCd()) {
                        Acc(row.getItemNm() ?: "", row.getBarcode(), 0L, 0L)
                    }
                    if (acc.barcode == null) acc.barcode = row.getBarcode()
                    acc.amount += (row.getSalesAmt() ?: BigDecimal.ZERO).toLong()
                    acc.quantity += (row.getSalesQty() ?: BigDecimal.ZERO).toLong()
                }
        }
        merged.map { (code, acc) ->
            PosSalesResponse.ProductSales(
                productCode = code,
                productName = acc.name,
                barcode = acc.barcode,
                amount = acc.amount,
                quantity = acc.quantity,
            )
        }.sortedByDescending { it.amount }
    }.getOrElse { e ->
        log.warn("POS매출 상세(제품 필터) 조회 실패 (custCd={}, {}~{}): {}", custCd, from, to, e.message)
        emptyList()
    }

    /**
     * 제품/분류 필터 → 소비자 바코드 목록 해소 (메인 DB Product/ProductBarcode).
     * POS `BARCODE` ↔ `ProductBarcode.barcode` 는 기존 POS매출 제품 검색과 동일 출처라
     * 전산실적용 해소 쿼리를 그대로 재사용한다.
     */
    private fun resolveBarcodes(productIds: List<Long>, category2: String?, category3: String?): List<String> =
        productRepository.findBarcodesForElectronicSales(productIds, category2, category3)

    private fun sortItems(items: List<PosSalesDashboardListItem>, sort: String?): List<PosSalesDashboardListItem> {
        if (sort.isNullOrBlank()) return items.sortedByDescending { it.salesAmount }
        val parts = sort.split(",")
        val field = parts[0].trim()
        val desc = parts.getOrNull(1)?.trim()?.equals("desc", ignoreCase = true) == true
        val comparator: Comparator<PosSalesDashboardListItem> = when (field) {
            "accountName" -> compareBy(nullsLast()) { it.accountName }
            "salesAmount" -> compareBy { it.salesAmount }
            "salesQuantity" -> compareBy { it.salesQuantity }
            else -> compareBy(nullsLast()) { it.accountName }
        }
        return if (desc) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
    }

    private fun findAccounts(
        effectiveCodes: List<String>,
        customerKeyword: String?,
        distributionChannels: List<String>,
        accountTypes: List<String>,
    ): List<Account> {
        return accountRepository.findByBranchCodeIn(effectiveCodes)
            .filter { acc ->
                customerKeyword.isNullOrBlank() ||
                    acc.name?.contains(customerKeyword, ignoreCase = true) == true
            }
            // 유통형태(거래처상태코드+거래처타입) / 거래처유형(ABC유형) 라벨 필터 — 메인 DB 해소분
            .filter { acc ->
                distributionChannels.isEmpty() ||
                    acc.distributionChannelLabel() in distributionChannels
            }
            .filter { acc ->
                accountTypes.isEmpty() || acc.abcTypeLabel() in accountTypes
            }
    }

    /**
     * 2단 선택 거래처 id 해소 — 상한 검증 + 존재 확인 + 권한 범위 확인.
     *
     * accountIds 의 개수가 [MAX_SELECTABLE_ACCOUNTS] 초과면 400. 선택 거래처 중 하나라도 권한
     * 범위(지점) 밖이면 403 (silent 제외로 합계가 틀어지는 것 방지).
     */
    private fun resolveSelectedAccounts(scope: DataScope, accountIds: List<Long>): List<Account> {
        if (accountIds.isEmpty()) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "조회할 거래처를 선택해주세요",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        if (accountIds.size > MAX_SELECTABLE_ACCOUNTS) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "거래처는 최대 ${MAX_SELECTABLE_ACCOUNTS}개까지 선택할 수 있습니다.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        val accounts = accountRepository.findByIdInAndIsDeletedNot(accountIds.distinct(), true)
        if (accounts.any { !scope.validateAccess(it.branchCode) }) throw AdminForbiddenException()
        return accounts
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
     * 1단 거래처 목록 크기 가드 — 메인 DB 에서 확보한 거래처 목록의 개수(추가 쿼리 없음)로 화면에
     * 내려줄 목록 규모를 제한한다. 상한 초과 시 조건 좁힘을 안내한다 (silent truncation 방지).
     */
    private fun validateAccountCount(count: Int) {
        if (count > MAX_ACCOUNT_COUNT) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "조회 대상 거래처가 ${count}건입니다. 최대 ${MAX_ACCOUNT_COUNT}건까지 조회할 수 있습니다. " +
                    "지점/유통형태/거래처유형/거래처명 조건을 좁혀주세요.",
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
        // 외부(POS) DB 일별 테이블 스캔 보호 — 레거시 maxSpan 정합, 두 끝점 일수 차이 최대 31.
        if (endDate.isAfter(startDate.plusDays(MAX_RANGE_DAYS))) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "조회 기간은 최대 ${MAX_RANGE_DAYS}일까지 가능합니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /** 레거시 `posmain.jsp` 의 `CUST_CD = "000" + accountCode` 패딩 정합. */
        private const val CUST_CD_PREFIX = "000"

        /** 엑셀 export 최대 행 수 — 초과분은 절단 (web 단 totalCount 경고와 정합). */
        private const val EXPORT_MAX_ROWS = 50_000

        /** 조회 기간 상한 (두 끝점 일수 차이). 레거시 daterangepicker `maxSpan: { days: 31 }` 정합. */
        private const val MAX_RANGE_DAYS = 31L

        /** POS `BARCODE IN` 청크 크기 — 외부 DB 에 전달하는 IN 목록 비대화 방지. */
        private const val BARCODE_CHUNK_SIZE = 1_000

        /**
         * POS `CUST_CD IN` 청크 크기 — 쿼리 1회의 집계 부하를 레거시 단일 거래처 조회의 50배
         * 이내로 상한 (실측 기준 1거래처×1달 ≈ 500행 → 쿼리 1회 ≈ 2.5만 행 상당).
         */
        private const val CUST_CD_CHUNK_SIZE = 50

        /**
         * 1단 거래처 목록 크기 상한 — 메인 DB 에서 확보한 거래처 목록 count 로 사전 확인 (추가
         * 쿼리 없음). 초과 시 400 조건 좁힘 안내. 외부 POS DB 는 미접촉이라 메인 DB 목록 규모만 제한.
         */
        private const val MAX_ACCOUNT_COUNT = 2_500

        /**
         * 2단 선택 거래처 수 상한 — 외부 POS DB `CUST_CD IN` 왕복을 1회로 유지하기 위해
         * [CUST_CD_CHUNK_SIZE] 이내로 제한. 초과 시 400.
         */
        const val MAX_SELECTABLE_ACCOUNTS = 20
    }
}
