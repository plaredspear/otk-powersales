package com.otoki.powersales.platform.common.config

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.dto.PermissionSetSnapshot
import com.otoki.powersales.platform.auth.sharing.dto.ProfileFlagsSnapshot
import com.otoki.powersales.platform.auth.sharing.dto.SharingRuleSnapshot
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.organization.repository.dto.OrganizationCacheDto
import com.otoki.powersales.platform.common.config.CacheConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * [CacheConfig.defaultRedisCacheConfiguration] 가 구성하는 Redis value serializer 로
 * [DataScope] 가 손실 없이 round-trip 되는지 검증.
 *
 * ## 배경 — admin API 500 (Cannot map null into type boolean)
 *
 * 운영에서 `admin-data-scope` 캐시 hit 시 `DataScope["isAllBranches"]` 를 `null` 로 역직렬화하다
 * Jackson 3 `FAIL_ON_NULL_FOR_PRIMITIVES` 로 예외가 발생했다. cache name 을 `:v2` 로 bump 해도
 * 재발했다면 원인은 옛 entry 잔존이 아니라 **직렬화기 자체가 `is`-prefixed Kotlin Boolean 필드를
 * write 한 property 이름과 read 시 기대 이름이 어긋나는 것** — 본 테스트가 그 라운드트립을 직접 재현.
 *
 * `ConcurrentMapCacheManager` 기반 기존 캐시 테스트는 직렬화를 거치지 않아 본 버그를 잡지 못한다.
 */
@DisplayName("DataScope Redis serializer round-trip")
class DataScopeRedisSerializationTest {

    @Test
    @DisplayName("isAllBranches=false 인 DataScope 가 손실 없이 round-trip 된다")
    fun roundTrip_isAllBranchesFalse() {
        val config = CacheConfig().defaultRedisCacheConfiguration()
        val pair = config.valueSerializationPair

        val original = DataScope(branchCodes = listOf("3234"), isAllBranches = false)

        val bytes = pair.write(original)
        val json = String(bytes.array(), bytes.position(), bytes.remaining())
        println("serialized JSON = $json")

        val restored = pair.read(ByteBuffer.wrap(bytes.array())) as DataScope

        assertThat(restored.isAllBranches).isFalse()
        assertThat(restored.branchCodes).containsExactly("3234")
        assertThat(restored).isEqualTo(original)
    }

    @Test
    @DisplayName("isAllBranches=true 인 DataScope 가 손실 없이 round-trip 된다")
    fun roundTrip_isAllBranchesTrue() {
        val config = CacheConfig().defaultRedisCacheConfiguration()
        val pair = config.valueSerializationPair

        val original = DataScope(branchCodes = emptyList(), isAllBranches = true)

        val bytes = pair.write(original)
        val restored = pair.read(ByteBuffer.wrap(bytes.array())) as DataScope

        assertThat(restored.isAllBranches).isTrue()
        assertThat(restored).isEqualTo(original)
    }

