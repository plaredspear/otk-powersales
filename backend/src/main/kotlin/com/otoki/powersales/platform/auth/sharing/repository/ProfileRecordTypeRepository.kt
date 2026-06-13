package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.ProfileRecordType
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileRecordTypeRepository : JpaRepository<ProfileRecordType, Long> {
    fun findAllByProfileId(profileId: Long): List<ProfileRecordType>
}
