package com.otoki.powersales.external.rdp.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeSnapshotRow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 사원(Employee) 외부 조회용 평면 row — **entity 전 컬럼 노출**.
 *
 * MFEIS([MfeisScheduleRow]) 는 레거시 SF 화면/엑셀 노출 컬럼에 정합하는 축소 필드 셋이지만, 본 DTO 는
 * "해당 엔티티의 필드를 모두 조회" 요구에 따라 Employee entity 의 매핑 컬럼 전량을 노출한다.
 * (SF 레거시에는 사원을 외부로 인출하는 REST/아웃바운드 선례가 없어 정합 기준으로 삼을 필드 셋이 없다 —
 *  레거시 목록 컨트롤러는 `Id, Name, DKRetail__EmpCode__c, DKRetail__OrgName__c` 4개만 노출했다.)
 *
 * **제외 대상 — `employeeInfo`**: `employee_info` 는 별도 테이블이며 비밀번호 해시(`password`),
 * 기기 식별자(`deviceUuid`), FCM 토큰을 담은 **인증 정보**라 외부 노출 대상이 아니다. 본 DTO 는
 * `employee` 테이블 컬럼만 노출한다.
 *
 * 관계(ownerUser / ownerGroup / createdBy / lastModifiedBy / manager / postponedAppointment)는
 * **객체를 펼치지 않고** FK id 와 함께 entity 가 이미 보유한 `*_sfid` 컬럼으로만 노출한다.
 * LAZY 관계를 직렬화하면 row 마다 추가 쿼리(N+1)가 발생하기 때문이다.
 * FK id 는 entity 의 관계 필드가 아니라 [EmployeeSnapshotRow] 가 쿼리에서 함께 가져온 값을 쓴다 —
 * 관계 필드에 의존하지 않아야 대량 조회에서 추가 쿼리 여지가 원천적으로 없다.
 *
 * [id] 는 keyset 커서(다음 페이지 조회 기준)로도 사용된다.
 */
