package com.otoki.powersales.common.salesforce

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
     *
     * BaseEntity 등 부모(@MappedSuperclass) 의 필드도 함께 수집 — BaseEntity.createdAt/updatedAt 의 @SFField 인식용.
     */
    fun getSFMapping(entityClass: Class<*>): Map<String, String> {
        return collectFieldsIncludingInherited(entityClass)
            .filter { it.isAnnotationPresent(SFField::class.java) }
            .associate { field ->
                val sfName = field.getAnnotation(SFField::class.java).value
                val colName = field.getAnnotation(Column::class.java)?.name ?: field.name
                sfName to colName
            }
    }

    /**
     * @HCColumn이 부여된 필드의 HC Column name → @Column.name 매핑을 반환한다.
     *
     * BaseEntity 등 부모 클래스의 필드도 함께 수집한다.
     */
    fun getHCMapping(entityClass: Class<*>): Map<String, String> {
        return collectFieldsIncludingInherited(entityClass)
            .filter { it.isAnnotationPresent(HCColumn::class.java) }
            .associate { field ->
                val hcName = field.getAnnotation(HCColumn::class.java).value
                val colName = field.getAnnotation(Column::class.java)?.name ?: field.name
                hcName to colName
            }
    }

    /**
     * @HCColumn이 부여된 필드의 매핑 정보를 리스트로 반환한다.
     *
     * BaseEntity 등 부모 클래스의 필드도 함께 수집한다.
     */
    fun getHCFieldMappings(entityClass: Class<*>): List<HCFieldMapping> {
        return collectFieldsIncludingInherited(entityClass)
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
     * 본 클래스 + 부모(@MappedSuperclass 등) 클래스의 declaredFields 를 모두 수집한다.
     *
     * 본 클래스 필드를 먼저, 그 다음 부모 필드를 추가. BaseEntity 의 createdAt/updatedAt 에 부여된 @SFField/@HCColumn 인식용.
     */
    private fun collectFieldsIncludingInherited(entityClass: Class<*>): List<java.lang.reflect.Field> {
        val result = mutableListOf<java.lang.reflect.Field>()
        var current: Class<*>? = entityClass
        while (current != null && current != Any::class.java) {
            result.addAll(current.declaredFields)
            current = current.superclass
        }
        return result
    }
}
