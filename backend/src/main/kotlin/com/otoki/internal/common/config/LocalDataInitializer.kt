package com.otoki.internal.common.config

import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.common.entity.User
import com.otoki.internal.common.repository.AgreementWordRepository
import com.otoki.internal.common.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Profile("local")
class LocalDataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val agreementWordRepository: AgreementWordRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        seedUser()
        seedAgreementWord()
    }

    private fun seedUser() {
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

    private fun seedAgreementWord() {
        if (agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse().isPresent) {
            log.info("GPS 동의 약관이 이미 존재합니다 — skip")
            return
        }

        val agreementWord = AgreementWord(
            name = "AGR-LOCAL-001",
            contents = """
                |[LOCAL 개발용] 위치정보 수집·이용 동의서
                |
                |주식회사 오뚜기(이하 "회사")는 영업사원의 효율적인 업무 수행을 위해 아래와 같이 위치정보를 수집·이용하고자 합니다.
                |
                |1. 수집하는 위치정보: GPS 기반 현재 위치 (위도, 경도)
                |2. 이용 목적: 영업 활동 기록, 근무 현황 관리
                |3. 보유 기간: 수집일로부터 1년
                |
                |위치정보 수집에 동의하십니까?
            """.trimMargin(),
            active = true,
            isDeleted = false,
            activeDate = LocalDate.now(),
            createdDate = LocalDateTime.now()
        )

        agreementWordRepository.save(agreementWord)
        log.info("GPS 동의 약관 시드 데이터 생성 완료: name={}", agreementWord.name)
    }
}
