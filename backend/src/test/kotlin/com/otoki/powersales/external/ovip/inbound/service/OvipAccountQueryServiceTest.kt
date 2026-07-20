package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.Industry
import com.otoki.powersales.domain.foundation.account.entity.Rating
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountSnapshotRow
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.inbound.config.OvipInboundProperties
import com.otoki.powersales.external.ovip.inbound.dto.AccountRow
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotSearchRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OvipAccountQueryService 테스트")
class OvipAccountQueryServiceTest {

    private val repository: AccountRepository = mockk()
    private val auditService: OvipInboundAuditService = mockk(relaxed = true)

    private fun service(
        defaultPageSize: Int = 100,
        maxPageSize: Int = 500,
    ): OvipAccountQueryService {
        val props = OvipInboundProperties(
            account = OvipInboundProperties.Pagination(
                defaultPageSize = defaultPageSize,
                maxPageSize = maxPageSize,
            )
        )
        return OvipAccountQueryService(repository, props, auditService)
    }

    private fun entity(id: Long): AccountSnapshotRow = snapshot(
        Account(id = id, externalKey = "EK-$id", name = "거래처-$id")
    )

    /** 관계 FK 는 쿼리에서 함께 조회되므로 테스트도 동일하게 snapshot 으로 감싼다. */
    private fun snapshot(
        account: Account,
        ownerUserId: Long? = null,
        createdById: Long? = null,
        lastModifiedById: Long? = null,
        parentId: Long? = null,
    ) = AccountSnapshotRow(account, ownerUserId, createdById, lastModifiedById, parentId)

    private fun req(cursor: Long? = null, size: Int? = null) =
        SnapshotSearchRequest(cursor = cursor, size = size)

    @Nested
    @DisplayName("size clamp")
    inner class SizeClamp {

        @Test
        @DisplayName("size 미지정 -> 기본값으로 (limit = default + 1) 조회")
        fun defaultSize() {
            val limitSlot = slot<Int>()
            every { repository.findSnapshotByKeyset(any(), capture(limitSlot)) } returns emptyList()

            service(defaultPageSize = 100).search(req(), "c", "127.0.0.1")

            assertThat(limitSlot.captured).isEqualTo(101)
        }

        @Test
        @DisplayName("size 가 max 초과 -> max 로 clamp (limit = max + 1)")
        fun clampToMax() {
            val limitSlot = slot<Int>()
            every { repository.findSnapshotByKeyset(any(), capture(limitSlot)) } returns emptyList()

            service(maxPageSize = 500).search(req(size = 5000), "c", "127.0.0.1")

            assertThat(limitSlot.captured).isEqualTo(501)
        }

        @Test
        @DisplayName("size 가 0 이하 -> 기본값 적용")
        fun nonPositiveSize() {
            val limitSlot = slot<Int>()
            every { repository.findSnapshotByKeyset(any(), capture(limitSlot)) } returns emptyList()

            service(defaultPageSize = 100).search(req(size = 0), "c", "127.0.0.1")

            assertThat(limitSlot.captured).isEqualTo(101)
        }
    }

