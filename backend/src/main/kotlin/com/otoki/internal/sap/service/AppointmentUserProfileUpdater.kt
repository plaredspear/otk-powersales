package com.otoki.internal.sap.service

import com.otoki.internal.common.entity.User
import com.otoki.internal.common.repository.UserRepository
import com.otoki.internal.sap.entity.Appointment
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AppointmentUserProfileUpdater(
    private val userRepository: UserRepository,
    private val orgRepository: OrgRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun updateUserProfiles(appointments: List<Appointment>) {
        val allOrgs = orgRepository.findAll()
        val orgByCC5 = allOrgs.filter { it.costCenterLevel5 != null }
            .associateBy { it.costCenterLevel5!! }
        val orgByCC4 = allOrgs.filter { it.costCenterLevel4 != null }
            .associateBy { it.costCenterLevel4!! }
        val orgByCC3 = allOrgs.filter { it.costCenterLevel3 != null }
            .associateBy { it.costCenterLevel3!! }

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

                val user = userRepository.findByEmployeeId(appointment.employeeCode).orElse(null)
                if (user == null) {
                    skippedCount++
                    continue
                }

                val org = orgByCC5[afterOrgCode]
                    ?: orgByCC4[afterOrgCode]
                    ?: orgByCC3[afterOrgCode]

                val jikchak = appointment.jikchak ?: ""
                val newAuthority = if (org != null) {
                    determineAuthority(org.orgCodeLevel3, afterOrgCode, jikchak)
                } else {
                    null
                }

                if (newAuthority != null) {
                    user.appAuthority = newAuthority
                }
                user.costCenterCode = afterOrgCode
                user.orgName = appointment.afterOrgName

                updatedCount++
            } catch (e: Exception) {
                log.warn("발령 후처리 실패: employeeCode={}, error={}",
                    appointment.employeeCode, e.message)
                skippedCount++
            }
        }

        log.info("발령 후처리 완료: updated={}, skipped={}", updatedCount, skippedCount)
    }

    internal fun determineAuthority(
        orgCodeLevel3: String?,
        afterOrgCode: String,
        jikchak: String
    ): String {
        // Step 2: 조직 기반 프로필 결정
        if (orgCodeLevel3 == "5066") return "마케팅"

        if (orgCodeLevel3 == "3475" && !jikchak.contains("조장") && !jikchak.contains("판매")) {
            return "Staff"
        }

        if (orgCodeLevel3 == "3472") return "Staff"

        if (afterOrgCode in setOf("5397", "5398", "5639")) return "Staff"

        // Step 3: 직책 기반 프로필 결정
        if (jikchak.contains("조장") || jikchak == "판매팀장") return "조장"

        if (jikchak.contains("지점장") || jikchak.contains("팀장")) return "지점장"

        if (jikchak.contains("부장")) {
            if (jikchak.contains("사업") || jikchak == "실장") return "사업부장"
            if (jikchak == "본부장") return "영업본부장"
            return "영업부장"
        }

        if (jikchak == "본부장") return "영업본부장"

        return "영업사원"
    }
}
