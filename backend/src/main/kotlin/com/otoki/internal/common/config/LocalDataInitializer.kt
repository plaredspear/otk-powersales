package com.otoki.internal.common.config

import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.common.repository.AgreementWordRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.sap.entity.Organization
import com.otoki.internal.sap.repository.OrganizationRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@Component
@Profile("local")
class LocalDataInitializer(
    private val employeeRepository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val agreementWordRepository: AgreementWordRepository,
    private val organizationRepository: OrganizationRepository,
    private val transactionTemplate: TransactionTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        runSafely("seedUser") { seedUser() }
        runSafely("seedAgreementWord") { seedAgreementWord() }
        runSafely("seedOrg") { seedOrg() }
    }

    private fun runSafely(name: String, block: () -> Unit) {
        try {
            transactionTemplate.executeWithoutResult { block() }
        } catch (e: Exception) {
            log.warn("시드 데이터 '{}' 실행 실패 (기존 데이터 충돌 가능): {}", name, e.message)
        }
    }

    private fun seedUser() {
        val encodedPassword = passwordEncoder.encode("1234")

        // 영업지원실 (ADMIN - 웹 관리자 전체 조회용)
        if (!employeeRepository.existsByEmployeeCode("00000001")) {
            employeeRepository.save(
                Employee(
                    employeeCode = "00000001",
                    name = "개발테스트",
                    status = "재직",
                    appLoginActive = true,
                    orgName = "테스트지점",
                    appAuthority = "영업지원실",
                    birthDate = "19850315",
                    homePhone = "02-1234-5678",
                    workPhone = "02-9876-5432",
                    startDate = LocalDate.of(2015, 3, 1),
                    costCenterCode = "1111",
                    password = encodedPassword,
                    passwordChangeRequired = false
                )
            )
            log.info("시드 계정 생성 완료: employeeCode=00000001, name=개발테스트, role=LEADER")
        }

        // 여사원 (USER)
        if (!employeeRepository.existsByEmployeeCode("00000002")) {
            employeeRepository.save(
                Employee(
                    employeeCode = "00000002",
                    name = "여사원테스트",
                    status = "재직",
                    appLoginActive = true,
                    orgName = "테스트지점",
                    appAuthority = "여사원",
                    birthDate = "19920820",
                    homePhone = "02-2345-6789",
                    workPhone = "02-8765-4321",
                    startDate = LocalDate.of(2018, 7, 1),
                    costCenterCode = "1111",
                    password = encodedPassword,
                    passwordChangeRequired = false
                )
            )
            log.info("시드 계정 생성 완료: employeeCode=00000002, name=여사원테스트, role=USER")
        }

        // 지점장 (ADMIN)
        if (!employeeRepository.existsByEmployeeCode("00000003")) {
            employeeRepository.save(
                Employee(
                    employeeCode = "00000003",
                    name = "지점장테스트",
                    status = "재직",
                    appLoginActive = true,
                    orgName = "테스트지점",
                    appAuthority = "지점장",
                    birthDate = "19780105",
                    homePhone = "02-3456-7890",
                    workPhone = "02-7654-3210",
                    startDate = LocalDate.of(2010, 1, 15),
                    costCenterCode = "1111",
                    password = encodedPassword,
                    passwordChangeRequired = false
                )
            )
            log.info("시드 계정 생성 완료: employeeCode=00000003, name=지점장테스트, role=ADMIN")
        }

        // 강남지점 조장
        if (!employeeRepository.existsByEmployeeCode("00000004")) {
            employeeRepository.save(
                Employee(
                    employeeCode = "00000004",
                    name = "강남조장",
                    status = "재직",
                    appLoginActive = true,
                    orgName = "강남지점",
                    appAuthority = "조장",
                    birthDate = "19880510",
                    homePhone = "02-4567-8901",
                    workPhone = "02-6543-2109",
                    startDate = LocalDate.of(2016, 5, 1),
                    costCenterCode = "1112",
                    password = encodedPassword,
                    passwordChangeRequired = false
                )
            )
            log.info("시드 계정 생성 완료: employeeCode=00000004, name=강남조장, role=LEADER")
        }

        // 강남지점 여사원
        if (!employeeRepository.existsByEmployeeCode("00000005")) {
            employeeRepository.save(
                Employee(
                    employeeCode = "00000005",
                    name = "강남여사원",
                    status = "재직",
                    appLoginActive = true,
                    orgName = "강남지점",
                    appAuthority = "여사원",
                    birthDate = "19950320",
                    homePhone = "02-5678-9012",
                    workPhone = "02-5432-1098",
                    startDate = LocalDate.of(2020, 3, 1),
                    costCenterCode = "1112",
                    password = encodedPassword,
                    passwordChangeRequired = false
                )
            )
            log.info("시드 계정 생성 완료: employeeCode=00000005, name=강남여사원, role=USER")
        }
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
            activeDate = LocalDate.now()
        )

        agreementWordRepository.save(agreementWord)
        log.info("GPS 동의 약관 시드 데이터 생성 완료: name={}", agreementWord.name)
    }

    private fun seedOrg() {
        if (organizationRepository.count() > 0) {
            log.info("조직마스터가 이미 존재합니다 — skip")
            return
        }

        val orgs = listOf(
            Organization(
                costCenterLevel2 = "1000", orgCodeLevel2 = "O100", orgNameLevel2 = "오뚜기",
                costCenterLevel3 = "1100", orgCodeLevel3 = "O110", orgNameLevel3 = "영업본부",
                costCenterLevel4 = "1110", orgCodeLevel4 = "O111", orgNameLevel4 = "수도권영업부",
                costCenterLevel5 = "1111", orgCodeLevel5 = "O1111", orgNameLevel5 = "테스트지점"
            ),
            Organization(
                costCenterLevel2 = "1000", orgCodeLevel2 = "O100", orgNameLevel2 = "오뚜기",
                costCenterLevel3 = "1100", orgCodeLevel3 = "O110", orgNameLevel3 = "영업본부",
                costCenterLevel4 = "1110", orgCodeLevel4 = "O111", orgNameLevel4 = "수도권영업부",
                costCenterLevel5 = "1112", orgCodeLevel5 = "O1112", orgNameLevel5 = "강남지점"
            ),
            Organization(
                costCenterLevel2 = "1000", orgCodeLevel2 = "O100", orgNameLevel2 = "오뚜기",
                costCenterLevel3 = "1100", orgCodeLevel3 = "O110", orgNameLevel3 = "영업본부",
                costCenterLevel4 = "1120", orgCodeLevel4 = "O112", orgNameLevel4 = "중부영업부",
                costCenterLevel5 = "1121", orgCodeLevel5 = "O1121", orgNameLevel5 = "대전지점"
            )
        )

        organizationRepository.saveAll(orgs)
        log.info("조직마스터 시드 데이터 생성 완료: {}건", orgs.size)
    }
}
