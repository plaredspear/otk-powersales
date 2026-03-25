package com.otoki.internal.common.sap

import com.otoki.internal.sap.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SAP 어노테이션 테스트")
class SAPAnnotationTest {

    @Nested
    @DisplayName("@SAPSource 클래스 어노테이션")
    inner class SAPSourceTests {

        @Test
        @DisplayName("Account — UPSERT, /sap/ClientMasterReceive")
        fun account() {
            val annotation = Account::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/ClientMasterReceive")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.UPSERT)
        }

        @Test
        @DisplayName("Product — UPSERT, /sap/ProductMasterSend")
        fun product() {
            val annotation = Product::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/ProductMasterSend")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.UPSERT)
        }

        @Test
        @DisplayName("SystemCodeMaster — UPSERT, /sap/SystemCodeMaster")
        fun systemCodeMaster() {
            val annotation = SystemCodeMaster::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/SystemCodeMaster")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.UPSERT)
        }

        @Test
        @DisplayName("ErpOrder — UPSERT, /sap/ClientOrderSearch")
        fun erpOrder() {
            val annotation = ErpOrder::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/ClientOrderSearch")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.UPSERT)
        }

        @Test
        @DisplayName("DailySalesHistory — UPSERT, /sap/DailyErpSalesInfoReceive")
        fun dailySalesHistory() {
            val annotation = DailySalesHistory::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/DailyErpSalesInfoReceive")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.UPSERT)
        }

        @Test
        @DisplayName("MonthlySalesHistory — UPSERT, /sap/MonthlySalesHistory")
        fun monthlySalesHistory() {
            val annotation = MonthlySalesHistory::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/MonthlySalesHistory")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.UPSERT)
        }

        @Test
        @DisplayName("ProductBarcode — UPSERT, /sap/BarcodeMaster")
        fun productBarcode() {
            val annotation = ProductBarcode::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/BarcodeMaster")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.UPSERT)
        }

        @Test
        @DisplayName("Organization — DELETE_INSERT, /sap/OrganizeMasterReceive")
        fun organization() {
            val annotation = Organization::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/OrganizeMasterReceive")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.DELETE_INSERT)
        }

        @Test
        @DisplayName("Appointment — INSERT_ONLY, /sap/Appointment")
        fun appointment() {
            val annotation = Appointment::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/Appointment")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.INSERT_ONLY)
        }

        @Test
        @DisplayName("AttendInfo — INSERT_ONLY, /sap/AttendInfo")
        fun attendInfo() {
            val annotation = AttendInfo::class.java.getAnnotation(SAPSource::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.api).isEqualTo("/sap/AttendInfo")
            assertThat(annotation.syncMode).isEqualTo(SyncMode.INSERT_ONLY)
        }
    }

    @Nested
    @DisplayName("@SAPUpsertKey 필드 어노테이션")
    inner class SAPUpsertKeyTests {

        @Test
        @DisplayName("Account.externalKey — 단일키")
        fun accountExternalKey() {
            val field = Account::class.java.getDeclaredField("externalKey")
            val annotation = field.getAnnotation(SAPUpsertKey::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.composite).isFalse()
        }

        @Test
        @DisplayName("Product.productCode — 단일키")
        fun productProductCode() {
            val field = Product::class.java.getDeclaredField("productCode")
            val annotation = field.getAnnotation(SAPUpsertKey::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.composite).isFalse()
        }

        @Test
        @DisplayName("SystemCodeMaster.externalKey — 복합키")
        fun systemCodeMasterExternalKey() {
            val field = SystemCodeMaster::class.java.getDeclaredField("externalKey")
            val annotation = field.getAnnotation(SAPUpsertKey::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.composite).isTrue()
            assertThat(annotation.components).containsExactly("companyCode", "groupCode", "detailCode")
        }

        @Test
        @DisplayName("ErpOrder.sapOrderNumber — 단일키")
        fun erpOrderSapOrderNumber() {
            val field = ErpOrder::class.java.getDeclaredField("sapOrderNumber")
            val annotation = field.getAnnotation(SAPUpsertKey::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.composite).isFalse()
        }

        @Test
        @DisplayName("DailySalesHistory.externalKey — 복합키")
        fun dailySalesHistoryExternalKey() {
            val field = DailySalesHistory::class.java.getDeclaredField("externalKey")
            val annotation = field.getAnnotation(SAPUpsertKey::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.composite).isTrue()
            assertThat(annotation.components).containsExactly("sapAccountCode", "salesDate")
        }

        @Test
        @DisplayName("MonthlySalesHistory.externalkeyC — 복합키")
        fun monthlySalesHistoryExternalkeyC() {
            val field = MonthlySalesHistory::class.java.getDeclaredField("externalkeyC")
            val annotation = field.getAnnotation(SAPUpsertKey::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.composite).isTrue()
            assertThat(annotation.components).containsExactly("sapAccountCode", "salesYearMonth")
        }

        @Test
        @DisplayName("ProductBarcode.customKey — 복합키")
        fun productBarcodeCustomKey() {
            val field = ProductBarcode::class.java.getDeclaredField("customKey")
            val annotation = field.getAnnotation(SAPUpsertKey::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.composite).isTrue()
            assertThat(annotation.components).containsExactly("productCode", "unit", "sortOrder")
        }

        @Test
        @DisplayName("INSERT_ONLY 엔티티(Appointment)에는 @SAPUpsertKey 없음")
        fun appointmentNoUpsertKey() {
            val fields = Appointment::class.java.declaredFields
            val hasUpsertKey = fields.any { it.getAnnotation(SAPUpsertKey::class.java) != null }
            assertThat(hasUpsertKey).isFalse()
        }
    }
}
