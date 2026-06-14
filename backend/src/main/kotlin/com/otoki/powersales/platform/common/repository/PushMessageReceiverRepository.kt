package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.push.entity.PushMessageReceiver
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageReceiverRepository : JpaRepository<PushMessageReceiver, Int>