    /**
     * 캐시에 실제 저장되는 다른 DTO 들도 KotlinModule 등록 후 손실 없이 round-trip 되는지 회귀 검증.
     *
     * 각 DTO 는 `@Cacheable` 반환 타입 — Redis value serializer 를 거친다:
     *  - [ProfileFlagsSnapshot]  : `profileFlags` 캐시
     *  - [PermissionSetSnapshot] : `permissionSetFlags:v2` 캐시 (Map<String, Boolean> 중첩 포함)
     *  - [BranchResponse]        : `teamScheduleBranchesV2` 캐시 (List 형태로 저장)
     *  - [OrganizationCacheDto]  : `organizationCascadeV2` 캐시 (nullable field)
     *  - [SharingRuleSnapshot]   : `sharing-rules-for-user:v1` 캐시 (중첩 list + non-`is` Boolean)
     */
    @Test
    @DisplayName("나머지 캐시 DTO 도 손실 없이 round-trip 된다")
    fun roundTrip_otherCachedDtos() {
        val pair = CacheConfig().defaultRedisCacheConfiguration().valueSerializationPair

        fun roundTrip(original: Any): Any? {
            val bytes = pair.write(original)
            return pair.read(ByteBuffer.wrap(bytes.array()))
        }

        val profileFlags = ProfileFlagsSnapshot(
            viewAllData = true, modifyAllData = false, viewAllUsers = true,
            manageUsers = false, apiEnabled = true,
        )
        assertThat(roundTrip(profileFlags)).isEqualTo(profileFlags)

        val permissionSet = PermissionSetSnapshot(
            viewAllDataSystem = true,
            modifyAllDataSystem = false,
            viewAllRecordsBySObject = mapOf("Account" to true),
            modifyAllRecordsBySObject = mapOf("Account" to false),
            permissionSetIds = setOf(10L, 20L),
        )
        assertThat(roundTrip(permissionSet)).isEqualTo(permissionSet)

        val branch = BranchResponse(branchCode = "3234", branchName = "서울지점")
        assertThat(roundTrip(branch)).isEqualTo(branch)

        // List<BranchResponse> — teamScheduleBranches 캐시의 실제 저장 형태
        val branches = listOf(branch, BranchResponse(branchCode = "3235", branchName = "부산지점"))
        @Suppress("UNCHECKED_CAST")
        assertThat(roundTrip(branches) as List<BranchResponse>).containsExactlyElementsOf(branches)

        val orgDto = OrganizationCacheDto(
            orgCodeLevel3 = "100", orgNameLevel3 = "영업본부",
            orgNameLevel4 = null, costCenterLevel3 = "3234",
        )
        assertThat(roundTrip(orgDto)).isEqualTo(orgDto)

        val sharingRule = SharingRuleSnapshot(
            sharingRuleId = 1L, developerName = "Rule_A", sObjectName = "Account",
            ruleType = "CRITERIA", accessLevel = "Read", includeOwnedByAll = true,
            conditions = listOf(
                SharingRuleSnapshot.ConditionSnapshot(
                    field = "BranchCode__c", operator = "equals", value = "3234",
                    conditionOrder = 1, logicConnector = null, resolvedUserId = 99L,
                ),
            ),
        )
        assertThat(roundTrip(sharingRule)).isEqualTo(sharingRule)
    }

    /**
     * `admin-permission:v1` 캐시가 저장하는 평탄화 권한 key set (`Set<String>`) round-trip 검증.
     *
     * [com.otoki.powersales.platform.auth.permission.SfPermissionResolver.resolveForUser] 가
     * `mutableSetOf<String>()` (런타임 `LinkedHashSet`) 을 반환하므로 그 실 타입으로 재현.
     * 여사원 일정관리 화면이 요구하는 두 권한 key 가 round-trip 후에도 보존되는지 확인.
     */
    @Test
    @DisplayName("admin-permission 캐시의 Set<String> 권한 key set 이 손실 없이 round-trip 된다")
    fun roundTrip_permissionKeySet() {
        val pair = CacheConfig().defaultRedisCacheConfiguration().valueSerializationPair

        val original: Set<String> = linkedSetOf(
            "monthly_female_employee_integration_schedule:R",
            "team_member_schedule:R",
            "employee:R",
            "SYSTEM:VIEW_ALL_USERS",
        )

        val bytes = pair.write(original)
        val json = String(bytes.array(), bytes.position(), bytes.remaining())
        println("serialized permission set JSON = $json")

        @Suppress("UNCHECKED_CAST")
        val restored = pair.read(ByteBuffer.wrap(bytes.array())) as Set<String>

        assertThat(restored)
            .contains("monthly_female_employee_integration_schedule:R", "team_member_schedule:R")
            .containsExactlyInAnyOrderElementsOf(original)
    }
}
