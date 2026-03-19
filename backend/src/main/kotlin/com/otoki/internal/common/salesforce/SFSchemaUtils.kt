package com.otoki.internal.common.salesforce

import jakarta.persistence.Column

object SFSchemaUtils {

    /**
     * @HCColumn 필드 매핑 정보 (Heroku 컬럼명 → JPA 컬럼명)
     */
    data class HCFieldMapping(
        val hcColumnName: String,
        val jpaColumnName: String,
        val fieldType: Class<*>
    )

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
     * @HCColumn이 부여된 필드의 매핑 정보를 리스트로 반환한다.
     */
    fun getHCFieldMappings(entityClass: Class<*>): List<HCFieldMapping> {
        return entityClass.declaredFields
            .filter { it.isAnnotationPresent(HCColumn::class.java) }
            .map { field ->
                HCFieldMapping(
                    hcColumnName = field.getAnnotation(HCColumn::class.java).value,
                    jpaColumnName = field.getAnnotation(Column::class.java)?.name ?: field.name,
                    fieldType = field.type
                )
            }
    }

    /**
     * @HCTable + @HCColumn 기반으로 SELECT SQL을 생성한다.
     * HC Column과 Dev DB Column이 동일하면 컬럼명만, 다르면 hc_col AS dev_col 형태.
     * @param schema 스키마명 (기본값: salesforce)
     */
    fun generateImportSql(entityClass: Class<*>, schema: String = "salesforce"): String {
        val tableName = entityClass.getAnnotation(HCTable::class.java)?.value
            ?: throw IllegalArgumentException("${entityClass.simpleName} does not have @HCTable annotation")

        val columns = entityClass.declaredFields
            .filter { it.isAnnotationPresent(HCColumn::class.java) }
            .map { field ->
                val hcName = field.getAnnotation(HCColumn::class.java).value
                val colName = field.getAnnotation(Column::class.java)?.name ?: field.name
                if (hcName == colName) hcName else "$hcName AS $colName"
            }

        return "SELECT ${columns.joinToString(", ")} FROM $schema.$tableName"
    }
}
