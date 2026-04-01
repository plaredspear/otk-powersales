package com.otoki.internal.common.repository

import com.otoki.internal.entity.PushMessage
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageRepository : JpaRepository<PushMessage, Int>