    @Nested
    @DisplayName("keyset 페이지 판정")
    inner class Keyset {

        @Test
        @DisplayName("size+1 건 조회됨 -> hasNext=true, 마지막 1건 버림, nextCursor=마지막 노출 row id")
        fun hasNextTrue() {
            every { repository.findSnapshotByKeyset(null, 3) } returns
                listOf(entity(10), entity(11), entity(12))

            val res = service().search(req(size = 2), "c", "127.0.0.1")

            assertThat(res.hasNext).isTrue()
            assertThat(res.items).hasSize(2)
            assertThat(res.items.map { it.id }).containsExactly(10L, 11L)
            assertThat(res.nextCursor).isEqualTo(11L)
        }

        @Test
        @DisplayName("size 이하 조회됨 -> hasNext=false, nextCursor=null, 전건 노출")
        fun hasNextFalse() {
            every { repository.findSnapshotByKeyset(null, 3) } returns listOf(entity(10), entity(11))

            val res = service().search(req(size = 2), "c", "127.0.0.1")

            assertThat(res.hasNext).isFalse()
            assertThat(res.items).hasSize(2)
            assertThat(res.nextCursor).isNull()
        }

        @Test
        @DisplayName("빈 결과 -> hasNext=false, items 빈 목록, nextCursor=null")
        fun empty() {
            every { repository.findSnapshotByKeyset(999, 101) } returns emptyList()

            val res = service().search(req(cursor = 999), "c", "127.0.0.1")

            assertThat(res.items).isEmpty()
            assertThat(res.hasNext).isFalse()
            assertThat(res.nextCursor).isNull()
        }

        @Test
        @DisplayName("cursor 가 repository 로 그대로 전달됨")
        fun cursorPassthrough() {
            val cursorSlot = slot<Long>()
            every { repository.findSnapshotByKeyset(capture(cursorSlot), any()) } returns emptyList()

            service().search(req(cursor = 12345), "c", "127.0.0.1")

            assertThat(cursorSlot.captured).isEqualTo(12345L)
        }
    }

    @Nested
    @DisplayName("row 변환")
    inner class RowMapping {

        @Test
        @DisplayName("enum 필드는 enum name 이 아니라 DB 저장값(displayName) 으로 노출")
        fun enumUsesDisplayName() {
            val account = Account(
                id = 1,
                name = "거래처",
                industry = Industry.AGRICULTURE,
                rating = Rating.entries.first(),
            )
            every { repository.findSnapshotByKeyset(null, 101) } returns listOf(snapshot(account))

            val row = service().search(req(), "c", "127.0.0.1").items.single()

            assertThat(row.industry).isEqualTo(Industry.AGRICULTURE.displayName)
            assertThat(row.industry).isNotEqualTo(Industry.AGRICULTURE.name)
            assertThat(row.rating).isEqualTo(Rating.entries.first().displayName)
        }

        @Test
        @DisplayName("관계 미연결 시 FK id 는 null")
        fun nullRelations() {
            every { repository.findSnapshotByKeyset(null, 101) } returns listOf(entity(1))

            val row = service().search(req(), "c", "127.0.0.1").items.single()

            assertThat(row.ownerUserId).isNull()
            assertThat(row.createdById).isNull()
            assertThat(row.lastModifiedById).isNull()
            assertThat(row.parentId).isNull()
        }

        @Test
        @DisplayName("entity 필드가 row 로 그대로 전달됨")
        fun fieldsPassthrough() {
            val account = Account(
                id = 7,
                name = "오뚜기마트",
                externalKey = "EK-7",
                branchCode = "41010",
                accountType = "슈퍼",
            )

            val row = AccountRow.from(snapshot(account))

            assertThat(row.id).isEqualTo(7L)
            assertThat(row.name).isEqualTo("오뚜기마트")
            assertThat(row.externalKey).isEqualTo("EK-7")
            assertThat(row.branchCode).isEqualTo("41010")
            assertThat(row.accountType).isEqualTo("슈퍼")
        }
    }

    @Nested
    @DisplayName("audit")
    inner class Audit {

        @Test
        @DisplayName("정상 조회 시 REQUEST_ACCEPTED 적재 + received_count = 노출 건수")
        fun auditAccepted() {
            every { repository.findSnapshotByKeyset(null, 3) } returns
                listOf(entity(10), entity(11), entity(12))
            val auditSlot = slot<OvipInboundAudit>()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            service().search(req(size = 2), "cli-1", "10.0.0.1")

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(OvipInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditSlot.captured.clientId).isEqualTo("cli-1")
            assertThat(auditSlot.captured.receivedCount).isEqualTo(2)
            assertThat(auditSlot.captured.endpoint).isEqualTo(OvipAccountQueryService.ENDPOINT)
        }
    }
}
