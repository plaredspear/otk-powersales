package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.UploadFile
import org.springframework.data.jpa.repository.JpaRepository

interface UploadFileRepository : JpaRepository<UploadFile, Long> {

    fun findByRecordIdAndIsDeletedFalse(recordId: String): List<UploadFile>
}
