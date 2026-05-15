package com.otoki.powersales.user.service

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.event.EmployeeCreatedEvent
import com.otoki.powersales.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserProvisioningService 테스트")
class UserProvisioningServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var service: UserProvisioningService

    @BeforeEach
    fun setUp() {
        whenever(passwordEncoder.encode(any<CharSequence>())).thenAnswer { it.arguments[0].toString() + ":encoded" }
        whenever(userRepository.findByUsername(any<String>())).thenReturn(null)
    }

    @Nested
    @DisplayName("handleEmployeeCreated - EmployeeCreatedEvent 수신 시 User INSERT")
    inner class EventHandling {

        @Test
        @DisplayName("U1 정상 — workEmail 기준 User 생성 (employee_number / username / email / password 매칭)")
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

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.employeeNumber).isEqualTo("100123")
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

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.username).isEqualTo("kim@personal.com")
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

            verify(userRepository, never()).save(any<User>())
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

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.password).isEqualTo("1001260000:encoded")
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

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.isActive).isFalse()
        }

        @Test
        @DisplayName("U6 같은 username 의 User 가 이미 존재 — 멱등 skip")
        fun handleEvent_existingUsername_skipsCreation() {
            whenever(userRepository.findByUsername("dup@otoki.com")).thenReturn(
                User(
                    username = "dup@otoki.com",
                    employeeNumber = "100128",
                    password = "x"
                )
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

            verify(userRepository, never()).save(any<User>())
        }

        @Test
        @DisplayName("U7 UserProvisioningService 내부 예외 — 예외가 호출자에게 전파되지 않음 (SF @future 동등)")
        fun handleEvent_internalException_swallowed() {
            whenever(userRepository.save(any<User>())).thenThrow(RuntimeException("DB constraint"))

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

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            val saved = captor.firstValue
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

            cases.forEach { (role, expected) ->
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

            val captor = argumentCaptor<User>()
            verify(userRepository, org.mockito.kotlin.times(cases.size)).save(captor.capture())
            val byProfile = captor.allValues.associate { it.employeeNumber to it.profileType }
            cases.forEach { (role, expected) ->
                assertThat(byProfile["P_$role"]).isEqualTo(expected)
            }
        }
    }
}
