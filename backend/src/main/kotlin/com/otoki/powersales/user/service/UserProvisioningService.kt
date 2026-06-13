package com.otoki.powersales.user.service

import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.event.EmployeeCreatedEvent
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Employee 신규 생성에 따른 User 행 생성 책임을 가진 user 패키지 도메인 서비스.
 *
 * ## 레거시 매핑
 * - SF `IF_REST_SAP_EmployeeMaster.upsertUser(@future)` (cls:200-313) 동등.
 * - Employee 트랜잭션 commit 이후 별도 트랜잭션 + 별도 스레드에서 User INSERT 수행.
 *
 * ## 분기 정책 (SF 동등)
 * 1. Email (`workEmail` 우선, fallback `email`) 부재 시 User INSERT skip
 *    — SF `IF_REST_SAP_EmployeeMaster.cls:281` `obj.DKRetail__Email__c != null` 가드 동등
 * 2. 이미 같은 username 의 User 존재 시 멱등 skip
 * 3. 임시 비밀번호: `{employeeCode}{birthDate MMdd}`, birthDate 부재 시 `0000`
 * 4. `passwordChangeRequired = true` (Web 최초 로그인 시 강제 변경)
 * 5. `profileId`: 시점에 알 수 있는 `UserRole` 시드값 기준 매핑.
 *    운영 경로는 발령 후처리에서 [com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater]
 *    가 `EmployeeProfileResolver.resolveProfileId` 로 재산출하여 정정한다 (SF 분기 동등).
 *
 * ## 호출 경로
 * - SAP 인바운드: [com.otoki.powersales.employee.service.EmployeeUpsertService] 가
 *   Employee INSERT 후 [EmployeeCreatedEvent] 발행 → 본 서비스의
 *   [handleEmployeeCreated] 가 `AFTER_COMMIT + @Async` 로 수신.
 * - 로컬 시드: [com.otoki.powersales.common.config.LocalDataInitializer] 가
 *   [provisionForSeed] 를 동기 호출 (시드는 트랜잭션 분리 의미 없음).
 */
@Service
class UserProvisioningService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val profileRepository: ProfileRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * SAP 인바운드 경로 진입점.
     *
     * `AFTER_COMMIT`: Employee 트랜잭션이 commit 된 후에만 발화 (rollback 시 호출 안 됨).
     * `@Async`: 별도 스레드에서 실행 → 메인 요청 스레드(SAP HTTP 응답) 와 분리.
     * `Propagation.REQUIRES_NEW`: 항상 새 트랜잭션. 실패 시 본 트랜잭션만 rollback.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleEmployeeCreated(event: EmployeeCreatedEvent) {
        try {
            provision(
                employeeCode = event.employeeCode,
                name = event.name,
                workEmail = event.workEmail,
                email = event.email,
                birthDate = event.birthDate,
                role = event.role,
                appLoginActive = event.appLoginActive,
                costCenterCode = event.costCenterCode,
            )
        } catch (ex: Exception) {
            // SF @future 동등: 메인(Employee) 트랜잭션에 영향 주지 않고 본 트랜잭션만 rollback.
            // 관측은 로그에 의존 (향후 outbox 패턴 도입 여지).
            log.warn("User 자동 생성 실패: employeeCode={}, error={}", event.employeeCode, ex.message, ex)
        }
    }

    /**
     * 시드 / 수동 호출 진입점 (동기, 호출자의 트랜잭션에 합류).
     *
     * 시드 데이터는 부트스트랩 단계 단일 호출이므로 트랜잭션 분리 의미가 없다.
     * 호출자가 [org.springframework.transaction.support.TransactionTemplate] 으로 감싸 호출.
     */
    fun provisionForSeed(
        employeeCode: String,
        name: String,
        workEmail: String?,
        email: String?,
        birthDate: String?,
        role: String?,
        appLoginActive: Boolean?,
        costCenterCode: String? = null,
        isSalesSupport: Boolean = false,
        encodedPassword: String? = null,
        passwordChangeRequired: Boolean = true,
    ) {
        provision(
            employeeCode = employeeCode,
            name = name,
            workEmail = workEmail,
            email = email,
            birthDate = birthDate,
            role = role,
            appLoginActive = appLoginActive,
            costCenterCode = costCenterCode,
            isSalesSupport = isSalesSupport,
            overrideEncodedPassword = encodedPassword,
            passwordChangeRequired = passwordChangeRequired,
        )
    }

    /**
     * 공통 본문. `handleEmployeeCreated` (비동기) 와 `provisionForSeed` (동기) 양쪽이 위임한다.
     *
     * skip 조건: (a) email 부재, (b) 같은 username User 이미 존재.
     */
    private fun provision(
        employeeCode: String,
        name: String,
        workEmail: String?,
        email: String?,
        birthDate: String?,
        role: String?,
        appLoginActive: Boolean?,
        costCenterCode: String? = null,
        isSalesSupport: Boolean = false,
        overrideEncodedPassword: String? = null,
        passwordChangeRequired: Boolean = true,
    ) {
        // SF 레거시 IF_REST_SAP_EmployeeMaster.cls:281 동등 — Email 부재 시 User 생성 skip.
        val resolvedEmail = workEmail?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: run {
                log.info("Email 부재 — User 생성 skip: employeeCode={}", employeeCode)
                return
            }

        // 멱등 가드 — 같은 username 의 User 가 이미 존재하면 skip.
        if (userRepository.findByUsername(resolvedEmail) != null) {
            log.info("이미 존재하는 User — skip: username={}", resolvedEmail)
            return
        }

        val tempPassword = "$employeeCode${birthDate?.takeLast(BIRTH_SUFFIX_LENGTH) ?: BIRTH_SUFFIX_FALLBACK}"
        val encoded = overrideEncodedPassword ?: passwordEncoder.encode(tempPassword)!!

        val user = User(
            username = resolvedEmail,
            email = resolvedEmail,
            employeeCode = employeeCode,
            name = name,
            password = encoded,
            passwordChangeRequired = passwordChangeRequired,
            isActive = appLoginActive ?: true,
            profileId = profileIdFor(role),
            isSalesSupport = isSalesSupport,
            costCenterCode = costCenterCode,
            isDeleted = false,
        )
        userRepository.save(user)
        log.info("User 자동 생성 완료: employeeCode={}, username={}", employeeCode, resolvedEmail)
    }

    /**
     * SF AppAuthority picklist value → Profile.id 산출.
     *
     * [SystemAdminProfilePolicy.profileNameForRole] 의 분기를 Profile.name → id 로 변환.
     * Profile entity 부재 시 null — SF Stage1 Profile 적재 (dev/prod) 또는 LocalDataInitializer (local) 가 보장하지만 동시성 시점에는 fallback null 허용.
     * 운영 환경에서는 발령(Appointment) 후처리 시 [EmployeeProfileResolver.resolveProfileId] 가
     * Org__c + jikchak 기반으로 재산출하여 정정한다.
     */
    private fun profileIdFor(role: String?): Long? {
        val name = SystemAdminProfilePolicy.profileNameForRole(role)
        return profileRepository.findByName(name)?.id
    }

    companion object {
        private const val BIRTH_SUFFIX_LENGTH = 4
        private const val BIRTH_SUFFIX_FALLBACK = "0000"
    }
}
