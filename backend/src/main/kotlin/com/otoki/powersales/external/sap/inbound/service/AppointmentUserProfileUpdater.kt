package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.user.service.EmployeeProfileResolver
import com.otoki.powersales.user.service.UserRoleResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AppointmentUserProfileUpdater(
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository,
    private val systemCodeMasterRepository: SystemCodeMasterRepository,
    private val userRepository: UserRepository,
    private val employeeProfileResolver: EmployeeProfileResolver,
    private val userRoleResolver: UserRoleResolver,
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: AdminDataScopeCache,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val PROMOTION_JOB_CODES = setOf("A049", "A053", "A055")
        private val ORG_PREFIX_GROUP1 = setOf("3228", "3229", "3230", "3231")
        private val ORG_PREFIX_GROUP2 = setOf("3233", "3234", "3235")
        private val CODE_GROUP_MAP = mapOf(
            "jikchak" to "H20020",
            "jikwee" to "H20030",
            "jikgub" to "H20010",
            "workType" to "H10050",
            "jobCode" to "H10060"
        )
    }

    @Transactional
    fun updateUserProfiles(appointments: List<Appointment>) {
        updateUserProfiles(appointments, LocalDate.now())
    }

    @Transactional
    internal fun updateUserProfiles(appointments: List<Appointment>, today: LocalDate) {
        val codeMap = loadSystemCodeMap()

        var updatedCount = 0
        var skippedCount = 0

        for (appointment in appointments) {
            try {
                if (!appointment.empCodeExist) {
                    skippedCount++
                    continue
                }

                val employeeCode = appointment.employeeCode
                if (employeeCode == null) {
                    skippedCount++
                    continue
                }

                // afterOrgCode(발령 조직코드) null 은 게이트 없이 진행한다 — SF
                // AppointmentTriggerHanlder.cls:101-204 는 사원 존재 외 게이트가 없고,
                // OrgCode__c null 이면 CostCenterCode__c = null 로 그대로 반영된다.

                val employee = employeeRepository.findByEmployeeCode(employeeCode).orElse(null)
                if (employee == null) {
                    skippedCount++
                    continue
                }

                val appointDate = appointment.appointDate
                if (appointDate == null) {
                    skippedCount++
                    continue
                }

                if (appointDate.isAfter(today)) {
                    applyReservedAppointment(employee, appointment, appointDate, codeMap)
                } else {
                    applyImmediateAppointment(employee, appointment, appointDate, codeMap)
                }

                updateUserProfileCache(employee)

                updatedCount++
            } catch (e: Exception) {
                log.warn("발령 후처리 실패: employeeCode={}, error={}",
                    appointment.employeeCode, e.message)
                skippedCount++
            }
        }

        log.info("발령 후처리 완료: updated={}, skipped={}", updatedCount, skippedCount)
    }

    fun applyImmediateAppointment(
        employee: Employee,
        appointment: Appointment,
        appointDate: LocalDate,
        codeMap: Map<String, String>
    ) {
        employee.costCenterCode = appointment.afterOrgCode
        employee.orgName = resolveOrgName(appointment.afterOrgCode, appointment.afterOrgName)

        employee.jikchak = resolveCode(codeMap, "H20020", appointment.jikchak)
        employee.jikwee = resolveCode(codeMap, "H20030", appointment.jikwee)
        employee.jikgub = resolveCode(codeMap, "H20010", appointment.jikgub)
        employee.workType = resolveCode(codeMap, "H10050", appointment.workType)
        employee.jobCode = resolveCode(codeMap, "H10060", appointment.jobCode)
        employee.workArea = appointment.workArea
        employee.jikjong = appointment.jikjong
        employee.appointmentDate = appointDate
        employee.ordDetailNode = appointment.ordDetailNode
        // 예약 필드(crmWorkStartDate/postponedAppointment) 는 건드리지 않는다 — SF 트리거 즉시 경로
        // (AppointmentTriggerHanlder.cls:131-178) 는 upsert 에 예약 필드를 싣지 않아 기존 값이 유지된다.
        // 미소진 예약 보유 중 별개 즉시 발령이 오면 예약은 살아남아 발령일에 배치가 반영한다.
        // 예약 소진은 배치 전용 [applyPostponedAppointment] (cls:140-141) 에서만 수행.

        applyJobCodeAuthority(employee, appointment.jobCode, appointment.jikchak)
        applyProfessionalPromotionTeamReset(
            employee, appointment.jobCode, appointment.jikchak, appointment.ordDetailNode
        )
    }

    private fun applyReservedAppointment(
        employee: Employee,
        appointment: Appointment,
        appointDate: LocalDate,
        codeMap: Map<String, String>
    ) {
        employee.crmWorkStartDate = appointDate
        // SF AppointmentTriggerHanlder.cls:197-198 정합 — 예정일 + 반영 대상 발령 참조를 함께 기록한다.
        // 참조가 없으면 발령일 도래 시 배치가 "최신 발령" 을 추측해야 하고, 동일 발령일 다건에서
        // 옛 발령을 집는 사고로 이어진다.
        employee.postponedAppointment = appointment
        // SF 유예 경로(cls:179-202) 는 AppAuthority/APPLoginActive + 예약 2필드만 기록한다 —
        // 전문행사조 초기화는 즉시 반영 경로 전용이므로 여기서 호출하지 않는다.
        applyJobCodeAuthority(employee, appointment.jobCode, appointment.jikchak)
    }

    /**
     * 예약 발령의 발령일 도래 반영 — SF `PostponedAppointmentBatch.cls:100-143` 정합 (배치 전용).
     *
     * SF 배치는 트리거 즉시 경로와 필드 구성이 **의도적으로 다르며**, 그 비대칭까지 그대로 재현한다:
     * - 발령일자는 발령 레코드의 값 그대로 (`cls:102`) — 배치 실행일 아님
     * - 조직명에 유통총괄1부/2부 prefix 없음 (`cls:104` — 트리거 `cls:160-167` 과 비대칭)
     * - AppAuthority 판정 코드셋은 A055/A049 만 (`cls:116` 2024-01-02 수정 — 트리거의 A053 미포함)
     * - 전문행사조 초기화 없음 (트리거 즉시 경로 전용)
     * - 예약 2필드(참조/예정일) 소진 (`cls:140-141`)
     * - User(Profile/Role) 갱신 없음 — SF 배치는 UserRole 갱신 코드를 주석 처리(`cls:90-99`)해
     *   의도적으로 미수행. 사용자 파생 캐시는 다음 발령 인바운드(트리거 경로)에서 따라잡는다.
     */
    fun applyPostponedAppointment(
        employee: Employee,
        appointment: Appointment,
        codeMap: Map<String, String>
    ) {
        employee.appointmentDate = appointment.appointDate
        employee.costCenterCode = appointment.afterOrgCode
        employee.orgName = appointment.afterOrgName
        employee.jikchak = resolveCode(codeMap, "H20020", appointment.jikchak)
        employee.jikwee = resolveCode(codeMap, "H20030", appointment.jikwee)
        employee.jikgub = resolveCode(codeMap, "H20010", appointment.jikgub)
        employee.workType = resolveCode(codeMap, "H10050", appointment.workType)
        employee.jobCode = resolveCode(codeMap, "H10060", appointment.jobCode)
        employee.workArea = appointment.workArea
        employee.jikjong = appointment.jikjong
        employee.ordDetailNode = appointment.ordDetailNode

        if (appointment.jobCode == "A055" || appointment.jobCode == "A049") {
            if (appointment.jikchak == "D0052") {
                employee.role = AppAuthority.LEADER
            } else {
                employee.role = AppAuthority.WOMAN
            }
            employee.appLoginActive = true
        }

        // 유예된 발령정보 초기화 (cls:140-141)
        employee.postponedAppointment = null
        employee.crmWorkStartDate = null
    }

    /**
     * 유예 발령의 관리자 수동 승인 반영 — SF `ManualConfirmPostponedAppController.confirmPostponedAppointment`
     * (cls 전문) 정합. web admin 사원 상세의 "발령정보 승인" 액션 전용.
     *
     * SF 수동 확정은 트리거 즉시 경로·소진 배치와 또 다른 **제3의 필드 구성**이며, 비대칭까지 그대로 재현한다:
     * - 날짜 게이트 없음 — 예정일(crmWorkStartDate)이 미래든 이미 지났든 참조된 발령을 즉시 반영
     * - 조직명에 유통총괄1부/2부 prefix 없음 (배치와 같고 트리거와 다름)
     * - 직책은 조건부 반영: 현재 직책 보유 시 변환 반영 / 현재 null 이면 발령 직책이 D0098·D0051 일 때만
     *   반영 / 그 외 미갱신 (SF 2023-04-10/06-02 수정 이력 그대로)
     * - 직위/직급/직군/직무코드는 [resolveCodeStrict] — 코드 null 또는 SystemCodeMaster 미등재 시
     *   **액션 전체 실패** (SF 는 sysCodeMapGroup.get(그룹) null 의 uncaught NPE 로 update DML 전에 죽는다)
     * - OrdDetailNode / AppAuthority / APPLoginActive / 전문행사조 **미반영** (SF 수동 확정에 해당 코드 없음)
     * - 예약 2필드(참조/예정일) 소진
     * - EmpCode 재대입(cls `e.DKRetail__EmpCode__c = a.EmployeeCode__c`) 은 미재현 — 신규 employeeCode 는
     *   불변(val)이며, SF 도 참조 발령의 사번은 예약 시점 사원 본인 사번이라 실차이 없음
     *
     * User(Profile) 갱신은 SF 가 `AppointmentTriggerHanlder.updateUser`(@future) 를 호출하므로 **수행 대상** —
     * 호출자가 [updateUserProfileCache] 를 이어서 호출한다 (배치와 달리 미생략).
     */
    fun applyManualConfirmAppointment(
        employee: Employee,
        appointment: Appointment,
        codeMap: Map<String, String>
    ) {
        // 유예된 발령정보 초기화 (SF 코드 순서 그대로 — 동일 트랜잭션이라 순서는 결과 무관)
        employee.crmWorkStartDate = null
        employee.postponedAppointment = null

        employee.appointmentDate = appointment.appointDate
        employee.costCenterCode = appointment.afterOrgCode
        employee.orgName = appointment.afterOrgName

        val currentJikchak = employee.jikchak
        if (currentJikchak != null) {
            if (currentJikchak != "") {
                employee.jikchak = resolveCodeStrict(codeMap, "H20020", appointment.jikchak)
            }
        } else if (appointment.jikchak == "D0098" || appointment.jikchak == "D0051") {
            employee.jikchak = resolveCodeStrict(codeMap, "H20020", appointment.jikchak)
        }

        employee.jikwee = resolveCodeStrict(codeMap, "H20030", appointment.jikwee)
        employee.jikgub = resolveCodeStrict(codeMap, "H20010", appointment.jikgub)
        employee.workType = resolveCodeStrict(codeMap, "H10050", appointment.workType)
        employee.jobCode = resolveCodeStrict(codeMap, "H10060", appointment.jobCode)
        employee.workArea = appointment.workArea
        employee.jikjong = appointment.jikjong
    }

    internal fun applyJobCodeAuthority(employee: Employee, jobCode: String?, jikchak: String?) {
        if (jobCode == null || jobCode !in PROMOTION_JOB_CODES) return

        if (jikchak == "D0052") {
            employee.role = AppAuthority.LEADER
            employee.appLoginActive = true
        } else {
            employee.role = AppAuthority.WOMAN
            employee.appLoginActive = true
        }
    }

    /**
     * 전문행사조 초기화 (SF `ProfessionalPromotionTeam__c = '일반'` — 신규는 미배정=null 컨벤션).
     *
     * SF `AppointmentTriggerHanlder.cls:143-151` 정합 — 판정은 사원의 현재 role 이 아니라 **발령
     * 레코드의 여사원 분기**(직무코드 ∈ A049/A053/A055 AND 직책 ≠ D0052) 기준이다. role 기준으로
     * 판정하면 비여사원 직무코드(A034 영업직 등) 발령을 받은 기존 여사원의 전문행사조까지 초기화되어
     * SF 와 어긋난다.
     */
    internal fun applyProfessionalPromotionTeamReset(
        employee: Employee,
        jobCode: String?,
        jikchak: String?,
        ordDetailNode: String?
    ) {
        if (jobCode == null || jobCode !in PROMOTION_JOB_CODES) return
        if (jikchak == "D0052") return
        if (ordDetailNode != "승진") {
            employee.professionalPromotionTeam = null
        }
    }

    internal fun resolveOrgName(afterOrgCode: String?, afterOrgName: String?): String? {
        if (afterOrgCode == null || afterOrgName == null) return afterOrgName
        return when {
            afterOrgCode in ORG_PREFIX_GROUP1 -> "유통총괄1부$afterOrgName"
            afterOrgCode in ORG_PREFIX_GROUP2 -> "유통총괄2부$afterOrgName"
            else -> afterOrgName
        }
    }

    /**
     * SystemCodeMaster 코드 → 한글명 변환.
     *
     * SF `AppointmentTriggerHanlder.cls:169-173` 정합 — 매핑이 없으면 **null** 을 저장한다
     * (`sysCodeMapGroup.get(g).get(c)` 미스 시 null). 원시코드(A034 등) 를 fallback 으로 저장하면
     * 한글명 기준으로 조회하는 화면/리포트에서 누락과 동일한데 null 과 달리 눈에 띄지 않는다.
     */
    internal fun resolveCode(codeMap: Map<String, String>, groupCode: String, detailCode: String?): String? {
        if (detailCode == null) return null
        return codeMap["$groupCode:$detailCode"]
    }

    /**
     * SystemCodeMaster 코드 → 한글명 **엄격** 변환 — 수동 발령 승인 전용.
     *
     * SF `ManualConfirmPostponedAppController` 정합 — 변환 대상 코드는 null 삼항 가드 없이
     * `sysCodeMapGroup.get(그룹).get(코드)` 로 직접 접근하므로, 코드가 null 이거나 SystemCodeMaster
     * 미등재면 그룹 맵 자체가 조회되지 않아 uncaught NPE 로 **액션 전체가 update DML 전에 실패**한다.
     * 신규도 동일하게 예외를 던져 아무 필드도 반영되지 않은 채 실패시킨다 (트리거/배치의
     * [resolveCode] null 저장과 다른 의도적 비대칭).
     */
    internal fun resolveCodeStrict(codeMap: Map<String, String>, groupCode: String, detailCode: String?): String {
        if (detailCode == null) {
            throw IllegalStateException("발령 코드 변환 실패 — 발령 레코드 코드 누락: group=$groupCode")
        }
        return codeMap["$groupCode:$detailCode"]
            ?: throw IllegalStateException("발령 코드 변환 실패 — SystemCodeMaster 미등재: group=$groupCode, code=$detailCode")
    }

    internal fun loadSystemCodeMap(): Map<String, String> {
        val groupCodes = CODE_GROUP_MAP.values.toList()
        val codes = systemCodeMasterRepository.findByGroupCodeIn(groupCodes)
        return codes.associate { "${it.groupCode}:${it.detailCode}" to (it.detailCodeName ?: it.detailCode.orEmpty()) }
    }

    /**
     * 발령 후처리로 변경된 Employee 의 최신 상태를 기준으로 User cache 갱신.
     *
     * SF `AppointmentTriggerHanlder.cls:233-365` `updateUser(@future)` 동등 — Profile/UserRole 산출 후
     * 매칭 User 행(`User.employeeCode == Employee.employeeCode`) 의 `profileId` / `isSalesSupport` 갱신.
     * 매칭 User 행 부재 시 silently skip (마이그레이션 이전 단계 / 신규 미동기화 사원 케이스).
     */
    internal fun updateUserProfileCache(employee: Employee) {
        val user = employee.employeeCode?.let { userRepository.findByEmployeeCode(it) } ?: return
        user.profileId = employeeProfileResolver.resolveProfileId(employee) ?: user.profileId
        user.isSalesSupport = userRoleResolver.isSalesSupport(employee)
        user.costCenterCode = employee.costCenterCode
        // profileId / isSalesSupport 가 권한 산출 입력이라 변경 즉시 cache invalidate.
        adminPermissionCache.invalidate(user.id)
        adminDataScopeCache.invalidate(user.id)
    }
}
