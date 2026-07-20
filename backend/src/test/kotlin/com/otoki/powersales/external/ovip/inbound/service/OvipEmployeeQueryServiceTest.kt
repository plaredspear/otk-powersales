package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.enums.Gender
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeSnapshotRow
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.inbound.config.OvipInboundProperties
import com.otoki.powersales.external.ovip.inbound.dto.EmployeeRow
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotSearchRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OvipEmployeeQueryService 테스트")
class OvipEmployeeQueryServiceTest {

    private val repository: EmployeeRepository = mockk()
    private val auditService: OvipInboundAuditService = mockk(relaxed = true)

    private fun service(
        defaultPageSize: Int = 100,
        maxPageSize: Int = 500,
    ): OvipEmployeeQueryService {
        val props = OvipInboundProperties(
            employee = OvipInboundProperties.Pagination(
                defaultPageSize = defaultPageSize,
                maxPageSize = maxPageSize,
            )
        )
        return OvipEmployeeQueryService(repository, props, auditService)
    }

    private fun entity(id: Long): EmployeeSnapshotRow =
        snapshot(Employee(id = id, employeeCode = "E$id", name = "사원-$id"))

    /** 관계 FK 는 쿼리에서 함께 조회되므로 테스트도 동일하게 snapshot 으로 감싼다. */
    private fun snapshot(
        employee: Employee,
        ownerUserId: Long? = null,
        ownerGroupId: Long? = null,
        createdById: Long? = null,
        lastModifiedById: Long? = null,
        managerId: Long? = null,
        postponedAppointmentId: Long? = null,
    ) = EmployeeSnapshotRow(
        employee, ownerUserId, ownerGroupId, createdById,
        lastModifiedById, managerId, postponedAppointmentId,
    )

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
        @DisplayName("gender 는 enum name 이 아니라 DB 저장값(displayName '남'/'여') 으로 노출")
        fun genderUsesDisplayName() {
            val row = EmployeeRow.from(
                snapshot(Employee(id = 1, employeeCode = "E1", name = "홍길동", gender = Gender.FEMALE))
            )

            assertThat(row.gender).isEqualTo("여")
            assertThat(row.gender).isNotEqualTo(Gender.FEMALE.name)
        }

        @Test
        @DisplayName("origin 은 @Enumerated(STRING) 이라 enum name 으로 노출")
        fun originUsesName() {
            val row = EmployeeRow.from(
                snapshot(Employee(id = 1, employeeCode = "E1", name = "홍길동", origin = EmployeeOrigin.SAP))
            )

            assertThat(row.origin).isEqualTo(EmployeeOrigin.SAP.name)
        }

        @Test
        @DisplayName("관계 미연결 시 FK id 는 null")
        fun nullRelations() {
            val row = EmployeeRow.from(entity(1))

            assertThat(row.ownerUserId).isNull()
            assertThat(row.ownerGroupId).isNull()
            assertThat(row.createdById).isNull()
            assertThat(row.lastModifiedById).isNull()
            assertThat(row.managerId).isNull()
            assertThat(row.postponedAppointmentId).isNull()
        }

        @Test
        @DisplayName("entity 필드가 row 로 그대로 전달됨")
        fun fieldsPassthrough() {
            val row = EmployeeRow.from(
                snapshot(Employee(
                    id = 7,
                    employeeCode = "E7",
                    name = "홍길동",
                    orgName = "영업1팀",
                    costCenterCode = "41010",
                    status = "재직",
                    role = "여사원",
                ))
            )

            assertThat(row.id).isEqualTo(7L)
            assertThat(row.employeeCode).isEqualTo("E7")
            assertThat(row.name).isEqualTo("홍길동")
            assertThat(row.orgName).isEqualTo("영업1팀")
            assertThat(row.costCenterCode).isEqualTo("41010")
            assertThat(row.status).isEqualTo("재직")
            assertThat(row.role).isEqualTo("여사원")
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
            assertThat(auditSlot.captured.endpoint).isEqualTo(OvipEmployeeQueryService.ENDPOINT)
        }
    }
}
