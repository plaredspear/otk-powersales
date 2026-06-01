package com.otoki.powersales.sales.service

import com.otoki.pos.repository.LiveTotSalesDailyRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.sales.dto.request.ElectronicSalesDashboardListRequest
import com.otoki.powersales.sales.dto.response.ElectronicSalesDashboardDetailResponse
import com.otoki.powersales.sales.dto.response.ElectronicSalesDashboardListItem
import com.otoki.powersales.sales.dto.response.ElectronicSalesDashboardListResponse
import org.slf4j.LoggerFactory
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
 * 본 service 는 그 화면을 web admin 으로 확장 — 권한 범위 거래처 N건의 월간 전산매출 합계 명세 +
 * 거래처별 제품 상세. [MonthlySalesAdminQueryService] (물류배부) 와 동일한 권한/필터 패턴.
 *
 * ## 기간 산출
 * 레거시 daterangepicker (일 단위) 대신 연월(`year`/`month`) 단위 — **해당 월 1일~말일** 누적.
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
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 거래처별 전산매출 명세 조회 — 페이징 + 정렬 + 필터.
     *
     * 권한 범위 거래처 N건의 해당 월 전산매출(POS) 합계를 거래처 1행으로 반환.
     * 페이징은 조회된 거래처 list 를 메모리에서 sort + slice.
     *
     * @throws AdminForbiddenException 권한 범위와 입력 costCenterCodes 의 교집합이 비어있을 때
     */
    fun getList(scope: DataScope, request: ElectronicSalesDashboardListRequest): ElectronicSalesDashboardListResponse {
        validateParams(request.year, request.month, request.costCenterCodes)
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
        validateParams(request.year, request.month, request.costCenterCodes)
        val effectiveCodes = applyScope(scope, request.costCenterCodes)
        return sortItems(buildListItems(effectiveCodes, request), request.sort)
    }

    /**
     * 단건 거래처 상세 — 제품별 전산매출 명세 (레거시 `SelectAbcData` 동등).
     *
     * @throws AdminForbiddenException 거래처가 권한 범위 밖일 때
     */
    fun getDetail(scope: DataScope, customerId: Int, year: Int, month: Int): ElectronicSalesDashboardDetailResponse {
        validateYearMonth(year, month)
        val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
            ?: throw BusinessException(
                errorCode = "ACCOUNT_NOT_FOUND",
                message = "거래처를 찾을 수 없습니다: $customerId",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        if (!scope.validateAccess(account.branchCode)) throw AdminForbiddenException()

        val (startDate, endDate) = monthRange(year, month)
        val custCd = toCustCd(account.externalKey)

        val products = if (custCd == null) emptyList() else runCatching {
            liveTotSalesDailyRepository.aggregateByProduct(custCd, startDate, endDate).map { row ->
                ElectronicSalesDashboardDetailResponse.ProductSales(
                    productCode = row.getItemCd(),
                    productName = row.getItemNm() ?: "",
                    amount = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong(),
                    quantity = (row.getSalesQty() ?: BigDecimal.ZERO).toLong(),
                )
            }
        }.getOrElse { e ->
            log.warn("POS 전산매출 상세 조회 실패 (custCd={}, {}~{}): {}", custCd, startDate, endDate, e.message)
            emptyList()
        }

        return ElectronicSalesDashboardDetailResponse(
            customerId = account.id,
            customerName = account.name,
            sapAccountCode = account.externalKey,
            salesYear = year,
            salesMonth = month,
            totalAmount = products.sumOf { it.amount },
            totalQuantity = products.sumOf { it.quantity },
            items = products,
        )
    }

    // ------------------- helpers -------------------

    private fun buildListItems(
        effectiveCodes: List<String>,
        request: ElectronicSalesDashboardListRequest,
    ): List<ElectronicSalesDashboardListItem> {
        val accounts = findAccounts(effectiveCodes, request.accountGroup, request.customerKeyword)
            .let { all ->
                if (request.accountIds.isEmpty()) all else all.filter { it.id in request.accountIds }
            }
        if (accounts.isEmpty()) return emptyList()

        val (startDate, endDate) = monthRange(request.year, request.month)

        // custCd(legacy 패딩) → account 역매핑으로 POS 합계를 거래처에 결합
        val custCdToAccount = accounts.mapNotNull { acc ->
            toCustCd(acc.externalKey)?.let { it to acc }
        }.toMap()

        val salesByCustCd: Map<String, Pair<Long, Long>> = if (custCdToAccount.isEmpty()) {
            emptyMap()
        } else {
            runCatching {
                liveTotSalesDailyRepository
                    .aggregateByCustomer(custCdToAccount.keys.toList(), startDate, endDate)
                    .associate { row ->
                        row.getCustCd() to (
                            (row.getSalesAmt() ?: BigDecimal.ZERO).toLong() to
                                (row.getSalesQty() ?: BigDecimal.ZERO).toLong()
                            )
                    }
            }.getOrElse { e ->
                log.warn("POS 전산매출 명세 조회 실패 ({}~{}): {}", startDate, endDate, e.message)
                emptyMap()
            }
        }

        return accounts.map { account ->
            val custCd = toCustCd(account.externalKey)
            val (amount, quantity) = custCd?.let { salesByCustCd[it] } ?: (0L to 0L)
            ElectronicSalesDashboardListItem(
                accountId = account.id,
                accountName = account.name,
                sapAccountCode = account.externalKey,
                branchCode = account.branchCode,
                branchName = account.branchName,
                salesYear = request.year,
                salesMonth = request.month,
                salesAmount = amount,
                salesQuantity = quantity,
            )
        }
    }

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
        accountGroup: String?,
        customerKeyword: String?,
    ): List<Account> {
        val candidates = if (accountGroup != null) {
            effectiveCodes.flatMap { code ->
                accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
                    branchCode = code,
                    accountGroups = listOf(accountGroup),
                    isDeleted = true,
                )
            }
        } else {
            accountRepository.findByBranchCodeIn(effectiveCodes)
        }
        return if (customerKeyword.isNullOrBlank()) candidates
        else candidates.filter { it.name?.contains(customerKeyword, ignoreCase = true) == true }
    }

    /** [Account.externalKey] → POS `CUST_CD` (legacy 패딩 `"000" + accountCode`). blank → null. */
    private fun toCustCd(externalKey: String?): String? =
        if (externalKey.isNullOrBlank()) null else "$CUST_CD_PREFIX$externalKey"

    /** (year, month) → 해당 월 1일~말일 `YYYY-MM-DD` 범위. */
    private fun monthRange(year: Int, month: Int): Pair<String, String> {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
        return firstDay.format(DATE_FORMATTER) to lastDay.format(DATE_FORMATTER)
    }

    internal fun applyScope(scope: DataScope, costCenterCodes: List<String>): List<String> {
        if (scope.isAllBranches) return costCenterCodes
        val allowed = scope.branchCodes.toSet()
        val intersect = costCenterCodes.filter { it in allowed }
        if (intersect.isEmpty()) throw AdminForbiddenException()
        return intersect
    }

    private fun validateParams(year: Int, month: Int, costCenterCodes: List<String>) {
        validateYearMonth(year, month)
        if (costCenterCodes.isEmpty()) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "cost_center_codes는 필수입니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validateYearMonth(year: Int, month: Int) {
        if (year !in 2019..2099) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "year는 2019~2099 범위여야 합니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        if (month !in 1..12) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "month는 1~12 범위여야 합니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /** 레거시 `abcmain.jsp` 의 `CUST_CD = "000" + accountCode` 패딩 정합. */
        private const val CUST_CD_PREFIX = "000"
    }
}
