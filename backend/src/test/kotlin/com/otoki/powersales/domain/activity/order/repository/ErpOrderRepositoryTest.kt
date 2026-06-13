package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.common.config.QueryDslConfig
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * ErpOrderRepository 테스트 (#593).
 *
 * `findClientOrders` 의 `deliveryDate` nullable 파라미터 경로를 회귀 보호한다.
 * 레거시 버그: `(:deliveryDate IS NULL OR ...)` 패턴이 PostgreSQL 에서
 * `could not determine data type of parameter` 로 실패 → `CAST(:deliveryDate AS date)` 로 수정.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class ErpOrderRepositoryTest {

    @Autowired
    private lateinit var erpOrderRepository: ErpOrderRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var account: Account
    private val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "deliveryRequestDate", "sapOrderNumber"))

    @BeforeEach
    fun setUp() {
        account = testEntityManager.persistAndFlush(Account(name = "테스트거래처"))
        persistOrder("0300000001", LocalDate.of(2026, 6, 10))
        persistOrder("0300000002", LocalDate.of(2026, 6, 11))
        persistOrder("0300000003", null)
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findClientOrders - deliveryDate=null 이면 납기일 제한 없이 전체 조회한다")
    fun findClientOrders_nullDeliveryDate_returnsAll() {
        val result = erpOrderRepository.findClientOrders(account.id, null, pageable)

        assertThat(result.totalElements).isEqualTo(3)
        assertThat(result.content.map { it.sapOrderNumber })
            .containsExactlyInAnyOrder("0300000001", "0300000002", "0300000003")
    }

    @Test
    @DisplayName("findClientOrders - deliveryDate 지정 시 해당 납기일 주문만 조회한다")
    fun findClientOrders_withDeliveryDate_filters() {
        val result = erpOrderRepository.findClientOrders(account.id, LocalDate.of(2026, 6, 11), pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content.single().sapOrderNumber).isEqualTo("0300000002")
    }

    @Test
    @DisplayName("findClientOrders - is_deleted=true 주문은 제외한다")
    fun findClientOrders_excludesDeleted() {
        val deleted = ErpOrder(sapOrderNumber = "0300000099", deliveryRequestDate = LocalDate.of(2026, 6, 10)).apply {
            account = this@ErpOrderRepositoryTest.account
            isDeleted = true
        }
        testEntityManager.persistAndFlush(deleted)
        testEntityManager.clear()

        val result = erpOrderRepository.findClientOrders(account.id, null, pageable)

        assertThat(result.content.map { it.sapOrderNumber }).doesNotContain("0300000099")
    }

    private fun persistOrder(sapOrderNumber: String, deliveryRequestDate: LocalDate?) {
        val order = ErpOrder(sapOrderNumber = sapOrderNumber, deliveryRequestDate = deliveryRequestDate).apply {
            account = this@ErpOrderRepositoryTest.account
        }
        testEntityManager.persistAndFlush(order)
    }
}
