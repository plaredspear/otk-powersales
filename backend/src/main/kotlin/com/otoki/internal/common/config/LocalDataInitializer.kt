package com.otoki.internal.common.config

import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.sap.entity.User
import com.otoki.internal.common.repository.AgreementWordRepository
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.NoticeCategory
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.sap.entity.Organization
import com.otoki.internal.sap.repository.OrganizationRepository
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
    private val agreementWordRepository: AgreementWordRepository,
    private val noticeRepository: NoticeRepository,
    private val organizationRepository: OrganizationRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        seedUser()
        seedAgreementWord()
        seedNotices()
        seedOrg()
    }

    private fun seedUser() {
        val encodedPassword = passwordEncoder.encode("1234")

        // 영업지원실 (ADMIN - 웹 관리자 전체 조회용)
        if (!userRepository.existsByEmployeeId("00000001")) {
            userRepository.save(
                User(
                    employeeId = "00000001",
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
            log.info("시드 계정 생성 완료: employeeId=00000001, name=개발테스트, role=LEADER")
        }

        // 여사원 (USER)
        if (!userRepository.existsByEmployeeId("00000002")) {
            userRepository.save(
                User(
                    employeeId = "00000002",
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
            log.info("시드 계정 생성 완료: employeeId=00000002, name=여사원테스트, role=USER")
        }

        // 지점장 (ADMIN)
        if (!userRepository.existsByEmployeeId("00000003")) {
            userRepository.save(
                User(
                    employeeId = "00000003",
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
            log.info("시드 계정 생성 완료: employeeId=00000003, name=지점장테스트, role=ADMIN")
        }

        // 강남지점 조장
        if (!userRepository.existsByEmployeeId("00000004")) {
            userRepository.save(
                User(
                    employeeId = "00000004",
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
            log.info("시드 계정 생성 완료: employeeId=00000004, name=강남조장, role=LEADER")
        }

        // 강남지점 여사원
        if (!userRepository.existsByEmployeeId("00000005")) {
            userRepository.save(
                User(
                    employeeId = "00000005",
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
            log.info("시드 계정 생성 완료: employeeId=00000005, name=강남여사원, role=USER")
        }
    }

    private fun seedNotices() {
        if (noticeRepository.count() > 0) {
            log.info("공지사항이 이미 존재합니다 — skip")
            return
        }

        val now = LocalDateTime.now()
        val notices = listOf(
            Notice(
                name = "NTC-LOCAL-001",
                category = NoticeCategory.COMPANY,
                scope = "전체",
                contents = """
                    |[LOCAL 개발용] 2026년 상반기 영업 목표 안내
                    |
                    |안녕하세요. 영업기획팀입니다.
                    |2026년 상반기 영업 목표가 확정되어 안내드립니다.
                    |
                    |1. 목표 확인: 파워세일즈 앱 > 매출현황 > 목표 탭
                    |2. 적용 기간: 2026.01.01 ~ 2026.06.30
                    |3. 문의: 영업기획팀 (내선 1234)
                """.trimMargin(),
                isDeleted = false,
                createdDate = now.minusDays(5)
            ),
            Notice(
                name = "NTC-LOCAL-002",
                category = NoticeCategory.COMPANY,
                scope = "전체",
                contents = """
                    |[LOCAL 개발용] 모바일 앱 업데이트 안내
                    |
                    |파워세일즈 앱 v2.0이 출시되었습니다.
                    |
                    |주요 변경사항:
                    |- GPS 출근 등록 기능 추가
                    |- 홈 화면 UI 개선
                    |- 매출 차트 성능 향상
                    |
                    |앱스토어에서 업데이트해주세요.
                """.trimMargin(),
                isDeleted = false,
                createdDate = now.minusDays(4)
            ),
            Notice(
                name = "NTC-LOCAL-003",
                category = NoticeCategory.EDUCATION,
                scope = "전체",
                contents = """
                    |[LOCAL 개발용] 하계 안전 교육 일정 안내
                    |
                    |2026년 하계 안전 교육 일정을 안내드립니다.
                    |
                    |- 일시: 2026.07.01 (화) 14:00~16:00
                    |- 장소: 본사 대강당 (온라인 병행)
                    |- 대상: 전 영업사원
                    |- 내용: 폭염 대비 안전 수칙, 차량 관리
                """.trimMargin(),
                eduCategory = "교육",
                isDeleted = false,
                createdDate = now.minusDays(3)
            ),
            Notice(
                name = "NTC-LOCAL-004",
                category = NoticeCategory.BRANCH,
                scope = "지점",
                contents = """
                    |[LOCAL 개발용] 테스트지점 6월 회의 안내
                    |
                    |테스트지점 월례 회의 일정입니다.
                    |
                    |- 일시: 2026.06.15 (월) 09:00
                    |- 장소: 테스트지점 회의실
                    |- 안건: 상반기 실적 중간 점검, 하반기 전략 논의
                """.trimMargin(),
                branch = "테스트지점",
                branchCode = "BR-TEST-001",
                isDeleted = false,
                createdDate = now.minusDays(2)
            ),
            Notice(
                name = "NTC-LOCAL-005",
                category = NoticeCategory.BRANCH,
                scope = "지점",
                contents = """
                    |[LOCAL 개발용] 테스트지점 신제품 입고 안내
                    |
                    |신제품 입고 예정 안내입니다.
                    |
                    |- 제품: 오뚜기 진라면 매운맛 리뉴얼
                    |- 입고일: 2026.06.20
                    |- 비고: 거래처 샘플 배포 요청 시 영업지원팀 연락
                """.trimMargin(),
                branch = "테스트지점",
                branchCode = "BR-TEST-001",
                isDeleted = false,
                createdDate = now.minusDays(1)
            )
        )

        noticeRepository.saveAll(notices)
        log.info("공지사항 시드 데이터 생성 완료: {}건", notices.size)
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
