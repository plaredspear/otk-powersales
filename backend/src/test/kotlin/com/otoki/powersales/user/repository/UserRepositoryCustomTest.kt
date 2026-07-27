package com.otoki.powersales.user.repository

import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

/**
 * UserRepositoryCustom QueryDSL 검증.
 *
 * - `findIdsBySfidIn` — SF user sfid → 신규 User.id (sfid, id) 쌍 매핑. 매칭 실패 sfid 누락 검증.
 * - `findUsers` — 사용자 관리 화면의 지점(costCenterCode) 필터 동작 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class UserRepositoryCustomTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    @DisplayName("findIdsBySfidIn - 매칭된 sfid 만 (sfid, id) 쌍으로 반환, 미매칭 sfid 는 누락")
    fun returnsMatchedPairsOnly() {
        val u1 = persist("user1", "EMP1", "005000000000001")
        val u2 = persist("user2", "EMP2", "005000000000002")
        persist("user3", "EMP3", null) // sfid 없음 → 매칭 대상 아님

        val result = userRepository.findIdsBySfidIn(
            listOf("005000000000001", "005000000000002", "005999999999999") // 마지막은 미존재
        )

        assertThat(result).containsExactlyInAnyOrder(
            "005000000000001" to u1.id,
            "005000000000002" to u2.id,
        )
    }

    @Test
    @DisplayName("findIdsBySfidIn - 빈 입력이면 빈 결과")
    fun emptyInput() {
        persist("user1", "EMP1", "005000000000001")

        assertThat(userRepository.findIdsBySfidIn(emptyList())).isEmpty()
    }

    @Test
    @DisplayName("findUsers - costCenterCode 필터는 해당 지점 사용자만 반환")
    fun findUsers_filtersByCostCenterCode() {
        persistWithBranch("branch.a1", "EMPA1", "5721")
        persistWithBranch("branch.a2", "EMPA2", "5721")
        persistWithBranch("branch.b1", "EMPB1", "5722")

        val result = userRepository.findUsers(
            keyword = null,
            isActive = null,
            profileId = null,
            costCenterCodes = listOf("5721"),
            pageable = PageRequest.of(0, 20),
        )

        assertThat(result.content).extracting<String> { it.username }
            .containsExactlyInAnyOrder("branch.a1", "branch.a2")
    }

    @Test
    @DisplayName("findUsers - costCenterCode 가 null 인 사용자는 지점 필터 시 제외, 미필터 시 포함")
    fun findUsers_excludesNullCostCenterWhenFiltered() {
        persistWithBranch("branch.a1", "EMPA1", "5721")
        persistWithBranch("admin.only", "EMPADM", null) // 사원 미매칭 관리자 계정

        val filtered = userRepository.findUsers(
            keyword = null, isActive = null, profileId = null,
            costCenterCodes = listOf("5721"), pageable = PageRequest.of(0, 20),
        )
        assertThat(filtered.content).extracting<String> { it.username }
            .containsExactly("branch.a1")

        // 지점 미선택(전체) 이면 costCenterCode 가 null 인 계정도 그대로 노출된다.
        val unfiltered = userRepository.findUsers(
            keyword = null, isActive = null, profileId = null,
            costCenterCodes = null, pageable = PageRequest.of(0, 20),
        )
        assertThat(unfiltered.content).extracting<String> { it.username }
            .containsExactlyInAnyOrder("branch.a1", "admin.only")
    }

    private fun persist(username: String, employeeCode: String, sfid: String?): User {
        val user = User(username = username, employeeCode = employeeCode, password = "x").apply {
            this.sfid = sfid
        }
        return em.persistAndFlush(user)
    }

    private fun persistWithBranch(username: String, employeeCode: String, costCenterCode: String?): User {
        val user = User(username = username, employeeCode = employeeCode, password = "x").apply {
            this.costCenterCode = costCenterCode
        }
        return em.persistAndFlush(user)
    }
}
