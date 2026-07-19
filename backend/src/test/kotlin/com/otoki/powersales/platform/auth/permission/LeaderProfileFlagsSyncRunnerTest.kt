package com.otoki.powersales.platform.auth.permission

import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.platform.auth.entity.Profile
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.platform.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.platform.auth.sharing.repository.ProfileFlagsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.ApplicationArguments
import org.springframework.cache.CacheManager
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper

/**
 * NOTE: LeaderProfileFlagsSyncRunner 는 현재 `@Component` 미부착으로 부팅 시 실행되지 않는다
 * (SF 데이터 마이그레이션 SF값 완전 우선 결정 — 클래스 KDoc 참조). 본 테스트는 되살릴 경우를 대비해
 * run() 로직 자체의 회귀를 계속 보장한다 (직접 인스턴스화 — 스캔 애노테이션 무관).
 */
@DisplayName("LeaderProfileFlagsSyncRunner 테스트 (현재 빈 미등록 — 로직 회귀 보존용)")
class LeaderProfileFlagsSyncRunnerTest {

    private val profileRepository: ProfileRepository = mockk()
    private val profileFlagsRepository: ProfileFlagsRepository = mockk(relaxed = true)
    private val adminPermissionCache: AdminPermissionCache = mockk(relaxed = true)
    private val adminDataScopeCache: AdminDataScopeCache = mockk(relaxed = true)
    private val cacheManager: CacheManager = mockk(relaxed = true)
    private val objectMapper: ObjectMapper = ObjectMapper()
    private val transactionTemplate: TransactionTemplate = mockk()

    private val runner = LeaderProfileFlagsSyncRunner(
        profileRepository,
        profileFlagsRepository,
        adminPermissionCache,
        adminDataScopeCache,
        cacheManager,
        objectMapper,
        transactionTemplate,
    )

    private val args: ApplicationArguments = mockk(relaxed = true)

    init {
        // TransactionTemplate.execute 는 콜백을 그대로 실행하도록 stub.
        every { transactionTemplate.execute(any<TransactionCallback<Int>>()) } answers {
            firstArg<TransactionCallback<Int>>().doInTransaction(mockk(relaxed = true))
        }
    }

    private fun profile(id: Long, name: String) = Profile(id = id, name = name)

    @Test
    @DisplayName("row 부재 시 seed 값으로 신규 생성 + is_locally_modified=false")
    fun `creates flags when absent`() {
        every { profileRepository.findByName("6.조장") } returns profile(6L, "6.조장")
        every { profileRepository.findByName("7.영업사원 + 조장") } returns profile(7L, "7.영업사원 + 조장")
        every { profileFlagsRepository.findByProfileId(any()) } returns null

        val saved = mutableListOf<ProfileFlags>()
        every { profileFlagsRepository.save(capture(saved)) } answers { firstArg() }

        runner.run(args)

        assertThat(saved).hasSize(2)
        val leader6 = saved.first { it.profileId == 6L }
        assertThat(leader6.permissionsApiEnabled).isTrue
        assertThat(leader6.permissionsViewAllData).isFalse
        assertThat(leader6.isLocallyModified).isFalse
        // 6.조장 custom_permissions 에 female_employee 포함, object_permissions 는 compact JSON.
        assertThat(leader6.customPermissions).contains("female_employee")
        assertThat(leader6.objectPermissions).contains("MonthlyFemaleEmployeeIntegrationSchedule__c")
        assertThat(leader6.objectPermissions).doesNotContain("\n")
        // 6.조장 의 education_post 는 read 전용 (사용자 결정) — 과거 CRUD 4비트에서 축소.
        // 쓰기 비트가 되살아나는 회귀를 막는다. 7.영업사원+조장 은 CRUD 유지라 프로필별로 다르다.
        // JSON 을 파싱해 키/비트를 정확히 검증한다 (문자열 contains 는 education_post 가
        // education_post_attachment 의 접두사라 오탐/누락이 생긴다).
        val leader6Custom = ObjectMapper().readTree(leader6.customPermissions)
        val educationPost = leader6Custom.get("education_post")
        assertThat(educationPost).isNotNull
        assertThat(educationPost.get("allowRead")?.asBoolean()).isTrue
        // read 외 비트는 부재하거나 false 여야 한다.
        listOf("allowCreate", "allowEdit", "allowDelete").forEach { bit ->
            assertThat(educationPost.get(bit)?.asBoolean() ?: false)
                .describedAs("6.조장 education_post.%s 는 부여되지 않아야 한다", bit)
                .isFalse
        }
        // 교육 세부 자원은 가드 entity 가 아니라 (AdminEducationController 는 education_post 단일 가드)
        // SoT 에 기재하지 않는다 — 죽은 키가 유입되는 회귀 방지.
        listOf("education_code", "education_post_attachment", "education_view_history").forEach { key ->
            assertThat(leader6Custom.has(key))
                .describedAs("%s 는 가드 entity 가 아니므로 SoT 에 없어야 한다", key)
                .isFalse
        }
        // PPT 마스터/이력 조회 권한 (요청 확인분 — 이미 부여돼 있어야 한다).
        val leader6Object = ObjectMapper().readTree(leader6.objectPermissions)
        listOf("ProfessionalPromotionTeamMaster__c", "ProfessionalPromotionTeamHistory__c").forEach { obj ->
            assertThat(leader6Object.get(obj)?.get("allowRead")?.asBoolean())
                .describedAs("6.조장 %s 는 read 가 부여돼야 한다", obj)
                .isTrue
        }

        val leader7 = saved.first { it.profileId == 7L }
        assertThat(leader7.permissionsApiEnabled).isTrue
        // 7.영업사원+조장 custom_permissions 는 education_post 만 포함(female_employee 는 6.조장 전용).
        assertThat(leader7.customPermissions).contains("education_post")
        assertThat(leader7.customPermissions).doesNotContain("female_employee")
        assertThat(leader7.objectPermissions).contains("viewAllRecords")
    }

