package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.ProfileFlags
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileFlagsRepository : JpaRepository<ProfileFlags, Long> {
    fun findByProfileId(profileId: Long): ProfileFlags?
}
