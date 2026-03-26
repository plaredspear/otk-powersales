package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.UploadFile
import org.springframework.data.jpa.repository.JpaRepository

interface UploadFileRepository : JpaRepository<UploadFile, Long> {

    fun findByRecordIdAndIsDeletedFalse(recordId: String): List<UploadFile>

    fun findByParentTypeAndParentIdAndIsDeletedFalse(parentType: String, parentId: Long): List<UploadFile>
}