data class EmployeeRow(
    /** PK — keyset 커서 기준. */
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("sfid")
    val sfid: String?,

    /** 사번 — SF 에서 blank 인 사원(외부 위탁 진열사원 등)이 존재하여 nullable. */
    @JsonProperty("employeeCode")
    val employeeCode: String?,

    @JsonProperty("name")
    val name: String?,

    @JsonProperty("birthDate")
    val birthDate: String?,

    /** 재직상태명. */
    @JsonProperty("status")
    val status: String?,

    @JsonProperty("appLoginActive")
    val appLoginActive: Boolean?,

    /** 현장사원 App 권한 (조장 / 여사원 / 지점장 / AccountViewAll). */
    @JsonProperty("role")
    val role: String?,

    @JsonProperty("orgName")
    val orgName: String?,

    /** 소속 조직 HR OrgCode (컬럼명은 CostCenterCode 이나 값 도메인은 OrgCode 축). */
    @JsonProperty("costCenterCode")
    val costCenterCode: String?,

    @JsonProperty("workPhone")
    val workPhone: String?,

    @JsonProperty("phone")
    val phone: String?,

    @JsonProperty("homePhone")
    val homePhone: String?,

    @JsonProperty("workEmail")
    val workEmail: String?,

    @JsonProperty("email")
    val email: String?,

    /** 성별 — DB 저장값과 동일한 SF 원본값(displayName, `남`/`여`). */
    @JsonProperty("gender")
    val gender: String?,

    @JsonProperty("startDate")
    val startDate: LocalDate?,

    @JsonProperty("endDate")
    val endDate: LocalDate?,

    @JsonProperty("agreementFlag")
    val agreementFlag: Boolean?,

    @JsonProperty("isDeleted")
    val isDeleted: Boolean?,

    /** 전문행사조 — DB 저장값과 동일한 표시명(displayName). */
    @JsonProperty("professionalPromotionTeam")
    val professionalPromotionTeam: String?,

    @JsonProperty("jikchak")
    val jikchak: String?,

    @JsonProperty("jikwee")
    val jikwee: String?,

    @JsonProperty("jikgub")
    val jikgub: String?,

    @JsonProperty("workType")
    val workType: String?,

    @JsonProperty("jobCode")
    val jobCode: String?,

    @JsonProperty("workArea")
    val workArea: String?,

    @JsonProperty("jikjong")
    val jikjong: String?,

    @JsonProperty("appointmentDate")
    val appointmentDate: LocalDate?,

    @JsonProperty("ordDetailNode")
    val ordDetailNode: String?,

    @JsonProperty("crmWorkStartDate")
    val crmWorkStartDate: LocalDate?,

    /** CC Code — [costCenterCode] 와 별개 필드 (DKRetail__CostCenterCode__c). */
    @JsonProperty("dkCostCenterCode")
    val dkCostCenterCode: String?,

    @JsonProperty("locationCode")
    val locationCode: String?,

    @JsonProperty("totalAnnualLeave")
    val totalAnnualLeave: BigDecimal?,

    @JsonProperty("usedAnnualLeave")
    val usedAnnualLeave: BigDecimal?,

    @JsonProperty("lockingFlag")
    val lockingFlag: Boolean?,

    /** 데이터출처 — `@Enumerated(STRING)` 이라 enum name 이 그대로 저장/노출된다. */
    @JsonProperty("origin")
    val origin: String?,

    @JsonProperty("officePhone")
    val officePhone: String?,

    /** 근무형태(CRM) — DB 저장값과 동일한 표시명(displayName). */
    @JsonProperty("crmWorkType")
    val crmWorkType: String?,

    // -- 관계 FK (객체 미전개 — sfid + FK id 만) --

    @JsonProperty("managerSfid")
    val managerSfid: String?,

    @JsonProperty("managerId")
    val managerId: Long?,

    @JsonProperty("postponedAppointmentSfid")
    val postponedAppointmentSfid: String?,

    @JsonProperty("postponedAppointmentId")
    val postponedAppointmentId: Long?,

    /** OwnerId — SF polymorphic (`005` = User / `00G` = Group). FK 는 아래 둘 중 하나만 채워진다. */
    @JsonProperty("ownerSfid")
    val ownerSfid: String?,

    @JsonProperty("ownerUserId")
    val ownerUserId: Long?,

    @JsonProperty("ownerGroupId")
    val ownerGroupId: Long?,

    @JsonProperty("createdBySfid")
    val createdBySfid: String?,

    @JsonProperty("createdById")
    val createdById: Long?,

    @JsonProperty("lastModifiedBySfid")
    val lastModifiedBySfid: String?,

    @JsonProperty("lastModifiedById")
    val lastModifiedById: Long?,

    // -- audit (BaseEntity) --

    @JsonProperty("createdAt")
    val createdAt: LocalDateTime?,

    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime?
) {
    companion object {
        /**
         * 스냅샷 row(entity + 관계 FK) → 외부 노출 row 변환.
         *
         * entity 의 관계 필드는 **읽지 않는다** — FK 는 [snapshot] 이 쿼리에서 이미 가져왔다.
         * `employeeInfo`(인증 정보) 역시 의도적으로 읽지 않는다 (노출 금지 + 추가 SELECT 방지).
         */
        fun from(snapshot: EmployeeSnapshotRow): EmployeeRow = with(snapshot.employee) { EmployeeRow(
            id = id,
            sfid = sfid,
            employeeCode = employeeCode,
            name = name,
            birthDate = birthDate,
            status = status,
            appLoginActive = appLoginActive,
            role = role,
            orgName = orgName,
            costCenterCode = costCenterCode,
            workPhone = workPhone,
            phone = phone,
            homePhone = homePhone,
            workEmail = workEmail,
            email = email,
            gender = gender?.displayName,
            startDate = startDate,
            endDate = endDate,
            agreementFlag = agreementFlag,
            isDeleted = isDeleted,
            professionalPromotionTeam = professionalPromotionTeam?.displayName,
            jikchak = jikchak,
            jikwee = jikwee,
            jikgub = jikgub,
            workType = workType,
            jobCode = jobCode,
            workArea = workArea,
            jikjong = jikjong,
            appointmentDate = appointmentDate,
            ordDetailNode = ordDetailNode,
            crmWorkStartDate = crmWorkStartDate,
            dkCostCenterCode = dkCostCenterCode,
            locationCode = locationCode,
            totalAnnualLeave = totalAnnualLeave,
            usedAnnualLeave = usedAnnualLeave,
            lockingFlag = lockingFlag,
            origin = origin?.name,
            officePhone = officePhone,
            crmWorkType = crmWorkType?.displayName,
            managerSfid = managerSfid,
            managerId = snapshot.managerId,
            postponedAppointmentSfid = postponedAppointmentSfid,
            postponedAppointmentId = snapshot.postponedAppointmentId,
            ownerSfid = ownerSfid,
            ownerUserId = snapshot.ownerUserId,
            ownerGroupId = snapshot.ownerGroupId,
            createdBySfid = createdBySfid,
            createdById = snapshot.createdById,
            lastModifiedBySfid = lastModifiedBySfid,
            lastModifiedById = snapshot.lastModifiedById,
            createdAt = createdAt,
            updatedAt = updatedAt,
        ) }
    }
}
