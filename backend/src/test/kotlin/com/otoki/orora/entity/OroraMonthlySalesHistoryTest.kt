package com.otoki.orora.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * [OroraMonthlySalesHistory] derive property 검증.
 *
 * SF formula 정합:
 * - `ABCClosingSumAmount__c = ABC1 + ABC2 + ABC3 + ABC4`
 * - `ShipClosingSumAmount__c = Ship1 + Ship2 + Ship3 + Ship4`
 * - `ClosingAmountSum__c = ABCClosingSumAmount + ShipClosingSumAmount`
 * - 모든 SF formula 가 `formulaTreatBlanksAs=BlankAsZero` — null → ZERO 치환 검증
 */
@DisplayName("OroraMonthlySalesHistory derive property")
class OroraMonthlySalesHistoryTest {

	private fun row(
		abc1: BigDecimal? = null,
		abc2: BigDecimal? = null,
		abc3: BigDecimal? = null,
		abc4: BigDecimal? = null,
		ship1: BigDecimal? = null,
		ship2: BigDecimal? = null,
		ship3: BigDecimal? = null,
		ship4: BigDecimal? = null,
	) = OroraMonthlySalesHistory(
		sapAccountCode = "5500000001",
		salesDate = "202605",
		abcClosingAmount1 = abc1,
		abcClosingAmount2 = abc2,
		abcClosingAmount3 = abc3,
		abcClosingAmount4 = abc4,
		shipClosingAmount1 = ship1,
		shipClosingAmount2 = ship2,
		shipClosingAmount3 = ship3,
		shipClosingAmount4 = ship4,
	)

	@Nested
	@DisplayName("abcClosingSumAmount")
	inner class AbcClosingSumAmount {

		@Test
		@DisplayName("ABC1~4 합산 — SF formula `ABC1+ABC2+ABC3+ABC4` 동등")
		fun sumsAllFourAbcColumns() {
			val r = row(
				abc1 = BigDecimal("100"),
				abc2 = BigDecimal("200"),
				abc3 = BigDecimal("300"),
				abc4 = BigDecimal("400"),
			)
			assertThat(r.abcClosingSumAmount).isEqualByComparingTo(BigDecimal("1000"))
		}

		@Test
		@DisplayName("모든 컬럼 null — ZERO (formulaTreatBlanksAs=BlankAsZero 정합)")
		fun allNullReturnsZero() {
			assertThat(row().abcClosingSumAmount).isEqualByComparingTo(BigDecimal.ZERO)
		}

		@Test
		@DisplayName("일부 컬럼만 null — null 컬럼은 0 으로 치환 후 합산")
		fun nullColumnsTreatedAsZero() {
			val r = row(
				abc1 = BigDecimal("500"),
				abc2 = null,
				abc3 = BigDecimal("300"),
				abc4 = null,
			)
			assertThat(r.abcClosingSumAmount).isEqualByComparingTo(BigDecimal("800"))
		}

		@Test
		@DisplayName("Ship 컬럼은 abcSum 산출에 포함되지 않음")
		fun shipColumnsExcluded() {
			val r = row(
				abc1 = BigDecimal("100"),
				ship1 = BigDecimal("9999"),
				ship2 = BigDecimal("9999"),
			)
			assertThat(r.abcClosingSumAmount).isEqualByComparingTo(BigDecimal("100"))
		}
	}

	@Nested
	@DisplayName("shipClosingSumAmount")
	inner class ShipClosingSumAmount {

		@Test
		@DisplayName("Ship1~4 합산 — SF formula `Ship1+Ship2+Ship3+Ship4` 동등")
		fun sumsAllFourShipColumns() {
			val r = row(
				ship1 = BigDecimal("10"),
				ship2 = BigDecimal("20"),
				ship3 = BigDecimal("30"),
				ship4 = BigDecimal("40"),
			)
			assertThat(r.shipClosingSumAmount).isEqualByComparingTo(BigDecimal("100"))
		}

		@Test
		@DisplayName("모든 컬럼 null — ZERO")
		fun allNullReturnsZero() {
			assertThat(row().shipClosingSumAmount).isEqualByComparingTo(BigDecimal.ZERO)
		}

		@Test
		@DisplayName("ABC 컬럼은 shipSum 산출에 포함되지 않음")
		fun abcColumnsExcluded() {
			val r = row(
				abc1 = BigDecimal("9999"),
				abc2 = BigDecimal("9999"),
				ship1 = BigDecimal("50"),
			)
			assertThat(r.shipClosingSumAmount).isEqualByComparingTo(BigDecimal("50"))
		}
	}

	@Nested
	@DisplayName("closingAmountSum")
	inner class ClosingAmountSum {

		@Test
		@DisplayName("SF formula `ABCClosingSumAmount + ShipClosingSumAmount` 동등")
		fun sumsBothAbcAndShip() {
			val r = row(
				abc1 = BigDecimal("100"),
				abc2 = BigDecimal("200"),
				abc3 = BigDecimal("300"),
				abc4 = BigDecimal("400"),
				ship1 = BigDecimal("10"),
				ship2 = BigDecimal("20"),
				ship3 = BigDecimal("30"),
				ship4 = BigDecimal("40"),
			)
			assertThat(r.closingAmountSum).isEqualByComparingTo(BigDecimal("1100"))
		}

		@Test
		@DisplayName("모든 컬럼 null — ZERO (SF UpdateLastMonthRevenueBatch 의 row 부재 fallback 동등)")
		fun allNullReturnsZero() {
			assertThat(row().closingAmountSum).isEqualByComparingTo(BigDecimal.ZERO)
		}

		@Test
		@DisplayName("ABC 만 보유 — Ship 0 으로 치환 + ABC 합계 반환")
		fun onlyAbcPopulated() {
			val r = row(
				abc1 = BigDecimal("100"),
				abc3 = BigDecimal("300"),
			)
			assertThat(r.closingAmountSum).isEqualByComparingTo(BigDecimal("400"))
		}

		@Test
		@DisplayName("Ship 만 보유 — ABC 0 으로 치환 + Ship 합계 반환")
		fun onlyShipPopulated() {
			val r = row(
				ship2 = BigDecimal("200"),
				ship4 = BigDecimal("400"),
			)
			assertThat(r.closingAmountSum).isEqualByComparingTo(BigDecimal("600"))
		}
	}
}
