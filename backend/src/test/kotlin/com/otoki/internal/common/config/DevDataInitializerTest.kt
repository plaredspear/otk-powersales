package com.otoki.internal.common.config

import com.otoki.internal.common.entity.User
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

@ExtendWith(MockitoExtension::class)
@DisplayName("DevDataInitializer 테스트")
class DevDataInitializerTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var devDataInitializer: DevDataInitializer

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

            // When
            devDataInitializer.run(null)

            // Then
            verify(userRepository).save(any<User>())
        }

        @Test
        @DisplayName("멱등성 - DB에 시드 계정 이미 존재 -> 저장 skip")
        fun run_skips_whenAlreadyExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000009")).thenReturn(true)

            // When
            devDataInitializer.run(null)

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

            // When
            devDataInitializer.run(null)

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
}
