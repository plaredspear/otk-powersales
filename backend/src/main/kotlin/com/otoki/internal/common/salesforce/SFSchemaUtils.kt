package com.otoki.internal.common.salesforce

import jakarta.persistence.Column

object SFSchemaUtils {

    /**
     * @SFField가 부여된 필드의 SF Field name → @Column.name 매핑을 반환한다.
     */
    fun getSFMapping(entityClass: Class<*>): Map<String, String> {
        return entityClass.declaredFields
            .filter { it.isAnnotationPresent(SFField::class.java) }
            .associate { field ->
                val sfName = field.getAnnotation(SFField::class.java).value
                val colName = field.getAnnotation(Column::class.java)?.name ?: field.name
                sfName to colName
            }
    }

    /**
     * @HCColumn이 부여된 필드의 HC Column name → @Column.name 매핑을 반환한다.
     */
    fun getHCMapping(entityClass: Class<*>): Map<String, String> {
        return entityClass.declaredFields
            .filter { it.isAnnotationPresent(HCColumn::class.java) }
            .associate { field ->
                val hcName = field.getAnnotation(HCColumn::class.java).value
                val colName = field.getAnnotation(Column::class.java)?.name ?: field.name
                hcName to colName
            }
    }

    /**
     * @HCTable + @HCColumn 기반으로 SELECT SQL을 생성한다.
     * HC Column과 Dev DB Column이 동일하면 컬럼명만, 다르면 hc_col AS dev_col 형태.
     * 스키마 접두사는 salesforce.
     */
    fun generateImportSql(entityClass: Class<*>): String {
        val tableName = entityClass.getAnnotation(HCTable::class.java)?.value
            ?: throw IllegalArgumentException("${entityClass.simpleName} does not have @HCTable annotation")

        val columns = entityClass.declaredFields
            .filter { it.isAnnotationPresent(HCColumn::class.java) }
            .map { field ->
                val hcName = field.getAnnotation(HCColumn::class.java).value
                val colName = field.getAnnotation(Column::class.java)?.name ?: field.name
                if (hcName == colName) hcName else "$hcName AS $colName"
            }

        return "SELECT ${columns.joinToString(", ")} FROM salesforce.$tableName"
    }
}
