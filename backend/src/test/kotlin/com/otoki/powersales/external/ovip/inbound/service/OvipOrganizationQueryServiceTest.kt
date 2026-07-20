package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.org.organization.repository.OrganizationSnapshotRow
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OvipOrganizationQueryService 테스트")
class OvipOrganizationQueryServiceTest {

    private val repository: OrganizationRepository = mockk()
    private val auditService: OvipInboundAuditService = mockk(relaxed = true)

    private fun service() = OvipOrganizationQueryService(repository, auditService)

    /** 관계 FK 는 쿼리에서 함께 조회되므로 테스트도 동일하게 snapshot 으로 감싼다. */
    private fun snapshot(
        organization: Organization,
        ownerUserId: Long? = null,
        ownerGroupId: Long? = null,
        createdById: Long? = null,
        lastModifiedById: Long? = null,
    ) = OrganizationSnapshotRow(organization, ownerUserId, ownerGroupId, createdById, lastModifiedById)

    private fun entity(id: Long): OrganizationSnapshotRow = snapshot(
        Organization(id = id, name = "조직-$id", externalKey = "EK-$id")
    )

    @Nested
    @DisplayName("전량 조회")
    inner class FetchAll {

        @Test
        @DisplayName("repository 가 돌려준 전건을 그대로 노출 — 페이지네이션으로 자르지 않는다")
        fun returnsEveryRow() {
            every { repository.findAllSnapshot() } returns (1L..250L).map { entity(it) }

            val result = service().searchAll("cli-1", "10.0.0.1")

            assertThat(result.items).hasSize(250)
            assertThat(result.totalCount).isEqualTo(250)
            assertThat(result.items.map { it.id }).startsWith(1L, 2L).endsWith(249L, 250L)
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 목록 + totalCount = 0")
        fun emptyResult() {
            every { repository.findAllSnapshot() } returns emptyList()

            val result = service().searchAll("cli-1", "10.0.0.1")

            assertThat(result.items).isEmpty()
            assertThat(result.totalCount).isZero()
        }

        @Test
        @DisplayName("totalCount 는 항상 items 건수와 일치")
        fun totalCountMatchesItems() {
            every { repository.findAllSnapshot() } returns (1L..7L).map { entity(it) }

            val result = service().searchAll("cli-1", "10.0.0.1")

            assertThat(result.totalCount).isEqualTo(result.items.size)
        }
    }

    @Nested
    @DisplayName("row 변환")
    inner class RowMapping {

        @Test
        @DisplayName("조직 트리 레벨 2~5 코드/명칭이 모두 노출된다")
        fun exposesEveryTreeLevel() {
            every { repository.findAllSnapshot() } returns listOf(
                snapshot(
                    Organization(
                        id = 1,
                        name = "원주1지점",
                        costCenterLevel2 = "CC2", orgCodeLevel2 = "OC2", orgNameLevel2 = "영업본부",
                        costCenterLevel3 = "CC3", orgCodeLevel3 = "OC3", orgNameLevel3 = "Retail사업부",
                        costCenterLevel4 = "CC4", orgCodeLevel4 = "OC4", orgNameLevel4 = "중부지사",
                        costCenterLevel5 = "CC5", orgCodeLevel5 = "OC5", orgNameLevel5 = "원주1지점",
                    )
                )
            )

            val row = service().searchAll(null, "10.0.0.1").items.single()

            assertThat(row.costCenterLevel2).isEqualTo("CC2")
            assertThat(row.orgNameLevel3).isEqualTo("Retail사업부")
            assertThat(row.orgCodeLevel4).isEqualTo("OC4")
            assertThat(row.costCenterLevel5).isEqualTo("CC5")
            assertThat(row.orgNameLevel5).isEqualTo("원주1지점")
        }

        @Test
        @DisplayName("관계는 객체를 펼치지 않고 FK id + *_sfid 로만 노출 — 쿼리에서 함께 가져온 값을 쓴다")
        fun relationsExposedAsIdsOnly() {
            every { repository.findAllSnapshot() } returns listOf(
                snapshot(
                    Organization(id = 1, name = "조직").apply {
                        ownerSfid = "005OWNER"
                        createdBySfid = "005CREATE"
                        lastModifiedBySfid = "005MODIFY"
                    },
                    ownerUserId = 11,
                    ownerGroupId = 22,
                    createdById = 33,
                    lastModifiedById = 44,
                )
            )

            val row = service().searchAll(null, "10.0.0.1").items.single()

            assertThat(row.ownerUserId).isEqualTo(11)
            assertThat(row.ownerGroupId).isEqualTo(22)
            assertThat(row.createdById).isEqualTo(33)
            assertThat(row.lastModifiedById).isEqualTo(44)
            assertThat(row.ownerSfid).isEqualTo("005OWNER")
            assertThat(row.createdBySfid).isEqualTo("005CREATE")
            assertThat(row.lastModifiedBySfid).isEqualTo("005MODIFY")
        }
    }

    @Nested
    @DisplayName("audit")
    inner class Audit {

        @Test
        @DisplayName("정상 조회 시 REQUEST_ACCEPTED 적재 + received_count = 노출 건수")
        fun auditAccepted() {
            every { repository.findAllSnapshot() } returns (1L..3L).map { entity(it) }
            val auditSlot = slot<OvipInboundAudit>()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            service().searchAll("cli-1", "10.0.0.1")

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(OvipInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditSlot.captured.clientId).isEqualTo("cli-1")
            assertThat(auditSlot.captured.clientIp).isEqualTo("10.0.0.1")
            assertThat(auditSlot.captured.receivedCount).isEqualTo(3)
            assertThat(auditSlot.captured.endpoint).isEqualTo(OvipOrganizationQueryService.ENDPOINT)
            assertThat(auditSlot.captured.scope).isEqualTo(OvipInboundScopes.READ)
        }

        @Test
        @DisplayName("조회 결과가 없어도 audit 은 남는다 (received_count = 0)")
        fun auditOnEmptyResult() {
            every { repository.findAllSnapshot() } returns emptyList()
            val auditSlot = slot<OvipInboundAudit>()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            service().searchAll("cli-1", "10.0.0.1")

            assertThat(auditSlot.captured.receivedCount).isZero()
        }
    }
}
