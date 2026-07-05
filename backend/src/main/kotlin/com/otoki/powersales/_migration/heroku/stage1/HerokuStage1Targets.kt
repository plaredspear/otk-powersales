package com.otoki.powersales._migration.heroku.stage1

import com.otoki.powersales.platform.common.entity.EmployeeAdmin
import com.otoki.powersales.platform.common.entity.LoginHistory
import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import com.otoki.powersales.domain.activity.draft.entity.TmpClaim
import com.otoki.powersales.domain.activity.draft.entity.TmpClaimCode
import com.otoki.powersales.domain.activity.draft.entity.TmpOnsite
import com.otoki.powersales.domain.activity.draft.entity.TmpOrder
import com.otoki.powersales.domain.activity.draft.entity.TmpOrderProduct
import com.otoki.powersales.domain.activity.draft.entity.TmpPromotion
import com.otoki.powersales.domain.activity.draft.entity.TmpSuggest
import com.otoki.powersales.domain.support.education.entity.EducationCode
import com.otoki.powersales.domain.support.education.entity.EducationPost
import com.otoki.powersales.domain.support.education.entity.EducationPostAttachment
import com.otoki.powersales.domain.support.education.entity.EducationViewHistory
import com.otoki.powersales.domain.org.employee.entity.EmployeeInfo
import com.otoki.powersales.domain.foundation.product.entity.FavoriteProduct
import com.otoki.powersales.domain.activity.productexpiration.entity.ProductExpiration
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckItem
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import jakarta.persistence.Column
import jakarta.persistence.Table
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Heroku 데이터 마이그레이션 Stage 1 — 적재 target 메타 카탈로그.
 *
 * SF 프레임워크의 [com.otoki.powersales._migration.sf.stage1.Stage1Targets] 가 entity 별
 * `EntityMetadata` 를 손수 하드코딩하는 것과 달리, Heroku 는 각 엔티티의 `@HerokuOnly` +
 * `@HCColumn` 어노테이션을 **리플렉션으로 읽어 적재 메타를 자동 생성**한다 (스펙 #853 §3).
 * 어노테이션 단일 출처(SoT) 라 매핑 표를 별도 유지할 필요가 없다.
 *
 * 메타 추출 규칙 (엔티티당):
 *  - targetName  — 클래스 simpleName (예: `TmpOrder`)
 *  - tableName   — `@Table(name=...)` value
 *  - csvFileName — `@HerokuOnly(value)` (Heroku 원본 테이블명) + `.csv`. CSV 파일명 = Heroku 원본 테이블명 규약
 *  - 컬럼 매핑   — `@HCColumn("<heroku_col>")` 가 부착된 모든 필드(상속 필드 포함) 의
 *                  (`herokuColumn` = @HCColumn value = CSV 헤더, `dbColumn` = @Column name)
 *  - PK / FK     — `@HCColumn` 이 없는 필드 (serial PK, FK `*_id`, 연관 필드) 는 매핑에서 자동 제외.
 *                  → Stage 1 은 자연 키만 채우고 FK `*_id` 는 NULL 적재 (P2-B 가 사후 채움).
 *
 * `ProductSyncBuffer`(`if_product__c`) 는 부모 스펙 §1.1 제외 — 본 카탈로그에 등록하지 않는다.
 *
 * 일괄 실행 순서는 [DEPENDENCY_ORDER] (부모 → 자식). EmployeeInfo 는 employee 적재 후 공유 PK
 * resolve 가 필요하므로 [HEROKU_PK_RESOLVE_TARGETS] 로 별도 표시한다.
 */
object HerokuStage1Targets {

    /**
     * `@HerokuOnly` 부착 엔티티의 리플렉션 메타.
     *
     * @param targetName    엔티티 클래스 simpleName
     * @param tableName     신규 테이블명 (`@Table`)
     * @param csvFileName   Heroku 원본 테이블명 + `.csv`
     * @param columns       CSV 헤더 ↔ 신규 컬럼 매핑 (`@HCColumn` 부착 필드만)
     * @param requiresPkResolve EmployeeInfo 처럼 Stage1 적재 시점에 자연 키 → 공유 PK resolve 가 필요한지
     */
    data class HerokuEntityMeta(
        val targetName: String,
        val schemaName: String,
        val tableName: String,
        val csvFileName: String,
        val columns: List<HerokuColumnMapping>,
        val requiresPkResolve: Boolean,
    )

    /**
     * @param herokuColumn CSV 헤더명 (= `@HCColumn` value = Heroku 원본 컬럼명)
     * @param dbColumn     신규 테이블 컬럼명 (= `@Column` name)
     */
    data class HerokuColumnMapping(
        val herokuColumn: String,
        val dbColumn: String,
    )

    const val SCHEMA_NAME = "powersales"

    /**
     * 적재 대상 엔티티 클래스 — 의존성 순서(부모 → 자식). ProductSyncBuffer 제외 (§1.1).
     *
     * 부모(EducationPost / TmpOrder) 가 자식(EducationPostAttachment / TmpOrderProduct) 보다
     * 먼저 적재되어야 P2-B 패턴 B (부모 자연키 → 자식 FK) resolve 가 성립한다.
     */
    private val TARGET_CLASSES: List<KClass<*>> = listOf(
        EducationCode::class,
        TmpClaimCode::class,
        SafetyCheckItem::class,
        EmployeeAdmin::class,
        EmployeeInfo::class,
        EducationPost::class,
        EducationPostAttachment::class,
        EducationViewHistory::class,
        TmpOrder::class,
        TmpOrderProduct::class,
        TmpClaim::class,
        TmpSuggest::class,
        TmpOnsite::class,
        TmpPromotion::class,
        FavoriteProduct::class,
        LoginHistory::class,
        ProductExpiration::class,
        SafetyCheckSubmission::class,
    )

    /**
     * Stage 1 적재 시점에 자연 키 → 공유 PK resolve 가 필요한 target (employee 적재 선행 필수).
     *
     * EmployeeInfo 는 PK 가 employee 와 공유 PK(`@MapsId`, employee_id) 라 FK 가 곧 PK 다.
     * 다른 패턴 A 케이스는 Stage1 에서 `*_id` NULL 적재 후 P2-B 에서 사후 채우지만,
     * EmployeeInfo 는 employee_id PK NOT NULL 이라 적재 시점에 employee_code → employee_id
     * resolve 가 끝나 있어야 한다 (스펙 P1-B §6).
     */
    val HEROKU_PK_RESOLVE_TARGETS: Set<String> = setOf("EmployeeInfo")

    /**
     * 마이그레이션 적재에서 제외할 (targetName, dbColumn) 집합 — `@HCColumn` 부착 필드라도 skip.
     *
     * `@HCColumn` 은 런타임 HC sync 매핑용 SoT 라 지우지 않고(엔티티 정의는 마이그레이션 전용이 아님),
     * 마이그레이션 관심사인 "제외" 는 여기 마이그레이션 코드에 둔다. 제외 컬럼은 CSV 에 있어도
     * 신규 테이블로 적재하지 않고 NULL 로 남는다.
     *
     * - EmployeeInfo.fcm_token: FCM 디바이스 토큰(PII/보안). 레거시 시점 값은 이전하지 않고,
     *   신규 앱 로그인/토큰 리프레시 때 재등록되게 한다. 만료·재발급 특성상 이전 실익도 없다.
     * - EmployeeInfo.device_uuid: 기기 UUID(PII/보안). 레거시 값은 이전하지 않고, 신규 앱 로그인 시
     *   재등록되게 한다.
     */
    private val EXCLUDED_COLUMNS: Set<Pair<String, String>> = setOf(
        "EmployeeInfo" to "fcm_token",
        "EmployeeInfo" to "device_uuid",
    )

    private val ALL: Map<String, HerokuEntityMeta> =
        TARGET_CLASSES.associate { kClass ->
            val meta = buildMeta(kClass)
            meta.targetName to meta
        }

    private val DEPENDENCY_ORDER: List<String> =
        TARGET_CLASSES.map { it.simpleName!! }

    /**
     * 엔티티 1개의 리플렉션 메타 생성.
     *
     * `@Table` / `@HerokuOnly` 를 클래스에서, `@HCColumn` + `@Column` 을 모든 필드(상속 포함)
     * 에서 추출. `@HCColumn` 없는 필드(PK/FK/연관)는 매핑에서 제외.
     */
    private fun buildMeta(kClass: KClass<*>): HerokuEntityMeta {
        val targetName = kClass.simpleName
            ?: error("anonymous class cannot be a Heroku migration target")

        val herokuOnly = kClass.findAnnotation<HerokuOnly>()
            ?: error("$targetName is missing @HerokuOnly")
        val table = kClass.findAnnotation<Table>()
            ?: error("$targetName is missing @Table")

        val columns = collectColumnMappings(kClass, targetName)
        require(columns.isNotEmpty()) { "$targetName has no @HCColumn-mapped column" }

        return HerokuEntityMeta(
            targetName = targetName,
            schemaName = SCHEMA_NAME,
            tableName = table.name,
            csvFileName = "${herokuOnly.value}.csv",
            columns = columns,
            requiresPkResolve = targetName in HEROKU_PK_RESOLVE_TARGETS,
        )
    }

    /**
     * 클래스 + 모든 상속 부모(BaseEntity 등) 의 `@HCColumn` 부착 필드를 수집해
     * (herokuColumn, dbColumn) 매핑으로 변환. 동일 dbColumn 중복은 자식 우선 1회만 채택.
     *
     * BaseEntity 상속 엔티티는 부모의 createdAt/updatedAt (`@HCColumn("createddate")` /
     * `@HCColumn("lastmodifieddate")`) 도 포함된다.
     *
     * [EXCLUDED_COLUMNS] 에 등록된 (targetName, dbColumn) 는 `@HCColumn` 이 있어도 매핑에서 제외한다
     * (예: EmployeeInfo.fcm_token — PII/보안 미이전).
     */
    private fun collectColumnMappings(kClass: KClass<*>, targetName: String): List<HerokuColumnMapping> {
        val seen = LinkedHashSet<String>() // dbColumn 중복 방지 (insertion order 유지)
        val result = mutableListOf<HerokuColumnMapping>()

        // java reflection 으로 클래스 계층 전체의 declaredFields 순회 (자식 → 부모).
        var javaClass: Class<*>? = kClass.java
        while (javaClass != null && javaClass != Any::class.java) {
            for (field in javaClass.declaredFields) {
                val hc = field.getAnnotation(HCColumn::class.java) ?: continue
                val column = field.getAnnotation(Column::class.java) ?: continue
                val dbColumn = column.name.ifBlank { field.name }
                if (targetName to dbColumn in EXCLUDED_COLUMNS) continue // 마이그레이션 제외 컬럼 skip
                if (!seen.add(dbColumn)) continue
                result.add(HerokuColumnMapping(herokuColumn = hc.value, dbColumn = dbColumn))
            }
            javaClass = javaClass.superclass
        }
        return result
    }

    fun get(targetName: String): HerokuEntityMeta? = ALL[targetName]

    /** 등록된 target 일람 (의존성 순서). */
    fun list(): List<String> = DEPENDENCY_ORDER

    /**
     * 등록된 target 의 (이름, csvFileName) 일람 (의존성 순서). UI dropdown 이 prefix + csvFileName
     * 으로 최종 S3 key 를 미리보기 표시하는 데 사용.
     */
    fun listWithCsv(): List<TargetCsv> =
        DEPENDENCY_ORDER.mapNotNull { name -> ALL[name]?.let { TargetCsv(name, it.csvFileName) } }

    data class TargetCsv(val targetName: String, val csvFileName: String)

    /**
     * `@Table`/`@HCColumn` 어노테이션 단일 출처를 KClass 까지 노출 (verify-metadata 스크립트 /
     * 테스트가 리플렉션 정합을 직접 검증할 때 사용).
     */
    @Suppress("unused")
    fun targetClasses(): List<KClass<*>> = TARGET_CLASSES
}
