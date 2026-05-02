package com.otoki.powersales.common.config

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.common.repository.AgreementWordRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.promotion.entity.PromotionType
import com.otoki.powersales.promotion.repository.PromotionTypeRepository
import jakarta.persistence.EntityManager
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
    private val accountRepository: AccountRepository,
    private val organizationRepository: OrganizationRepository,
    private val promotionTypeRepository: PromotionTypeRepository,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        runSafely("seedUser") { seedUser() }
        runSafely("seedAgreementWord") { seedAgreementWord() }
        runSafely("seedOrg") { seedOrg() }
        runSafely("seedAccount") { seedAccount() }
        runSafely("seedPromotionType") { seedPromotionType() }
    }

    private fun runSafely(name: String, block: () -> Unit) {
        try {
            transactionTemplate.executeWithoutResult { block() }
        } catch (e: Exception) {
            log.warn("시드 데이터 '{}' 실행 실패 (기존 데이터 충돌 가능): {}", name, e.message)
        }
    }

    private fun employeeInfoExists(employeeCode: String): Boolean {
        val count = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM powersales.employee_info WHERE employee_code = :code"
        ).setParameter("code", employeeCode).singleResult as Number
        return count.toLong() > 0
    }

    private fun seedUser() {
        val encodedPassword = passwordEncoder.encode("1234")!!

        data class SeedEmployee(
            val code: String, val name: String, val role: UserRole,
            val orgName: String, val costCenterCode: String, val birthDate: String,
            val homePhone: String, val workPhone: String, val startDate: LocalDate
        )

        val seeds = listOf(
            SeedEmployee("99990001", "개발테스트", UserRole.SALES_SUPPORT, "테스트지점", "1111", "19850315", "02-1234-5678", "02-9876-5432", LocalDate.of(2015, 3, 1)),
            SeedEmployee("99990002", "여사원테스트", UserRole.WOMAN, "테스트지점", "1111", "19920820", "02-2345-6789", "02-8765-4321", LocalDate.of(2018, 7, 1)),
            SeedEmployee("99990003", "지점장테스트", UserRole.BRANCH_MANAGER, "테스트지점", "1111", "19780105", "02-3456-7890", "02-7654-3210", LocalDate.of(2010, 1, 15)),
            SeedEmployee("99990004", "강남조장", UserRole.LEADER, "테스트지점", "1111", "19880510", "02-4567-8901", "02-6543-2109", LocalDate.of(2016, 5, 1)),
            SeedEmployee("99990005", "강남여사원", UserRole.WOMAN, "강남지점", "1112", "19950320", "02-5678-9012", "02-5432-1098", LocalDate.of(2020, 3, 1))
        )

        for (seed in seeds) {
            if (employeeRepository.existsByEmployeeCode(seed.code)) continue

            val infoExists = employeeInfoExists(seed.code)
            val employee = Employee(
                employeeCode = seed.code, name = seed.name, status = "재직",
                appLoginActive = true, orgName = seed.orgName, role = seed.role,
                birthDate = seed.birthDate, homePhone = seed.homePhone, workPhone = seed.workPhone,
                startDate = seed.startDate, costCenterCode = seed.costCenterCode,
                password = encodedPassword, passwordChangeRequired = false
            )
            if (infoExists) {
                employee.employeeInfo = null
            }
            employeeRepository.save(employee)
            log.info("시드 계정 생성 완료: employeeCode={}, name={}", seed.code, seed.name)
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

    private fun seedAccount() {
        data class SeedAccount(
            val externalKey: String, val name: String, val branchCode: String,
            val branchName: String, val abcType: String, val address1: String,
            val phone: String, val accountGroup: String,
            val accountStatusName: String, val employeeCode: String
        )

        val seeds = listOf(
            SeedAccount("TEST-ACC-001", "테스트_이마트 테스트점", "1111", "테스트지점", "A", "서울 강남구 테헤란로 1", "02-1111-0001", "10", "정상", "99990004"),
            SeedAccount("TEST-ACC-002", "테스트_홈플러스 테스트점", "1111", "테스트지점", "A", "서울 강남구 테헤란로 2", "02-1111-0002", "10", "정상", "99990004"),
            SeedAccount("TEST-ACC-003", "테스트_롯데마트 테스트점", "1111", "테스트지점", "B", "서울 강남구 테헤란로 3", "02-1111-0003", "10", "정상", "99990004"),
            SeedAccount("TEST-ACC-004", "테스트_GS25 테스트점", "1111", "테스트지점", "B", "서울 강남구 테헤란로 4", "02-1111-0004", "20", "정상", "99990004"),
            SeedAccount("TEST-ACC-005", "테스트_CU 테스트점", "1111", "테스트지점", "C", "서울 강남구 테헤란로 5", "02-1111-0005", "20", "정상", "99990004"),
            SeedAccount("TEST-ACC-006", "테스트_이마트 강남점", "1112", "강남지점", "A", "서울 강남구 역삼로 1", "02-1112-0001", "10", "정상", "99990005"),
            SeedAccount("TEST-ACC-007", "테스트_홈플러스 강남점", "1112", "강남지점", "A", "서울 강남구 역삼로 2", "02-1112-0002", "10", "정상", "99990005"),
            SeedAccount("TEST-ACC-008", "테스트_롯데마트 강남점", "1112", "강남지점", "B", "서울 강남구 역삼로 3", "02-1112-0003", "10", "정상", "99990005")
        )

        for (seed in seeds) {
            if (accountRepository.findByExternalKey(seed.externalKey) != null) continue

            val account = Account(
                externalKey = seed.externalKey,
                name = seed.name,
                branchCode = seed.branchCode,
                branchName = seed.branchName,
                abcType = seed.abcType,
                address1 = seed.address1,
                phone = seed.phone,
                accountGroup = seed.accountGroup,
                accountStatusName = seed.accountStatusName,
                employeeCode = seed.employeeCode
            )
            accountRepository.save(account)
            log.info("시드 거래처 생성 완료: externalKey={}, name={}", seed.externalKey, seed.name)
        }
    }

    private fun seedPromotionType() {
        if (promotionTypeRepository.count() > 0) {
            log.info("행사유형이 이미 존재합니다 — skip")
            return
        }

        val types = listOf(
            PromotionType(name = "시식", displayOrder = 1)
        )

        promotionTypeRepository.saveAll(types)
        log.info("행사유형 시드 데이터 생성 완료: {}건", types.size)
    }
}
