package com.otoki.powersales.domain.support.notice.repository

import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import com.otoki.powersales.domain.support.notice.enums.NoticeStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
class NoticeRepositoryTest {

    @Autowired
    private lateinit var noticeRepository: NoticeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        noticeRepository.deleteAll()
        testEntityManager.clear()
    }

    // findRecentNotices 의 정렬 검증을 위해 createdAt 을 임의 시각으로 고정해야 한다.
    // JPA persist 경로는 AuditingEntityListener 가 createdAt 을 현재 시각으로 덮으므로,
    // persist 후 native UPDATE 로 직접 갱신한다 (Migration 경로의 native INSERT 와 동일하게
    // auditor 를 우회하는 정책).
    private fun persistNotice(
        name: String = "테스트 공지",
        category: NoticeCategory? = NoticeCategory.COMPANY,
        branchCode: String? = null,
        contents: String? = null,
        isDeleted: Boolean? = null,
        scope: NoticeScope? = null,
        // 사용자 노출 조회(findNotices/findRecentNotices)는 status=PUBLISHED 만 반환하므로
        // 기본값을 PUBLISHED 로 둔다 (DRAFT 미노출 검증이 필요한 케이스만 명시적으로 DRAFT 전달).
        status: NoticeStatus? = NoticeStatus.PUBLISHED,
        createdDate: LocalDateTime? = LocalDateTime.now()
    ): Notice {
        val notice = Notice(
            name = name,
            category = category,
            branchCode = branchCode,
            contents = contents,
            isDeleted = isDeleted,
            scope = scope,
            status = status
        )
        val persisted = testEntityManager.persistAndFlush(notice)
        if (createdDate != null) {
            testEntityManager.entityManager
                .createNativeQuery("UPDATE notice SET created_at = :ts WHERE notice_id = :id")
                .setParameter("ts", createdDate)
                .setParameter("id", persisted.id)
                .executeUpdate()
        }
        testEntityManager.clear()
        return persisted
    }

    @Nested
    @DisplayName("findRecentNotices - 최근 공지 조회")
    inner class FindRecentNoticesTests {

        @Test
        @DisplayName("지점공지 + 전체공지를 함께 조회한다")
        fun branchAndAll() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "부산1지점 공지", category = NoticeCategory.BRANCH, branchCode = "부산1지점", createdDate = now.minus(1, ChronoUnit.HOURS))
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY, createdDate = now.minus(2, ChronoUnit.HOURS))
            persistNotice(name = "서울1지점 공지", category = NoticeCategory.BRANCH, branchCode = "서울1지점", createdDate = now.minus(3, ChronoUnit.HOURS))

            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.name }).containsExactly("부산1지점 공지", "전체 공지")
        }

        @Test
        @DisplayName("교육 공지도 함께 조회된다")
        fun includesEducation() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "교육 공지", category = NoticeCategory.EDUCATION, createdDate = now.minus(1, ChronoUnit.HOURS))
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY, createdDate = now.minus(2, ChronoUnit.HOURS))

            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.name }).containsExactly("교육 공지", "전체 공지")
        }

        @Test
        @DisplayName("최신순으로 정렬되어 반환된다")
        fun orderedByCreatedDateDesc() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "가장 오래된 공지", category = NoticeCategory.COMPANY, createdDate = now.minus(3, ChronoUnit.DAYS))
            persistNotice(name = "중간 공지", category = NoticeCategory.COMPANY, createdDate = now.minus(1, ChronoUnit.DAYS))
            persistNotice(name = "최신 공지", category = NoticeCategory.COMPANY, createdDate = now.minus(1, ChronoUnit.HOURS))

            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result).hasSize(3)
            assertThat(result[0].name).isEqualTo("최신 공지")
            assertThat(result[1].name).isEqualTo("중간 공지")
            assertThat(result[2].name).isEqualTo("가장 오래된 공지")
        }

        @Test
        @DisplayName("최대 5개만 반환된다")
        fun maxFive() {
            // Given
            val now = LocalDateTime.now()
            for (i in 1..8) {
                persistNotice(name = "공지 $i", category = NoticeCategory.COMPANY, createdDate = now.minus(i.toLong(), ChronoUnit.HOURS))
            }

            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result).hasSize(5)
        }

        @Test
        @DisplayName("날짜와 무관하게 최신 5건을 반환한다")
        fun returnsLatestRegardlessOfDate() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "오래된 공지", category = NoticeCategory.COMPANY, createdDate = now.minus(30, ChronoUnit.DAYS))
            persistNotice(name = "최근 공지", category = NoticeCategory.COMPANY, createdDate = now.minus(1, ChronoUnit.DAYS))

            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("최근 공지")
            assertThat(result[1].name).isEqualTo("오래된 공지")
        }

        @Test
        @DisplayName("공지가 없으면 빈 목록을 반환한다")
        fun noNotices() {
            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("다른 지점의 지점공지는 조회되지 않는다")
        fun otherBranchExcluded() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "서울1지점 전용 공지", category = NoticeCategory.BRANCH, branchCode = "서울1지점", createdDate = now.minus(1, ChronoUnit.HOURS))

            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("영업사원 공개범위 공지(예: 판매전략실)는 제외된다 - 레거시 scope 화이트리스트")
        fun excludesSalesEmployeeScope() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "현장여사원 공지", category = NoticeCategory.COMPANY, scope = NoticeScope.FIELD_FEMALE_EMPLOYEE, createdDate = now.minus(1, ChronoUnit.HOURS))
            persistNotice(name = "scope 미지정 공지", category = NoticeCategory.COMPANY, scope = null, createdDate = now.minus(2, ChronoUnit.HOURS))
            persistNotice(name = "판매전략실 공지", category = NoticeCategory.COMPANY, scope = NoticeScope.SALES_EMPLOYEE, createdDate = now.minus(3, ChronoUnit.HOURS))

            // When
            val result = noticeRepository.findRecentNotices("부산1지점")

            // Then
            assertThat(result.map { it.name }).containsExactly("현장여사원 공지", "scope 미지정 공지")
        }
    }

    @Nested
    @DisplayName("findNotices - 공지 목록 조회 (QueryDSL)")
    inner class FindNoticesTests {

        private val pageable = PageRequest.of(0, 10)

        @Test
        @DisplayName("전체 조회 (category=null) - COMPANY + 해당 branch BRANCH 공지 반환, isDeleted 제외")
        fun findAll() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "서울지점 공지", category = NoticeCategory.BRANCH, branchCode = "서울지점")
            persistNotice(name = "부산지점 공지", category = NoticeCategory.BRANCH, branchCode = "부산지점")
            persistNotice(name = "삭제된 공지", category = NoticeCategory.COMPANY, isDeleted = true)

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content.map { it.name }).containsExactlyInAnyOrder("전체 공지", "서울지점 공지")
        }

        @Test
        @DisplayName("회사공지 필터 (category=COMPANY) - COMPANY 공지만 반환")
        fun filterCompany() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "서울지점 공지", category = NoticeCategory.BRANCH, branchCode = "서울지점")

            // When
            val result = noticeRepository.findNotices(NoticeCategory.COMPANY, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("전체 공지")
        }

        @Test
        @DisplayName("지점공지 필터 (category=BRANCH) - 해당 지점 BRANCH 공지만 반환")
        fun filterBranch() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "서울지점 공지", category = NoticeCategory.BRANCH, branchCode = "서울지점")
            persistNotice(name = "부산지점 공지", category = NoticeCategory.BRANCH, branchCode = "부산지점")

            // When
            val result = noticeRepository.findNotices(NoticeCategory.BRANCH, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("서울지점 공지")
        }

        @Test
        @DisplayName("교육 필터 (category=EDUCATION) - EDUCATION 공지만 반환")
        fun filterEducation() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "교육 공지", category = NoticeCategory.EDUCATION)

            // When
            val result = noticeRepository.findNotices(NoticeCategory.EDUCATION, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("교육 공지")
        }

        @Test
        @DisplayName("텍스트 검색 (제목) - name에 검색어가 포함된 공지 반환")
        fun searchByTitle() {
            // Given
            persistNotice(name = "신제품 안내 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "기타 공지", category = NoticeCategory.COMPANY)

            // When
            val result = noticeRepository.findNotices(null, "안내", "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("신제품 안내 공지")
        }

        @Test
        @DisplayName("텍스트 검색 (본문) - contents에 검색어가 포함된 공지 반환")
        fun searchByContents() {
            // Given
            persistNotice(name = "공지1", category = NoticeCategory.COMPANY, contents = "중요한 내용입니다")
            persistNotice(name = "공지2", category = NoticeCategory.COMPANY, contents = "기타 정보")

            // When
            val result = noticeRepository.findNotices(null, "내용", "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("공지1")
        }

        @Test
        @DisplayName("검색 결과 없음 - 빈 Page 반환")
        fun searchNoResults() {
            // Given
            persistNotice(name = "일반 공지", category = NoticeCategory.COMPANY)

            // When
            val result = noticeRepository.findNotices(null, "존재하지않는검색어", "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("페이지네이션 - 15건 중 page=0, size=10이면 10건 반환")
        fun pagination() {
            // Given
            val now = LocalDateTime.now()
            for (i in 1..15) {
                persistNotice(name = "공지 $i", category = NoticeCategory.COMPANY, createdDate = now.minus(i.toLong(), ChronoUnit.MINUTES))
            }

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", PageRequest.of(0, 10))

            // Then
            assertThat(result.content).hasSize(10)
            assertThat(result.totalElements).isEqualTo(15)
            assertThat(result.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("isDeleted=true 제외 - 삭제된 공지가 결과에서 제외됨")
        fun excludeDeleted() {
            // Given
            persistNotice(name = "정상 공지", category = NoticeCategory.COMPANY, isDeleted = false)
            persistNotice(name = "삭제된 공지", category = NoticeCategory.COMPANY, isDeleted = true)

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("정상 공지")
        }

        @Test
        @DisplayName("영업사원 공개범위 공지(예: 판매전략실)는 제외된다 - 레거시 scope 화이트리스트")
        fun excludesSalesEmployeeScope() {
            // Given
            persistNotice(name = "현장여사원 공지", category = NoticeCategory.COMPANY, scope = NoticeScope.FIELD_FEMALE_EMPLOYEE)
            persistNotice(name = "scope 미지정 공지", category = NoticeCategory.COMPANY, scope = null)
            persistNotice(name = "판매전략실 공지", category = NoticeCategory.COMPANY, scope = NoticeScope.SALES_EMPLOYEE)

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content.map { it.name })
                .containsExactlyInAnyOrder("현장여사원 공지", "scope 미지정 공지")
        }

        @Test
        @DisplayName("isDeleted=null 포함 - isDeleted가 null인 공지가 결과에 포함됨")
        fun includeNullDeleted() {
            // Given
            persistNotice(name = "isDeleted=null 공지", category = NoticeCategory.COMPANY, isDeleted = null)
            persistNotice(name = "isDeleted=false 공지", category = NoticeCategory.COMPANY, isDeleted = false)

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content.map { it.name })
                .containsExactlyInAnyOrder("isDeleted=null 공지", "isDeleted=false 공지")
        }
    }

    @Nested
    @DisplayName("findPushTargetTokens - 푸시 대상 FCM 토큰 조회 (활성 사용자 필터)")
    inner class FindPushTargetTokensTests {

        /**
         * 푸시 대상 Employee(+EmployeeInfo) 를 직접 영속한다.
         *
         * Employee 생성자의 fcmToken 파라미터는 employeeCode 가 non-null 이면 함께 만들어지는
         * EmployeeInfo(employee_info) 로 흘러들어간다(PK 공유 @MapsId, cascade). 따라서 별도
         * EmployeeInfo persist 없이 persistAndFlush(employee) 한 번으로 두 테이블이 함께 적재된다.
         * 이 쿼리는 employee ⋈ employeeInfo 만 조회하므로 Notice 는 무관(persist 불필요).
         */
        private fun persistEmployee(
            employeeCode: String,
            appLoginActive: Boolean?,
            isDeleted: Boolean? = null,
            costCenterCode: String? = null,
            fcmToken: String? = null
        ): Employee {
            val employee = Employee(
                employeeCode = employeeCode,
                name = "테스트사원-$employeeCode",
                isDeleted = isDeleted,
                fcmToken = fcmToken
            ).apply {
                this.appLoginActive = appLoginActive
                this.costCenterCode = costCenterCode
            }
            val persisted = testEntityManager.persistAndFlush(employee)
            testEntityManager.clear()
            return persisted
        }

        @Test
        @DisplayName("회사공지(COMPANY) - 활성 + fcmToken 보유 사용자의 토큰을 반환한다")
        fun companyReturnsActiveTokens() {
            // Given
            persistEmployee(employeeCode = "A001", appLoginActive = true, fcmToken = "token-A001")
            persistEmployee(employeeCode = "A002", appLoginActive = true, fcmToken = "token-A002")

            // When
            val result = noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null)

            // Then
            assertThat(result).containsExactlyInAnyOrder("token-A001", "token-A002")
        }

        @Test
        @DisplayName("퇴사/비활성 제외 - appLoginActive=false 또는 null 사용자는 토큰이 있어도 제외된다")
        fun excludesInactiveUsers() {
            // Given
            persistEmployee(employeeCode = "B001", appLoginActive = true, fcmToken = "token-active")
            persistEmployee(employeeCode = "B002", appLoginActive = false, fcmToken = "token-inactive")
            persistEmployee(employeeCode = "B003", appLoginActive = null, fcmToken = "token-null-active")

            // When
            val result = noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null)

            // Then
            assertThat(result).containsExactly("token-active")
        }

        @Test
        @DisplayName("isDeleted=true 제외 - 삭제 사용자는 활성 + 토큰 보유여도 제외된다")
        fun excludesDeletedUsers() {
            // Given
            persistEmployee(employeeCode = "C001", appLoginActive = true, isDeleted = false, fcmToken = "token-not-deleted")
            persistEmployee(employeeCode = "C002", appLoginActive = true, isDeleted = null, fcmToken = "token-deleted-null")
            persistEmployee(employeeCode = "C003", appLoginActive = true, isDeleted = true, fcmToken = "token-deleted")

            // When
            val result = noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null)

            // Then — isDeleted=true 만 제외, null/false 는 포함
            assertThat(result).containsExactlyInAnyOrder("token-not-deleted", "token-deleted-null")
        }

        @Test
        @DisplayName("fcmToken null/빈문자열 제외 - 토큰 미보유 사용자는 제외된다")
        fun excludesBlankTokens() {
            // Given
            persistEmployee(employeeCode = "D001", appLoginActive = true, fcmToken = "token-valid")
            persistEmployee(employeeCode = "D002", appLoginActive = true, fcmToken = null)
            persistEmployee(employeeCode = "D003", appLoginActive = true, fcmToken = "")

            // When
            val result = noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null)

            // Then
            assertThat(result).containsExactly("token-valid")
        }

        @Test
        @DisplayName("지점공지(BRANCH) - costCenterCode 일치 활성 사용자만 반환, 다른 지점 제외")
        fun branchFiltersByCostCenterCode() {
            // Given
            persistEmployee(employeeCode = "E001", appLoginActive = true, costCenterCode = "X", fcmToken = "token-X-1")
            persistEmployee(employeeCode = "E002", appLoginActive = true, costCenterCode = "X", fcmToken = "token-X-2")
            persistEmployee(employeeCode = "E003", appLoginActive = true, costCenterCode = "Y", fcmToken = "token-Y")
            // 지점 일치하지만 비활성 → 제외
            persistEmployee(employeeCode = "E004", appLoginActive = false, costCenterCode = "X", fcmToken = "token-X-inactive")

            // When
            val result = noticeRepository.findPushTargetTokens(NoticeCategory.BRANCH, "X")

            // Then
            assertThat(result).containsExactlyInAnyOrder("token-X-1", "token-X-2")
        }

        @Test
        @DisplayName("지점공지(BRANCH) + branchCode=null - 오발송 방지로 빈 목록 즉시 반환")
        fun branchWithNullBranchCodeReturnsEmpty() {
            // Given — 활성 + 토큰 보유 사용자가 있어도 branchCode 가 비면 대상 없음
            persistEmployee(employeeCode = "F001", appLoginActive = true, costCenterCode = "X", fcmToken = "token-F")

            // When
            val result = noticeRepository.findPushTargetTokens(NoticeCategory.BRANCH, null)

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("distinct - 동일 fcmToken 은 한 번만 반환된다")
        fun returnsDistinctTokens() {
            // Given — 서로 다른 사원이지만 우연히 동일 토큰(멀티기기 잔재 등)
            persistEmployee(employeeCode = "G001", appLoginActive = true, fcmToken = "dup-token")
            persistEmployee(employeeCode = "G002", appLoginActive = true, fcmToken = "dup-token")

            // When
            val result = noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null)

            // Then
            assertThat(result).containsExactly("dup-token")
        }
    }

    @Nested
    @DisplayName("countPushTargets - 푸시 대상 수 조회 (findPushTargetTokens 와 정합)")
    inner class CountPushTargetsTests {

        /** FindPushTargetTokensTests 와 동일한 방식으로 대상 Employee(+EmployeeInfo) 를 영속한다. */
        private fun persistEmployee(
            employeeCode: String,
            appLoginActive: Boolean?,
            isDeleted: Boolean? = null,
            costCenterCode: String? = null,
            fcmToken: String? = null
        ): Employee {
            val employee = Employee(
                employeeCode = employeeCode,
                name = "테스트사원-$employeeCode",
                isDeleted = isDeleted,
                fcmToken = fcmToken
            ).apply {
                this.appLoginActive = appLoginActive
                this.costCenterCode = costCenterCode
            }
            val persisted = testEntityManager.persistAndFlush(employee)
            testEntityManager.clear()
            return persisted
        }

        @Test
        @DisplayName("회사공지 - 활성 필터 적용 후 대상 수는 토큰 목록 크기와 같다")
        fun companyCountMatchesTokenSize() {
            // Given — 포함 2 (활성+토큰) / 제외 3 (비활성, 삭제, 토큰없음)
            persistEmployee(employeeCode = "H001", appLoginActive = true, fcmToken = "token-H001")
            persistEmployee(employeeCode = "H002", appLoginActive = true, fcmToken = "token-H002")
            persistEmployee(employeeCode = "H003", appLoginActive = false, fcmToken = "token-inactive")
            persistEmployee(employeeCode = "H004", appLoginActive = true, isDeleted = true, fcmToken = "token-deleted")
            persistEmployee(employeeCode = "H005", appLoginActive = true, fcmToken = null)

            // When
            val count = noticeRepository.countPushTargets(NoticeCategory.COMPANY, null)
            val tokens = noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null)

            // Then — 핵심 불변식: 예상 대상 수 == 실제 발송 토큰 수
            assertThat(count).isEqualTo(2L)
            assertThat(count).isEqualTo(tokens.size.toLong())
        }

        @Test
        @DisplayName("지점공지 - costCenterCode 일치 사용자만 카운트, 토큰 목록 크기와 같다")
        fun branchCountMatchesTokenSize() {
            // Given
            persistEmployee(employeeCode = "I001", appLoginActive = true, costCenterCode = "X", fcmToken = "token-X-1")
            persistEmployee(employeeCode = "I002", appLoginActive = true, costCenterCode = "X", fcmToken = "token-X-2")
            persistEmployee(employeeCode = "I003", appLoginActive = true, costCenterCode = "Y", fcmToken = "token-Y")

            // When
            val count = noticeRepository.countPushTargets(NoticeCategory.BRANCH, "X")
            val tokens = noticeRepository.findPushTargetTokens(NoticeCategory.BRANCH, "X")

            // Then
            assertThat(count).isEqualTo(2L)
            assertThat(count).isEqualTo(tokens.size.toLong())
        }

        @Test
        @DisplayName("지점공지 + branchCode=null - 오발송 방지로 0 반환")
        fun branchWithNullBranchCodeReturnsZero() {
            // Given — 활성 + 토큰 보유 사용자가 있어도 branchCode 가 비면 대상 없음
            persistEmployee(employeeCode = "J001", appLoginActive = true, costCenterCode = "X", fcmToken = "token-J")

            // When
            val count = noticeRepository.countPushTargets(NoticeCategory.BRANCH, null)

            // Then
            assertThat(count).isZero()
        }

        @Test
        @DisplayName("distinct - 동일 fcmToken 은 1건으로 카운트된다")
        fun countsDistinctTokens() {
            // Given — 서로 다른 사원이 동일 토큰(멀티기기 잔재 등)
            persistEmployee(employeeCode = "K001", appLoginActive = true, fcmToken = "dup-token")
            persistEmployee(employeeCode = "K002", appLoginActive = true, fcmToken = "dup-token")

            // When
            val count = noticeRepository.countPushTargets(NoticeCategory.COMPANY, null)
            val tokens = noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null)

            // Then — distinct 정합 (토큰 목록도 1건)
            assertThat(count).isEqualTo(1L)
            assertThat(count).isEqualTo(tokens.size.toLong())
        }
    }
}
