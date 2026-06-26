package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.common.entity.UploadFile
import org.springframework.data.jpa.repository.JpaRepository

interface UploadFileRepository : JpaRepository<UploadFile, Long> {

    fun findByRecordSfidAndIsDeletedFalse(recordSfid: String): List<UploadFile>

    fun findByParentTypeAndParentIdAndIsDeletedFalse(parentType: String, parentId: Long): List<UploadFile>

    fun findByParentTypeAndParentIdInAndIsDeletedFalse(
        parentType: String,
        parentIds: List<Long>
    ): List<UploadFile>

    fun findByIdAndParentTypeAndParentIdAndIsDeletedFalse(
        id: Long,
        parentType: String,
        parentId: Long
    ): UploadFile?

    // 본문 인라인 이미지 backfill 용 — placeholder 의 refid(=upload_file.id) 목록으로 일괄 조회.
    fun findByIdInAndParentTypeAndIsDeletedFalse(
        ids: Collection<Long>,
        parentType: String
    ): List<UploadFile>
}
