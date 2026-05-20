package com.otoki.powersales.user.service

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.event.EmployeeCreatedEvent
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

@DisplayName("UserProvisioningService 테스트")
class UserProvisioningServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()

    private val service = UserProvisioningService(
        userRepository,
        passwordEncoder,
    )

    private val savedUsers = mutableListOf<User>()

    @BeforeEach
    fun setUp() {
        savedUsers.clear()
        every { passwordEncoder.encode(any<CharSequence>()) } answers { firstArg<CharSequence>().toString() + ":encoded" }
        every { userRepository.findByUsername(any<String>()) } returns null
        every { userRepository.save(any<User>()) } answers {
            val arg = firstArg<User>()
            savedUsers.add(arg)
            arg
        }
    }

    @Nested
    @DisplayName("handleEmployeeCreated - EmployeeCreatedEvent 수신 시 User INSERT")
    inner class EventHandling {

        @Test
        @DisplayName("U1 정상 — workEmail 기준 User 생성 (employee_code / username / email / password 매칭)")
        fun handleEvent_createsUser() {
            service.handleEmployeeCreated(
                EmployeeCreatedEvent(
                    employeeCode = "100123",
                    name = "홍길동",
                    workEmail = "hong@otokims.co.kr",
                    email = null,
                    birthDate = "19900315",
                    role = UserRole.WOMAN,
                    appLoginActive = true,
                )
            )

            verify { userRepository.save(any<User>()) }
            assertThat(savedUsers).hasSize(1)
            val saved = savedUsers[0]
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.username).isEqualTo("hong@otokims.co.kr")
            assertThat(saved.email).isEqualTo("hong@otokims.co.kr")
            assertThat(saved.name).isEqualTo("홍길동")
            assertThat(saved.passwordChangeRequired).isTrue()
            assertThat(saved.password).isEqualTo("1001230315:encoded")
            assertThat(saved.profileType).isEqualTo(ProfileType.SALES_REP)
            assertThat(saved.isSalesSupport).isFalse()
        }

        @Test
        @DisplayName("U2 workEmail null + email 있음 — email fallback 으로 생성")
        fun handleEvent_workEmailNull_fallsBackToEmail() {
            service.handleEmployeeCreated(
                EmployeeCreatedEvent(
                    employeeCode = "100124",
                    name = "김철수",
                    workEmail = null,
                    email = "kim@personal.com",
                    birthDate = "19850101",
                    role = UserRole.WOMAN,
                    appLoginActive = true,
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].username).isEqualTo("kim@personal.com")
        }

        @Test
        @DisplayName("U3 workEmail / email 둘 다 부재 — User 생성 skip (SF cls:281 동등)")
        fun handleEvent_noEmail_skipsCreation() {
            service.handleEmployeeCreated(
                EmployeeCreatedEvent(
                    employeeCode = "100125",
                    name = "이영희",
                    workEmail = null,
                    email = null,
                    birthDate = "19900101",
                    role = UserRole.WOMAN,
                    appLoginActive = true,
                )
            )

            verify(exactly = 0) { userRepository.save(any<User>()) }
        }

        @Test
        @DisplayName("U4 birthDate 부재 — 임시 비밀번호 = 사번 + '0000'")
        fun handleEvent_noBirthDate_passwordFallback() {
            service.handleEmployeeCreated(
                EmployeeCreatedEvent(
                    employeeCode = "100126",
                    name = "박지민",
                    workEmail = "park@otoki.com",
                    email = null,
                    birthDate = null,
                    role = UserRole.WOMAN,
                    appLoginActive = true,
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].password).isEqualTo("1001260000:encoded")
        }

        @Test
        @DisplayName("U5 appLoginActive=false — User.isActive=false 동기")
        fun handleEvent_appLoginActiveFalse_userInactive() {
            service.handleEmployeeCreated(
                EmployeeCreatedEvent(
                    employeeCode = "100127",
                    name = "최민수",
                    workEmail = "choi@otoki.com",
                    email = null,
                    birthDate = "19880508",
                    role = UserRole.WOMAN,
                    appLoginActive = false,
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].isActive).isFalse()
        }

        @Test
        @DisplayName("U6 같은 username 의 User 가 이미 존재 — 멱등 skip")
        fun handleEvent_existingUsername_skipsCreation() {
            every { userRepository.findByUsername("dup@otoki.com") } returns User(
                username = "dup@otoki.com",
                employeeCode = "100128",
                password = "x"
            )

            service.handleEmployeeCreated(
                EmployeeCreatedEvent(
                    employeeCode = "100128",
                    name = "정한별",
                    workEmail = "dup@otoki.com",
                    email = null,
                    birthDate = "19900101",
                    role = UserRole.WOMAN,
                    appLoginActive = true,
                )
            )

            verify(exactly = 0) { userRepository.save(any<User>()) }
        }

        @Test
        @DisplayName("U7 UserProvisioningService 내부 예외 — 예외가 호출자에게 전파되지 않음 (SF @future 동등)")
        fun handleEvent_internalException_swallowed() {
            every { userRepository.save(any<User>()) } throws RuntimeException("DB constraint")

            service.handleEmployeeCreated(
                EmployeeCreatedEvent(
                    employeeCode = "100129",
                    name = "에러테스트",
                    workEmail = "err@otoki.com",
                    email = null,
                    birthDate = null,
                    role = UserRole.WOMAN,
                    appLoginActive = true,
                )
            )

            // 예외가 던져지지 않으면 통과 — Employee 측 트랜잭션 격리 보장
        }
    }

    @Nested
    @DisplayName("provisionForSeed - 시드 동기 호출")
    inner class SeedPath {

        @Test
        @DisplayName("S1 시드 — encodedPassword override + passwordChangeRequired=false 반영")
        fun provisionForSeed_overridesPasswordAndFlag() {
            service.provisionForSeed(
                employeeCode = "99990001",
                name = "개발테스트",
                workEmail = "dev@otoki.local",
                email = null,
                birthDate = "19850315",
                role = UserRole.SALES_SUPPORT,
                appLoginActive = true,
                encodedPassword = "pre_encoded",
                passwordChangeRequired = false,
            )

            assertThat(savedUsers).hasSize(1)
            val saved = savedUsers[0]
            assertThat(saved.password).isEqualTo("pre_encoded")
            assertThat(saved.passwordChangeRequired).isFalse()
            assertThat(saved.profileType).isEqualTo(ProfileType.STAFF)
            assertThat(saved.isSalesSupport).isTrue()
        }
    }

    @Nested
    @DisplayName("ProfileType 매핑")
    inner class ProfileTypeMapping {

        @Test
        @DisplayName("P1 UserRole → ProfileType 9개 매핑 검증")
        fun profileTypeMapping_allRoles() {
            val cases = mapOf(
                UserRole.SYSTEM_ADMIN to ProfileType.SYSTEM_ADMIN,
                UserRole.LEADER to ProfileType.TEAM_LEADER,
                UserRole.BRANCH_MANAGER to ProfileType.BRANCH_MANAGER,
                UserRole.SALES_MANAGER to ProfileType.SALES_MANAGER,
                UserRole.BUSINESS_MANAGER to ProfileType.BUSINESS_DIRECTOR,
                UserRole.HEADQUARTERS_MANAGER to ProfileType.DIVISION_HEAD,
                UserRole.SALES_SUPPORT to ProfileType.STAFF,
                UserRole.WOMAN to ProfileType.SALES_REP,
                UserRole.UNKNOWN to ProfileType.SALES_REP,
            )

            cases.forEach { (role, _) ->
                service.provisionForSeed(
                    employeeCode = "P_$role",
                    name = "테스트",
                    workEmail = "$role@otoki.local",
                    email = null,
                    birthDate = null,
                    role = role,
                    appLoginActive = true,
                )
            }

            verify(exactly = cases.size) { userRepository.save(any<User>()) }
            val byProfile = savedUsers.associate { it.employeeCode to it.profileType }
            cases.forEach { (role, expected) ->
                assertThat(byProfile["P_$role"]).isEqualTo(expected)
            }
        }
    }
}
