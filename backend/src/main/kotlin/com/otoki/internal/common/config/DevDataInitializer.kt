package com.otoki.internal.common.config

import com.otoki.internal.common.entity.User
import com.otoki.internal.common.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("local")
class DevDataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        val employeeId = "00000009"

        if (userRepository.existsByEmployeeId(employeeId)) {
            log.info("시드 계정이 이미 존재합니다: employeeId={}", employeeId)
            return
        }

        val user = User(
            employeeId = employeeId,
            name = "개발테스트",
            status = "재직",
            appLoginActive = true,
            orgName = "테스트지점",
            password = passwordEncoder.encode("1234"),
            passwordChangeRequired = false
        )

        userRepository.save(user)
        log.info("시드 계정 생성 완료: employeeId={}, name={}", employeeId, user.name)
    }
}