    @Test
    @DisplayName("is_locally_modified=true row 는 skip (web admin 편집분 보호)")
    fun `skips dirty rows`() {
        every { profileRepository.findByName("6.조장") } returns profile(6L, "6.조장")
        every { profileRepository.findByName("7.영업사원 + 조장") } returns profile(7L, "7.영업사원 + 조장")
        every { profileFlagsRepository.findByProfileId(6L) } returns
            ProfileFlags(profileId = 6L).apply { isLocallyModified = true }
        every { profileFlagsRepository.findByProfileId(7L) } returns null
        every { profileFlagsRepository.save(any()) } answers { firstArg() }

        runner.run(args)

        // 7 만 저장, 6 은 dirty 라 skip.
        verify(exactly = 1) { profileFlagsRepository.save(match { it.profileId == 7L }) }
        verify(exactly = 0) { profileFlagsRepository.save(match { it.profileId == 6L }) }
    }

    @Test
    @DisplayName("is_locally_modified=false 기존 row 는 seed 값으로 갱신")
    fun `updates non-dirty existing row`() {
        every { profileRepository.findByName("6.조장") } returns profile(6L, "6.조장")
        every { profileRepository.findByName("7.영업사원 + 조장") } returns null
        val existing = ProfileFlags(profileId = 6L).apply {
            isLocallyModified = false
            permissionsApiEnabled = false
        }
        every { profileFlagsRepository.findByProfileId(6L) } returns existing

        val slot = slot<ProfileFlags>()
        every { profileFlagsRepository.save(capture(slot)) } answers { firstArg() }

        runner.run(args)

        assertThat(slot.captured.profileId).isEqualTo(6L)
        assertThat(slot.captured.permissionsApiEnabled).isTrue
        assertThat(slot.captured.isLocallyModified).isFalse
    }

    @Test
    @DisplayName("profile 미적재 시 해당 seed skip (예외 없이 진행)")
    fun `skips when profile missing`() {
        every { profileRepository.findByName(any()) } returns null

        runner.run(args)

        verify(exactly = 0) { profileFlagsRepository.save(any()) }
    }

    @Test
    @DisplayName("적용분 존재 시 권한 캐시 invalidate")
    fun `invalidates caches when applied`() {
        every { profileRepository.findByName(any()) } returns profile(6L, "6.조장")
        every { profileFlagsRepository.findByProfileId(any()) } returns null
        every { profileFlagsRepository.save(any()) } answers { firstArg() }

        runner.run(args)

        verify { adminPermissionCache.invalidateAll() }
        verify { adminDataScopeCache.invalidateAll() }
    }
}
