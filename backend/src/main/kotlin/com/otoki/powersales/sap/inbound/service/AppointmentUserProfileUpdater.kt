package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.schedule.entity.Appointment
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.common.repository.SystemCodeMasterRepository
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
    private val userRoleResolver: UserRoleResolver
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

                val afterOrgCode = appointment.afterOrgCode
                if (afterOrgCode == null) {
                    skippedCount++
                    continue
                }

                val employee = employeeRepository.findByEmployeeCode(appointment.employeeCode).orElse(null)
                if (employee == null) {
                    skippedCount++
                    continue
                }

                val appointDate = appointment.appointDate

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
        employee.crmWorkStartDate = null

        applyJobCodeAuthority(employee, appointment.jobCode, appointment.jikchak)
        applyProfessionalPromotionTeamReset(employee, appointment.ordDetailNode)
    }

    private fun applyReservedAppointment(
        employee: Employee,
        appointment: Appointment,
        appointDate: LocalDate,
        codeMap: Map<String, String>
    ) {
        employee.crmWorkStartDate = appointDate
        applyJobCodeAuthority(employee, appointment.jobCode, appointment.jikchak)
        applyProfessionalPromotionTeamReset(employee, appointment.ordDetailNode)
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

    internal fun applyProfessionalPromotionTeamReset(employee: Employee, ordDetailNode: String?) {
        if (employee.role != AppAuthority.WOMAN) return
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

    internal fun resolveCode(codeMap: Map<String, String>, groupCode: String, detailCode: String?): String? {
        if (detailCode == null) return null
        val key = "$groupCode:$detailCode"
        return codeMap[key] ?: detailCode
    }

    internal fun loadSystemCodeMap(): Map<String, String> {
        val groupCodes = CODE_GROUP_MAP.values.toList()
        val codes = systemCodeMasterRepository.findByGroupCodeIn(groupCodes)
        return codes.associate { "${it.groupCode}:${it.detailCode}" to (it.detailCodeName ?: it.detailCode) }
    }

    /**
     * 발령 후처리로 변경된 Employee 의 최신 상태를 기준으로 User cache 갱신.
     *
     * SF `AppointmentTriggerHanlder.cls:233-365` `updateUser(@future)` 동등 — Profile/UserRole 산출 후
     * 매칭 User 행(`User.employeeCode == Employee.employeeCode`) 의 `profileId` / `isSalesSupport` 갱신.
     * 매칭 User 행 부재 시 silently skip (마이그레이션 이전 단계 / 신규 미동기화 사원 케이스).
     */
    internal fun updateUserProfileCache(employee: Employee) {
        val user = userRepository.findByEmployeeCode(employee.employeeCode) ?: return
        user.profileId = employeeProfileResolver.resolveProfileId(employee) ?: user.profileId
        user.isSalesSupport = userRoleResolver.isSalesSupport(employee)
        user.costCenterCode = employee.costCenterCode
    }
}
