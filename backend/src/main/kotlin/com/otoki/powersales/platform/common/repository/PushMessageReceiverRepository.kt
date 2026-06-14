package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.common.entity.PushMessageReceiver
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageReceiverRepository : JpaRepository<PushMessageReceiver, Int>
