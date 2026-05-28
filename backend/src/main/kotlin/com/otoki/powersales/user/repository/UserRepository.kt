package com.otoki.powersales.user.repository

import com.otoki.powersales.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {

    fun findByUsername(username: String): User?

    fun findByEmployeeCode(employeeCode: String): User?

    fun findByEmployeeCodeIn(employeeCodes: Collection<String>): List<User>

    /** Spec #803 — Profile 상세의 부여 사용자 수 + 일람용. */
    fun countByProfileId(profileId: Long): Long

    fun findFirstByProfileId(profileId: Long): User?

    /**
     * SF user sfid → 신규 User PK 일괄 매핑.
     *
     * SharingPolicyQueryRepository 가 sharing rule condition 의 audit/owner field value (SF user sfid) 를
     * snapshot 적재 시점에 신규 User.id 로 pre-resolve 하는 용도.
     */
    @Query("SELECT u.sfid, u.id FROM User u WHERE u.sfid IN :sfids")
    fun findIdsBySfidIn(sfids: Collection<String>): List<Array<Any>>
}
