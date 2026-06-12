package com.otoki.powersales._migration.heroku.service

/**
 * Heroku Stage 2 FK resolve 매핑 SoT.
 *
 * SF 프레임워크 `SfFkResolveTables.kt` 의 `FK_PREFIX_MAPPING` 에 대응. 모든 식별자는 컴파일 타임
 * 상수라 SQL injection 없음. 런칭 후 패키지 통째 폐기 용이.
 *
 * 두 패턴:
 *  - [NATURAL_KEY_FK] — 패턴 A: 적재 테이블의 자연 키 컬럼을 참조 테이블 자연 키와 조인해 FK `*_id` 채움
 *  - [PARENT_FK] — 패턴 B: 같은 배치 부모-자식의 부모 자연 키로 부모 신규 serial PK 를 조인
 *
 * 참조/부모 매칭 키는 2026-06-06 레거시 코드/스키마 검증으로 확정 (design 문서 §2):
 *  - account_code / sap_account_code → account.external_key (SF externalkey__c = SAP Account Code)
 *  - product_code → product.product_code
 *  - employee_code / emp_code → employee.employee_code
 *  - tmp_order_product.tmp_order_id → tmp_order (employee_code 1:1, tmp_order PK = tmp_employeecode)
 */
object HerokuFkResolveTables {

    const val SCHEMA = "powersales"

    /**
     * 패턴 A — 자연 키 → 참조 테이블 serial id.
     *
     * @param sourceTable   적재 테이블 (FK 를 채울 대상)
     * @param sourceColumn  적재 테이블의 자연 키 컬럼
     * @param fkColumn      채울 FK 컬럼
     * @param refTable      참조 테이블 (serial PK 보유)
     * @param refKeyColumn  참조 테이블의 자연 키 컬럼 (sourceColumn 과 매칭)
     * @param refIdColumn   참조 테이블의 serial PK 컬럼
     * @param naturalKey    UI/리포트 표시용 자연 키 이름 (unmatched 집계에 노출)
     */
    data class NaturalKeyFk(
        val sourceTable: String,
        val sourceColumn: String,
        val fkColumn: String,
        val refTable: String,
        val refKeyColumn: String,
        val refIdColumn: String,
        val naturalKey: String,
    )

    /**
     * 패턴 B — 부모 FK. 부모 자연 키로 부모 신규 serial PK 를 조인.
     *
     * @param sourceTable  자식 테이블
     * @param sourceColumn 자식이 보존한 부모 식별 자연 키 컬럼
     * @param fkColumn     채울 부모 FK 컬럼
     * @param parentTable  부모 테이블
     * @param parentKeyColumn 부모 테이블의 자연 키 컬럼 (sourceColumn 과 매칭)
     * @param parentIdColumn  부모 테이블의 serial PK 컬럼
     */
    data class ParentFk(
        val sourceTable: String,
        val sourceColumn: String,
        val fkColumn: String,
        val parentTable: String,
        val parentKeyColumn: String,
        val parentIdColumn: String,
    )

    private fun employeeFk(sourceTable: String, sourceColumn: String = "employee_code") = NaturalKeyFk(
        sourceTable = sourceTable,
        sourceColumn = sourceColumn,
        fkColumn = "employee_id",
        refTable = "employee",
        refKeyColumn = "employee_code",
        refIdColumn = "employee_id",
        naturalKey = sourceColumn,
    )

    private fun accountFk(sourceTable: String, sourceColumn: String = "account_code") = NaturalKeyFk(
        sourceTable = sourceTable,
        sourceColumn = sourceColumn,
        fkColumn = "account_id",
        refTable = "account",
        refKeyColumn = "external_key",
        refIdColumn = "account_id",
        naturalKey = sourceColumn,
    )

    private fun productFk(sourceTable: String, sourceColumn: String = "product_code") = NaturalKeyFk(
        sourceTable = sourceTable,
        sourceColumn = sourceColumn,
        fkColumn = "product_id",
        refTable = "product",
        refKeyColumn = "product_code",
        refIdColumn = "product_id",
        naturalKey = sourceColumn,
    )

    /**
     * 패턴 A 매핑 — 적재 순서(P1-B §4) 기준 정렬. EmployeeInfo 는 Stage1 적재 시점에 PK resolve 가
     * 끝나므로 (P1-B §6) 본 목록에 없다.
     */
    val NATURAL_KEY_FK: List<NaturalKeyFk> = listOf(
        // education_post.emp_code → employee
        employeeFk("education_post", "emp_code"),
        // education_view_history.emp_code → employee
        employeeFk("education_view_history", "emp_code"),
        // tmp_order
        employeeFk("tmp_order"),
        accountFk("tmp_order"),
        // tmp_order_product
        employeeFk("tmp_order_product"),
        productFk("tmp_order_product"),
        // tmp_claim — account 키 컬럼명이 sap_account_code
        employeeFk("tmp_claim"),
        accountFk("tmp_claim", "sap_account_code"),
        productFk("tmp_claim"),
        // tmp_suggest
        employeeFk("tmp_suggest"),
        accountFk("tmp_suggest"),
        productFk("tmp_suggest"),
        // tmp_onsite — account 키 컬럼명이 sap_account_code
        employeeFk("tmp_onsite"),
        accountFk("tmp_onsite", "sap_account_code"),
        productFk("tmp_onsite"),
        // tmp_promotion — product 키 컬럼명이 promotion_product_code
        employeeFk("tmp_promotion"),
        productFk("tmp_promotion", "promotion_product_code"),
    )

    /**
     * 패턴 B 매핑 — 부모 FK.
     */
    val PARENT_FK: List<ParentFk> = listOf(
        // tmp_order_product → tmp_order (employee_code 1:1, tmp_order PK = tmp_employeecode)
        ParentFk(
            sourceTable = "tmp_order_product",
            sourceColumn = "employee_code",
            fkColumn = "tmp_order_id",
            parentTable = "tmp_order",
            parentKeyColumn = "employee_code",
            parentIdColumn = "tmp_order_id",
        ),
        // education_post_attachment → education_post (edu_id)
        ParentFk(
            sourceTable = "education_post_attachment",
            sourceColumn = "edu_id",
            fkColumn = "education_post_id",
            parentTable = "education_post",
            parentKeyColumn = "edu_id",
            parentIdColumn = "education_post_id",
        ),
        // education_view_history → education_post (edu_id)
        ParentFk(
            sourceTable = "education_view_history",
            sourceColumn = "edu_id",
            fkColumn = "education_post_id",
            parentTable = "education_post",
            parentKeyColumn = "edu_id",
            parentIdColumn = "education_post_id",
        ),
    )
}
