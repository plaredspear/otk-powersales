package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.PushMessage
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageRepository : JpaRepository<PushMessage, Int>
