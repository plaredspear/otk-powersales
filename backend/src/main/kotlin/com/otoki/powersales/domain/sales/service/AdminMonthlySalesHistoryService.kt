package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesHistoryListResponse
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.domain.sales.materialize.OroraAccountRange
import com.otoki.powersales.domain.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * web admin 「기준정보 > ORORA 월매출」 조회 service.
 *
 * ## 데이터 source
 * 메인 RDS `monthly_sales_history` (= ORORA 월별 마감 view 를 매월 적재한 결과,
 * `OroraMonthlySalesChunkProcessor`). ORORA view 를 화면에서 직접 읽지 않는다 —
 * ORORA 도달은 VPC peering 한정(dev/prod) 이고, 화면/집계는 항상 RDS 적재본을 본다는
 * 기존 정책([MonthlySalesAdminQueryService] 과 동일) 을 따른다.
 * 일매출 화면([AdminDailySalesHistoryService]) 과 동일한 구조의 조회 전용 화면이다.
 *
 * ## 동작 요약
 * 거래처 1곳(거래처코드) + 매출년월(`yyyyMM`) 의 적재 행 + 금액 합계 2종 + 마지막 적재 시각
 * (조회 결과 행의 `max(updated_at)`) 을 반환한다.
 * 거래처는 필수 조건 — 월 단위 전 거래처 스캔을 원천 차단한다. 부수 효과: 없음 (조회 전용).
 *
 * SF `IsDeleted` soft-delete row 는 목록에 노출하되 합계에서는 제외한다 — 일매출 entity 에는 없는
 * 축이라 일매출 패턴을 그대로 따르면 안 된다. 상세 근거는 [MonthlySalesHistoryListResponse] KDoc 참조.
 *
 * 적재 배치 실행 이력(`scheduled_job_run`) 은 적재 시각 출처로 쓰지 않는다 — 대상 월이 metadata
 * JSON 에만 남아 조회 월과 대조할 수 없고(과거 월을 봐도 당월 배치 시각이 찍힘), 이력 보존이
 * 90일이며, SF 이관 데이터에는 실행 이력 자체가 없어 신뢰 가능한 값이 나오지 않는다.
 * 데이터 자체의 `updated_at` 만이 조회 월 기준으로 항상 정확하다.
 *
 * ## 가시성
 * 거래처 지점(`branch_code`) 이 호출자 [DataScope] 범위 밖이면 [AdminForbiddenException]
 * (일매출 / POS / 전산매출 admin 조회와 동일 정책).
 */
@Service
@Transactional(readOnly = true)
class AdminMonthlySalesHistoryService(
    private val accountRepository: AccountRepository,
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
) {

    /**
     * 거래처 + 매출년월의 월매출 적재 행 조회.
     *
     * @param accountCode 거래처코드 (`account.external_key`). ORORA 원본 형식(선행 `000` 포함) 입력도
     *                    허용 — 적재본은 prefix 제거 형식이라 미매칭 시 prefix 를 떼고 1회 재조회한다.
     * @param salesMonth 매출발생년월. `yyyyMM` / `yyyy-MM` 모두 허용 (숫자만 추출해 정규화).
     * @throws BusinessException `INVALID_PARAMETER` (매출년월 형식 오류 / 적재 지원 연도 밖) /
     *                           `ACCOUNT_NOT_FOUND` (거래처 부재)
     * @throws AdminForbiddenException 거래처가 권한 범위 밖일 때
     */
    fun getMonthlySalesHistories(
        scope: DataScope,
        accountCode: String,
        salesMonth: String,
    ): MonthlySalesHistoryListResponse {
        val period = normalizeSalesMonth(salesMonth)
        val resolved = resolveAccount(accountCode)
        if (!scope.validateAccess(resolved.account.branchCode)) throw AdminForbiddenException()

        val rows = monthlySalesHistoryRepository
            .findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(
                resolved.sapAccountCode,
                period.year,
                period.month,
            )

        return MonthlySalesHistoryListResponse.of(
            salesMonth = period.value,
            sapAccountCode = resolved.sapAccountCode,
            accountName = resolved.account.name,
            branchName = resolved.account.branchName,
            entities = rows,
        )
    }

    /**
     * 매출년월 입력을 `yyyyMM` + 조회용 enum 쌍으로 정규화. 숫자 외 문자(하이픈 등) 는 제거한다.
     *
     * `sales_year` / `sales_month` 는 SF picklist enum 컬럼이라 [SalesYear] 범위(2019~2030) 밖 연도는
     * 애초에 적재될 수 없다 — 매칭 실패로 빈 목록을 주는 대신 입력 오류로 명시한다.
     */
    private fun normalizeSalesMonth(salesMonth: String): SalesPeriod {
        val digits = salesMonth.filter { it.isDigit() }
        val year = SalesYear.fromValueOrNull(digits.take(4))
        val month = SalesMonth.fromValueOrNull(digits.drop(4))
        if (digits.length != 6 || year == null || month == null) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "매출발생년월 형식이 올바르지 않거나 조회 가능 범위를 벗어났습니다 (yyyyMM): $salesMonth",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        return SalesPeriod(digits, year, month)
    }

    /**
     * 거래처코드로 Account 조회 — ORORA 원본 형식(선행 `000`) 입력도 흡수한다.
     */
    private fun resolveAccount(accountCode: String): ResolvedAccount {
        val trimmed = accountCode.trim()
        if (trimmed.isBlank()) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "거래처코드는 필수입니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        val candidates = buildList {
            add(trimmed)
            if (trimmed.startsWith(OroraAccountRange.ACCOUNT_CODE_PREFIX)) {
                add(trimmed.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX))
            }
        }
        val matched = candidates.firstNotNullOfOrNull { candidate ->
            accountRepository.findByExternalKey(candidate)?.let { ResolvedAccount(it, candidate) }
        }
        return matched ?: throw BusinessException(
            errorCode = "ACCOUNT_NOT_FOUND",
            message = "거래처를 찾을 수 없습니다: $trimmed",
            httpStatus = HttpStatus.NOT_FOUND,
        )
    }

    /** 정규화된 매출년월 — 표시용 `yyyyMM` 문자열 + 조회용 enum 쌍. */
    private data class SalesPeriod(val value: String, val year: SalesYear, val month: SalesMonth)

    /** 조회된 거래처 + 실제 매칭에 쓰인 거래처코드 (적재본 `sap_account_code` 형식). */
    private data class ResolvedAccount(val account: Account, val sapAccountCode: String)
}
