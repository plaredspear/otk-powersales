/*
package com.otoki.internal.repository

import com.otoki.internal.entity.InspectionFieldType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/ **
 * 현장 유형 Repository
 * /
@Repository
interface InspectionFieldTypeRepository : JpaRepository<InspectionFieldType, String> {

    / **
     * 활성 상태인 현장 유형 목록 조회
     * sortOrder 순으로 정렬
     * /
    @Query(
        "SELECT f FROM InspectionFieldType f " +
        "WHERE f.isActive = true " +
        "ORDER BY f.sortOrder ASC, f.code ASC"
    )
    fun findActiveFieldTypes(): List<InspectionFieldType>
}
*/
