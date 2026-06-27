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
import org.springframework.test.context.ActiveProfiles

/**
 * UserRepositoryCustom QueryDSL 전환 검증 — `findIdsBySfidIn`.
 *
 * SF user sfid → 신규 User.id (sfid, id) 쌍 매핑. 매칭 실패 sfid 는 결과에서 누락되는지 실 DB 로 검증한다.
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

    private fun persist(username: String, employeeCode: String, sfid: String?): User {
        val user = User(username = username, employeeCode = employeeCode, password = "x").apply {
            this.sfid = sfid
        }
        return em.persistAndFlush(user)
    }
}
