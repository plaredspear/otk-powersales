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
