package com.otoki.powersales.product.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.order.sap.client.InventoryInfo
import com.otoki.powersales.order.sap.client.SapInventorySearchClient
import com.otoki.powersales.product.dto.request.InventorySearchRequest
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.enums.ProductStatus
import com.otoki.powersales.product.enums.StorageCondition
import com.otoki.powersales.product.exception.InvalidSearchParameterException
import com.otoki.powersales.product.repository.ProductRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminProductInventoryService 테스트")
class AdminProductInventoryServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var sapInventorySearchClient: SapInventorySearchClient

    @InjectMocks
    private lateinit var service: AdminProductInventoryService

    @Test
    @DisplayName("정상 케이스 — 1~50건 + 거래처 존재 + 납기일 내일 -> SAP 응답 매핑")
    fun searchInventory_success() {
        // Given
        val tomorrow = LocalDate.now().plusDays(1)
        val request = InventorySearchRequest(
            accountId = 1,
            productCodes = listOf("P001", "P002"),
            deliveryRequestDate = tomorrow
        )
        whenever(accountRepository.findById(eq(1))).thenReturn(Optional.of(createAccount(1)))
        whenever(productRepository.findByProductCodeIn(eq(listOf("P001", "P002"))))
            .thenReturn(
                listOf(
                    createProduct(productCode = "P001", name = "꿀배청 680G", unit = "EA"),
                    createProduct(productCode = "P002", name = "카레 100G", unit = "EA")
                )
            )
        whenever(sapInventorySearchClient.search(eq(1L), any())).thenReturn(
            mapOf(
                "P001" to InventoryInfo("P001", "SAP_P001", 1, 100, BigDecimal("1000")),
                "P002" to InventoryInfo("P002", "SAP_P002", 1, 200, BigDecimal("2000")),
            )
        )

        // When
        val response = service.searchInventory(request)

        // Then
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
        // Given
        val tomorrow = LocalDate.now().plusDays(1)
        val request = InventorySearchRequest(
            accountId = 1,
            productCodes = listOf("P001"),
            deliveryRequestDate = tomorrow
        )
        whenever(accountRepository.findById(eq(1))).thenReturn(Optional.of(createAccount(1)))
        whenever(productRepository.findByProductCodeIn(eq(listOf("P001"))))
            .thenReturn(listOf(createProduct(productCode = "P001", name = "꿀배청")))
        whenever(sapInventorySearchClient.search(eq(1L), any())).thenReturn(emptyMap())

        // When
        val response = service.searchInventory(request)

        // Then
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
            deliveryRequestDate = LocalDate.now().plusDays(1)
        )
        whenever(accountRepository.findById(eq(999))).thenReturn(Optional.empty())

        assertThatThrownBy { service.searchInventory(request) }
            .isInstanceOf(InvalidSearchParameterException::class.java)
            .hasMessageContaining("거래처")
    }

    private fun createAccount(id: Int): Account = Account(id = id, name = "테스트 거래처")

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
