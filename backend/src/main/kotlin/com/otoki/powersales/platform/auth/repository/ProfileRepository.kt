package com.otoki.powersales.platform.auth.repository

import com.otoki.powersales.platform.auth.entity.Profile
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileRepository : JpaRepository<Profile, Long> {
    /** EmployeeProfileResolver.resolveProfileId() 의 enum value → id 변환 + LocalDataInitializer 의 부족분 시드 lookup. */
    fun findByName(name: String): Profile?
}
