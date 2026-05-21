package com.otoki.powersales.auth.repository

import com.otoki.powersales.auth.entity.Profile
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileRepository : JpaRepository<Profile, Long> {
    fun findBySfid(sfid: String): Profile?
}
