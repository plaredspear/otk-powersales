package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.order.sap.client.InventoryInfo
import com.otoki.powersales.domain.activity.order.sap.client.SapInventorySearchClient
import com.otoki.powersales.domain.foundation.product.dto.request.InventorySearchRequest
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.domain.foundation.product.exception.InvalidSearchParameterException
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Optional

@DisplayName("AdminProductInventoryService 테스트")
class AdminProductInventoryServiceTest {

    private val productRepository: ProductRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val sapInventorySearchClient: SapInventorySearchClient = mockk()

    private val service = AdminProductInventoryService(
        productRepository,
        accountRepository,
        sapInventorySearchClient,
    )

    @Test
    @DisplayName("정상 케이스 — 1~50건 + 거래처 존재 + 납기일 내일 -> SAP 응답 매핑")
    fun searchInventory_success() {
        val tomorrow = LocalDate.now().plus(1, ChronoUnit.DAYS)
        val request = InventorySearchRequest(
            accountId = 1,
            productCodes = listOf("P001", "P002"),
            deliveryRequestDate = tomorrow
        )
        every { accountRepository.findById(1) } returns Optional.of(createAccount(1))
        every { productRepository.findByProductCodeIn(listOf("P001", "P002")) } returns listOf(
            createProduct(productCode = "P001", name = "꿀배청 680G", unit = "EA"),
            createProduct(productCode = "P002", name = "카레 100G", unit = "EA")
        )
        every { sapInventorySearchClient.search(1L, any(), any()) } returns mapOf(
            "P001" to InventoryInfo("P001", "SAP_P001", 1, 100, BigDecimal("1000")),
            "P002" to InventoryInfo("P002", "SAP_P002", 1, 200, BigDecimal("2000")),
        )

        val response = service.searchInventory(request)

        assertThat(response.results).hasSize(2)
        assertThat(response.results[0].productCode).isEqualTo("P001")
        assertThat(response.results[0].productName).isEqualTo("꿀배청 680G")
        assertThat(response.results[0].unit).isEqualTo("EA")
        assertThat(response.results[0].unitPrice).isEqualByComparingTo("1000")
        assertThat(response.results[0].message).isNull()
    }

    @Test
    @DisplayName("SAP 응답 누락 케이스 — products 는 있으나 SAP 응답에 없는 경우 message 노출")
    fun searchInventory_sapMissing() {
        val tomorrow = LocalDate.now().plus(1, ChronoUnit.DAYS)
        val request = InventorySearchRequest(
            accountId = 1,
            productCodes = listOf("P001"),
            deliveryRequestDate = tomorrow
        )
        every { accountRepository.findById(1) } returns Optional.of(createAccount(1))
        every { productRepository.findByProductCodeIn(listOf("P001")) } returns listOf(
            createProduct(productCode = "P001", name = "꿀배청")
        )
        every { sapInventorySearchClient.search(1L, any(), any()) } returns emptyMap()

        val response = service.searchInventory(request)

        assertThat(response.results).hasSize(1)
        assertThat(response.results[0].message).isEqualTo("SAP 응답에 누락된 제품입니다")
        assertThat(response.results[0].unitPrice).isEqualByComparingTo("0")
    }

    @Test
    @DisplayName("납기일이 오늘 이전 -> InvalidSearchParameterException")
    fun searchInventory_dateTooEarly() {
        val request = InventorySearchRequest(
            accountId = 1,
            productCodes = listOf("P001"),
            deliveryRequestDate = LocalDate.now()
        )

        assertThatThrownBy { service.searchInventory(request) }
            .isInstanceOf(InvalidSearchParameterException::class.java)
            .hasMessageContaining("내일")
    }

    @Test
    @DisplayName("거래처 미존재 -> InvalidSearchParameterException")
    fun searchInventory_accountNotFound() {
        val request = InventorySearchRequest(
            accountId = 999,
            productCodes = listOf("P001"),
            deliveryRequestDate = LocalDate.now().plus(1, ChronoUnit.DAYS)
        )
        every { accountRepository.findById(999) } returns Optional.empty()

        assertThatThrownBy { service.searchInventory(request) }
            .isInstanceOf(InvalidSearchParameterException::class.java)
            .hasMessageContaining("거래처")
    }

    private fun createAccount(id: Long): Account = Account(id = id, name = "테스트 거래처")

    private fun createProduct(
        productCode: String,
        name: String? = null,
        unit: String? = null
    ): Product = Product(
        productCode = productCode,
        name = name,
        unit = unit,
        storageCondition = StorageCondition.fromDisplayNameOrNull("실온"),
        productStatus = ProductStatus.fromDisplayNameOrNull("-")
    )
}
