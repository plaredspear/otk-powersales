package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.platform.common.config.QueryDslConfig
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
 * `findClientOrders` 는 레거시 SF `ClientOrderSearch`(`DeliveryRequestDate__c =: 단일 날짜`) 와 동등하게
 * 납기일 단일 날짜 등호로 조회한다 — 납기일 일치 주문만 반환하고 납기일 null 주문은 제외한다.
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
    @DisplayName("findClientOrders - 지정 납기일 주문만 조회하고 납기일 null 주문은 제외한다")
    fun findClientOrders_singleDate_filters() {
        val result = erpOrderRepository.findClientOrders(account.id, LocalDate.of(2026, 6, 11), pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content.single().sapOrderNumber).isEqualTo("0300000002")
    }

    @Test
    @DisplayName("findClientOrders - 일치하는 납기일 주문이 없으면 빈 결과 (null 납기일 포함 미반환)")
    fun findClientOrders_noMatch_returnsEmpty() {
        val result = erpOrderRepository.findClientOrders(account.id, LocalDate.of(2026, 6, 12), pageable)

        assertThat(result.totalElements).isEqualTo(0)
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

        val result = erpOrderRepository.findClientOrders(account.id, LocalDate.of(2026, 6, 10), pageable)

        assertThat(result.content.map { it.sapOrderNumber })
            .containsExactly("0300000001")
    }

    @Test
    @DisplayName("findClientOrders - ref_sap_order_number 가 있는 후속 주문(취소/변경)은 제외한다")
    fun findClientOrders_excludesReferencingOrders() {
        // 참조 주문번호가 있는 후속 주문(취소건) — 원주문 상세의 "관련 주문"으로만 노출되고 목록엔 미표시.
        val cancel = ErpOrder(
            sapOrderNumber = "0604311314",
            refSapOrderNumber = "0300000001",
            deliveryRequestDate = LocalDate.of(2026, 6, 10),
        ).apply { account = this@ErpOrderRepositoryTest.account }
        testEntityManager.persistAndFlush(cancel)
        testEntityManager.clear()

        val result = erpOrderRepository.findClientOrders(account.id, LocalDate.of(2026, 6, 10), pageable)

        // 원주문(ref 없음)만 노출, 취소건(ref 있음)은 제외.
        assertThat(result.content.map { it.sapOrderNumber })
            .containsExactly("0300000001")
    }

    @Test
    @DisplayName("deleteByOrderDateBefore - cutoff 이전 order_date 헤더만 삭제하고 order_date null 은 보존한다")
    fun deleteByOrderDateBefore_deletesOldKeepsNull() {
        // 기본 픽스처(3건)는 order_date 미지정(null) — 삭제 대상 아님.
        persistOrderWithOrderDate("0300000010", LocalDate.of(2026, 1, 1)) // cutoff 이전 → 삭제
        persistOrderWithOrderDate("0300000011", LocalDate.of(2026, 3, 1)) // cutoff 당일 → 보존(< 비교)
        persistOrderWithOrderDate("0300000012", LocalDate.of(2026, 5, 1)) // cutoff 이후 → 보존
        testEntityManager.clear()

        val deleted = erpOrderRepository.deleteByOrderDateBefore(LocalDate.of(2026, 3, 1))
        testEntityManager.clear()

        assertThat(deleted).isEqualTo(1)
        val remaining = erpOrderRepository.findAll().map { it.sapOrderNumber }
        // null 3건 + 당일/이후 2건 = 5건 보존, cutoff 이전 1건만 삭제
        assertThat(remaining).containsExactlyInAnyOrder(
            "0300000001", "0300000002", "0300000003", "0300000011", "0300000012"
        )
    }

    @DisplayName("findAdminErpOrders - 관리자 웹 ERP주문 목록 조회 (기준정보 화면)")
    @org.junit.jupiter.api.Nested
    inner class FindAdminErpOrders {

        private val adminPageable = PageRequest.of(0, 20)

        @Test
        @DisplayName("필터 없으면 삭제 제외 전체를 id DESC 로 반환한다")
        fun noFilter_returnsAllExceptDeleted() {
            val result = erpOrderRepository.findAdminErpOrders(null, null, null, null, null, adminPageable)

            // setUp 3건(0300000001~3) 모두 반환. 최신(id DESC) 순.
            assertThat(result.totalElements).isEqualTo(3)
            assertThat(result.content.map { it.sapOrderNumber }).containsExactly(
                "0300000003", "0300000002", "0300000001",
            )
        }

        @Test
        @DisplayName("keyword 로 주문번호/거래처명/주문자명 부분 일치 조회한다")
        fun keyword_matchesMultipleFields() {
            persistNamedOrder("0300000050", sapAccountName = "홍길동마트", employeeName = "김영업")

            assertThat(
                erpOrderRepository.findAdminErpOrders("홍길동", null, null, null, null, adminPageable).totalElements,
            ).isEqualTo(1)
            assertThat(
                erpOrderRepository.findAdminErpOrders("김영업", null, null, null, null, adminPageable).totalElements,
            ).isEqualTo(1)
            assertThat(
                erpOrderRepository.findAdminErpOrders("0300000050", null, null, null, null, adminPageable).totalElements,
            ).isEqualTo(1)
        }

        @Test
        @DisplayName("납기일 기간(from~to inclusive) 으로 조회한다")
        fun deliveryDateRange() {
            // setUp: 0300000001=6/10, 0300000002=6/11, 0300000003=null
            val result = erpOrderRepository.findAdminErpOrders(
                null, LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 11), null, null, adminPageable,
            )

            assertThat(result.content.map { it.sapOrderNumber }).containsExactly("0300000002")
        }

        @Test
        @DisplayName("주문생성일 기간으로 조회한다")
        fun orderDateRange() {
            persistOrderWithOrderDate("0300000060", LocalDate.of(2026, 4, 15))

            val result = erpOrderRepository.findAdminErpOrders(
                null, null, null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), adminPageable,
            )

            assertThat(result.content.map { it.sapOrderNumber }).containsExactly("0300000060")
        }

        @Test
        @DisplayName("is_deleted=true 주문은 제외한다")
        fun excludesDeleted() {
            val deleted = ErpOrder(sapOrderNumber = "0300000099").apply {
                account = this@ErpOrderRepositoryTest.account
                isDeleted = true
            }
            testEntityManager.persistAndFlush(deleted)

            val numbers = erpOrderRepository
                .findAdminErpOrders(null, null, null, null, null, adminPageable)
                .content.map { it.sapOrderNumber }

            assertThat(numbers).doesNotContain("0300000099")
        }
    }

    private fun persistOrder(sapOrderNumber: String, deliveryRequestDate: LocalDate?) {
        val order = ErpOrder(sapOrderNumber = sapOrderNumber, deliveryRequestDate = deliveryRequestDate).apply {
            account = this@ErpOrderRepositoryTest.account
        }
        testEntityManager.persistAndFlush(order)
    }

    private fun persistOrderWithOrderDate(sapOrderNumber: String, orderDate: LocalDate) {
        val order = ErpOrder(sapOrderNumber = sapOrderNumber, orderDate = orderDate).apply {
            account = this@ErpOrderRepositoryTest.account
        }
        testEntityManager.persistAndFlush(order)
    }

    private fun persistNamedOrder(sapOrderNumber: String, sapAccountName: String, employeeName: String) {
        val order = ErpOrder(
            sapOrderNumber = sapOrderNumber,
            sapAccountName = sapAccountName,
            employeeName = employeeName,
        ).apply {
            account = this@ErpOrderRepositoryTest.account
        }
        testEntityManager.persistAndFlush(order)
    }
}
