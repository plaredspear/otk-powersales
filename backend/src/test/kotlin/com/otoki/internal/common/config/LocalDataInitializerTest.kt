package com.otoki.internal.common.config

import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.common.entity.User
import com.otoki.internal.common.repository.AgreementWordRepository
import com.otoki.internal.common.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("LocalDataInitializer 테스트")
class LocalDataInitializerTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var agreementWordRepository: AgreementWordRepository

    @InjectMocks
    private lateinit var localDataInitializer: LocalDataInitializer

    @Nested
    @DisplayName("run - 시드 계정 생성")
    inner class RunTests {

        @Test
        @DisplayName("정상 생성 - DB에 시드 계정 없음 -> User 생성 및 저장")
        fun run_createsUser_whenNotExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.of(AgreementWord()))

            // When
            localDataInitializer.run(null)

            // Then
            verify(userRepository).save(any<User>())
        }

        @Test
        @DisplayName("멱등성 - DB에 시드 계정 이미 존재 -> 저장 skip")
        fun run_skips_whenAlreadyExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(true)
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.of(AgreementWord()))

            // When
            localDataInitializer.run(null)

            // Then
            verify(userRepository, never()).save(any<User>())
        }

        @Test
        @DisplayName("정상 생성 - 생성된 User의 employeeId와 name 확인")
        fun run_createsUserWithCorrectData() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.of(AgreementWord()))

            // When
            localDataInitializer.run(null)

            // Then
            verify(userRepository).save(org.mockito.kotlin.check<User> { user ->
                assertThat(user.employeeId).isEqualTo("00000009")
                assertThat(user.name).isEqualTo("개발테스트")
                assertThat(user.status).isEqualTo("재직")
                assertThat(user.appLoginActive).isTrue()
                assertThat(user.orgName).isEqualTo("테스트지점")
                assertThat(user.password).isEqualTo("encoded_password")
                assertThat(user.passwordChangeRequired).isFalse()
            })
        }
    }

    @Nested
    @DisplayName("run - GPS 동의 약관 시드 생성")
    inner class AgreementWordSeedTests {

        @Test
        @DisplayName("정상 생성 - 활성 약관 없음 -> AgreementWord 생성 및 저장")
        fun run_createsAgreementWord_whenNotExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(true)
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.empty())
            whenever(agreementWordRepository.save(any<AgreementWord>()))
                .thenAnswer { it.getArgument<AgreementWord>(0) }

            // When
            localDataInitializer.run(null)

            // Then
            verify(agreementWordRepository).save(any<AgreementWord>())
        }

        @Test
        @DisplayName("멱등성 - 활성 약관 이미 존재 -> 저장 skip")
        fun run_skipsAgreementWord_whenAlreadyExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(true)
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.of(AgreementWord()))

            // When
            localDataInitializer.run(null)

            // Then
            verify(agreementWordRepository, never()).save(any<AgreementWord>())
        }

        @Test
        @DisplayName("정상 생성 - 생성된 AgreementWord의 필드 확인")
        fun run_createsAgreementWordWithCorrectData() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(true)
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.empty())
            whenever(agreementWordRepository.save(any<AgreementWord>()))
                .thenAnswer { it.getArgument<AgreementWord>(0) }

            // When
            localDataInitializer.run(null)

            // Then
            verify(agreementWordRepository).save(org.mockito.kotlin.check<AgreementWord> { aw ->
                assertThat(aw.name).isEqualTo("AGR-LOCAL-001")
                assertThat(aw.contents).contains("[LOCAL 개발용]")
                assertThat(aw.contents).contains("위치정보 수집·이용 동의서")
                assertThat(aw.active).isTrue()
                assertThat(aw.isDeleted).isFalse()
                assertThat(aw.activeDate).isNotNull()
                assertThat(aw.createdDate).isNotNull()
            })
        }

        @Test
        @DisplayName("시드 계정 + 약관 동시 생성 - 두 테이블 모두 비어 있음 -> 모두 생성")
        fun run_createsBoth_whenNeitherExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.empty())
            whenever(agreementWordRepository.save(any<AgreementWord>()))
                .thenAnswer { it.getArgument<AgreementWord>(0) }

            // When
            localDataInitializer.run(null)

            // Then
            verify(userRepository).save(any<User>())
            verify(agreementWordRepository).save(any<AgreementWord>())
        }
    }
}
