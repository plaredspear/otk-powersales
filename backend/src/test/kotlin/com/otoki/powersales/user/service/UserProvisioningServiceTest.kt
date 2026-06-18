package com.otoki.powersales.user.service

import com.otoki.powersales.platform.auth.entity.Profile
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.event.EmployeeSnapshot
import com.otoki.powersales.user.event.EmployeesCreatedEvent
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
    private val profileRepository: ProfileRepository = mockk()

    private val service = UserProvisioningService(
        userRepository,
        passwordEncoder,
        profileRepository,
    )

    private val savedUsers = mutableListOf<User>()

    @BeforeEach
    fun setUp() {
        savedUsers.clear()
        every { passwordEncoder.encode(any<CharSequence>()) } answers { firstArg<CharSequence>().toString() + ":encoded" }
        every { userRepository.findByUsername(any<String>()) } returns null
        // bulk 경로 멱등 가드 — 기존 User 사번 집합 단일 조회 (기본: 없음)
        every { userRepository.findByEmployeeCodeIn(any<Collection<String>>()) } returns emptyList()
        every { userRepository.save(any<User>()) } answers {
            val arg = firstArg<User>()
            savedUsers.add(arg)
            arg
        }
        every { userRepository.saveAll(any<List<User>>()) } answers {
            val arg = firstArg<List<User>>()
            savedUsers.addAll(arg)
            arg
        }
        // 12종 Profile name → id stub
        every { profileRepository.findByName(any()) } answers {
            val name = firstArg<String>()
            Profile(id = PROFILE_NAME_TO_ID[name] ?: 0L, name = name)
        }
    }

    /** 단건 스냅샷을 bulk 이벤트 1건으로 감싸 호출하는 헬퍼 (테스트 가독성). */
    private fun handleOne(snapshot: EmployeeSnapshot) {
        service.handleEmployeesCreated(EmployeesCreatedEvent(listOf(snapshot)))
    }

    @Nested
    @DisplayName("handleEmployeesCreated - EmployeesCreatedEvent 수신 시 User 일괄 INSERT")
    inner class EventHandling {

        @Test
        @DisplayName("U1 정상 — workEmail 기준 User 생성 (employee_code / username / email / password 매칭)")
        fun handleEvent_createsUser() {
            handleOne(
                EmployeeSnapshot(
                    employeeCode = "100123",
                    name = "홍길동",
                    workEmail = "hong@otokims.co.kr",
                    email = null,
                    birthDate = "19900315",
                    role = AppAuthority.WOMAN,
                    appLoginActive = true,
                )
            )

            verify { userRepository.saveAll(any<List<User>>()) }
            assertThat(savedUsers).hasSize(1)
            val saved = savedUsers[0]
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.username).isEqualTo("hong@otokims.co.kr")
            assertThat(saved.email).isEqualTo("hong@otokims.co.kr")
            assertThat(saved.name).isEqualTo("홍길동")
            assertThat(saved.passwordChangeRequired).isTrue()
            assertThat(saved.password).isEqualTo("1001230315:encoded")
            assertThat(saved.profileId).isEqualTo(PROFILE_NAME_TO_ID["5.영업사원"])
            assertThat(saved.isSalesSupport).isFalse()
        }

        @Test
        @DisplayName("U2 workEmail null + email 있음 — email fallback 으로 생성")
        fun handleEvent_workEmailNull_fallsBackToEmail() {
            handleOne(
                EmployeeSnapshot(
                    employeeCode = "100124",
                    name = "김철수",
                    workEmail = null,
                    email = "kim@personal.com",
                    birthDate = "19850101",
                    role = AppAuthority.WOMAN,
                    appLoginActive = true,
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].username).isEqualTo("kim@personal.com")
        }

        @Test
        @DisplayName("U3 workEmail / email 둘 다 부재 — User 생성 skip (SF cls:281 동등)")
        fun handleEvent_noEmail_skipsCreation() {
            handleOne(
                EmployeeSnapshot(
                    employeeCode = "100125",
                    name = "이영희",
                    workEmail = null,
                    email = null,
                    birthDate = "19900101",
                    role = AppAuthority.WOMAN,
                    appLoginActive = true,
                )
            )

            // 통과 행이 없으면 saveAll 호출 안 함
            verify(exactly = 0) { userRepository.saveAll(any<List<User>>()) }
            assertThat(savedUsers).isEmpty()
        }

        @Test
        @DisplayName("U4 birthDate 부재 — 임시 비밀번호 = 사번 + '0000'")
        fun handleEvent_noBirthDate_passwordFallback() {
            handleOne(
                EmployeeSnapshot(
                    employeeCode = "100126",
                    name = "박지민",
                    workEmail = "park@otoki.com",
                    email = null,
                    birthDate = null,
                    role = AppAuthority.WOMAN,
                    appLoginActive = true,
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].password).isEqualTo("1001260000:encoded")
        }

        @Test
        @DisplayName("U5 appLoginActive=false — User.isActive=false 동기")
        fun handleEvent_appLoginActiveFalse_userInactive() {
            handleOne(
                EmployeeSnapshot(
                    employeeCode = "100127",
                    name = "최민수",
                    workEmail = "choi@otoki.com",
                    email = null,
                    birthDate = "19880508",
                    role = AppAuthority.WOMAN,
                    appLoginActive = false,
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].isActive).isFalse()
        }

        @Test
        @DisplayName("U6 이미 존재하는 username — 멱등 skip (findByEmployeeCodeIn bulk 조회)")
        fun handleEvent_existingUsername_skipsCreation() {
            every { userRepository.findByEmployeeCodeIn(listOf("100128")) } returns listOf(
                User(username = "dup@otoki.com", employeeCode = "100128", password = "x")
            )

            handleOne(
                EmployeeSnapshot(
                    employeeCode = "100128",
                    name = "정한별",
                    workEmail = "dup@otoki.com",
                    email = null,
                    birthDate = "19900101",
                    role = AppAuthority.WOMAN,
                    appLoginActive = true,
                )
            )

            assertThat(savedUsers).isEmpty()
        }

        @Test
        @DisplayName("U7 내부 예외 — 호출자에게 전파되지 않음 (SF @future 동등)")
        fun handleEvent_internalException_swallowed() {
            every { userRepository.saveAll(any<List<User>>()) } throws RuntimeException("DB constraint")

            service.handleEmployeesCreated(
                EmployeesCreatedEvent(
                    listOf(
                        EmployeeSnapshot(
                            employeeCode = "100129",
                            name = "에러테스트",
                            workEmail = "err@otoki.com",
                            email = null,
                            birthDate = null,
                            role = AppAuthority.WOMAN,
                            appLoginActive = true,
                        )
                    )
                )
            )

            // 예외가 던져지지 않으면 통과 — Employee 측 트랜잭션 격리 보장
        }

        @Test
        @DisplayName("U8 여러 명 일괄 — 단일 saveAll 로 일괄 INSERT (레거시 bulk 동등)")
        fun handleEvent_multipleEmployees_singleBulkInsert() {
            service.handleEmployeesCreated(
                EmployeesCreatedEvent(
                    listOf(
                        snapshot("200001", "a@otoki.com"),
                        snapshot("200002", "b@otoki.com"),
                        snapshot("200003", "c@otoki.com"),
                    )
                )
            )

            // 행마다 save 가 아니라 saveAll 1회로 처리
            verify(exactly = 1) { userRepository.saveAll(any<List<User>>()) }
            verify(exactly = 0) { userRepository.save(any<User>()) }
            assertThat(savedUsers).hasSize(3)
            assertThat(savedUsers.map { it.employeeCode }).containsExactly("200001", "200002", "200003")
        }

        @Test
        @DisplayName("U9 배치 내 중복 username — 첫 행만 채택 (배치 내부 멱등)")
        fun handleEvent_duplicateWithinBatch_keepsFirst() {
            service.handleEmployeesCreated(
                EmployeesCreatedEvent(
                    listOf(
                        snapshot("300001", "same@otoki.com"),
                        snapshot("300002", "same@otoki.com"),
                    )
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].employeeCode).isEqualTo("300001")
        }

        @Test
        @DisplayName("U10 기존/신규 혼합 — 기존 username 제외하고 신규만 적재")
        fun handleEvent_mixedExistingAndNew_savesOnlyNew() {
            every { userRepository.findByEmployeeCodeIn(listOf("400001", "400002")) } returns listOf(
                User(username = "old@otoki.com", employeeCode = "400001", password = "x")
            )

            service.handleEmployeesCreated(
                EmployeesCreatedEvent(
                    listOf(
                        snapshot("400001", "old@otoki.com"),
                        snapshot("400002", "fresh@otoki.com"),
                    )
                )
            )

            assertThat(savedUsers).hasSize(1)
            assertThat(savedUsers[0].employeeCode).isEqualTo("400002")
        }

        private fun snapshot(code: String, email: String) = EmployeeSnapshot(
            employeeCode = code,
            name = "사원$code",
            workEmail = email,
            email = null,
            birthDate = null,
            role = AppAuthority.WOMAN,
            appLoginActive = true,
        )
    }

    @Nested
    @DisplayName("provisionForSeed - 시드 동기 호출")
    inner class SeedPath {

        @Test
        @DisplayName("S1 시드 — encodedPassword override + passwordChangeRequired=false + isSalesSupport=true 반영")
        fun provisionForSeed_overridesPasswordAndFlag() {
            service.provisionForSeed(
                employeeCode = "99990001",
                name = "개발테스트",
                workEmail = "dev@otoki.local",
                email = null,
                birthDate = "19850315",
                role = null,
                appLoginActive = true,
                isSalesSupport = true,
                encodedPassword = "pre_encoded",
                passwordChangeRequired = false,
            )

            assertThat(savedUsers).hasSize(1)
            val saved = savedUsers[0]
            assertThat(saved.password).isEqualTo("pre_encoded")
            assertThat(saved.passwordChangeRequired).isFalse()
            // role=null → 5.영업사원 fallback (spec #807). isSalesSupport 는 명시 인자.
            assertThat(saved.profileId).isEqualTo(PROFILE_NAME_TO_ID["5.영업사원"])
            assertThat(saved.isSalesSupport).isTrue()
        }
    }

    @Nested
    @DisplayName("SF AppAuthority picklist → Profile.id 매핑")
    inner class ProfileIdMapping {

        @Test
        @DisplayName("P1 SF AppAuthority picklist 4종 + null → Profile.id 매핑")
        fun profileIdMapping_allPicklist() {
            val cases = listOf(
                "code_leader" to AppAuthority.LEADER to "6.조장",
                "code_branch" to AppAuthority.BRANCH_MANAGER to "4.지점장",
                "code_woman" to AppAuthority.WOMAN to "5.영업사원",
                "code_acc" to AppAuthority.ACCOUNT_VIEW_ALL to "5.영업사원",
                "code_null" to null to "5.영업사원",
            )

            cases.forEach { (codeAndRole, _) ->
                val (code, role) = codeAndRole
                service.provisionForSeed(
                    employeeCode = code,
                    name = "테스트",
                    workEmail = "$code@otoki.local",
                    email = null,
                    birthDate = null,
                    role = role,
                    appLoginActive = true,
                )
            }

            verify(exactly = cases.size) { userRepository.save(any<User>()) }
            val byProfile = savedUsers.associate { it.employeeCode to it.profileId }
            cases.forEach { (codeAndRole, expectedName) ->
                val (code, _) = codeAndRole
                assertThat(byProfile[code]).isEqualTo(PROFILE_NAME_TO_ID[expectedName])
            }
        }

        @Test
        @DisplayName("[Sanity] SystemAdminProfilePolicy 매핑 정합 — AppAuthority picklist → Profile.name")
        fun policyMapping() {
            assertThat(SystemAdminProfilePolicy.profileNameForRole(null))
                .isEqualTo("5.영업사원")
            assertThat(SystemAdminProfilePolicy.profileNameForRole(AppAuthority.WOMAN))
                .isEqualTo("5.영업사원")
            assertThat(SystemAdminProfilePolicy.profileNameForRole(AppAuthority.LEADER))
                .isEqualTo("6.조장")
            assertThat(SystemAdminProfilePolicy.profileNameForRole(AppAuthority.BRANCH_MANAGER))
                .isEqualTo("4.지점장")
        }
    }

    companion object {
        private val PROFILE_NAME_TO_ID: Map<String, Long> = mapOf(
            "시스템 관리자" to 1L,
            "8. 마케팅" to 2L,
            "9. Staff" to 3L,
            "6.조장" to 4L,
            "4.지점장" to 5L,
            "3.영업부장" to 6L,
            "2.사업부장" to 7L,
            "1.본부장" to 8L,
            "5.영업사원" to 9L,
            "7.영업사원 + 조장" to 10L,
            "공장관계자" to 11L,
            "OLS" to 12L,
        )
    }
}
