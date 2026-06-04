#!/usr/bin/env kotlin

/**
 * SF Sharing 정책 메타 import 도구 — spec #782 P1-B.
 *
 * 4 종류 XML 메타 파일을 파싱하여 정책 CSV 5건 출력:
 *   1) sharingRules/<SObject>.sharingRules-meta.xml
 *      → input/sharing-rule.csv
 *      → input/sharing-rule-condition.csv
 *      → input/sharing-rule-target.csv
 *   2) roles/<role>.role-meta.xml
 *      → input/user-role-hierarchy.csv (DeveloperName + parentRole DeveloperName)
 *   3) profiles/<profile>.profile-meta.xml + (선택) profile_object_permissions.csv
 *      → input/profile-flags.csv (Profile Name + 5 권한 비트 + object_permissions JSON)
 *      objectPermissions 는 Profile XML 에 비어 내려오므로 (SF retrieve 가 SObject 동반 없이는
 *      채우지 않음), extract-csv.sh 의 ObjectPermissions SOQL 결과 (profile_object_permissions.csv) 를
 *      Profile 별로 묶어 JSON 컬럼으로 합친다. CSV 부재 시 빈 JSON (XML-only 동작 하위호환).
 *   4) permissionsets/<set>.permissionset-meta.xml
 *      → input/permission-set-flags.csv (PermissionSet Name + 시스템 권한 비트 + object_permissions JSON)
 *
 * 사용법:
 *   kotlinc -script extract-sharing-meta.main.kts -- \
 *       --src-dir <retrieve 루트> \
 *       --out-dir <CSV 출력 디렉토리>
 *
 * 입력 retrieve 루트는 `<root>/force-app/main/default/{sharingRules,roles,profiles,permissionsets}/` 4 폴더를 보유.
 *
 * 책임 분리:
 * - 본 스크립트는 XML → CSV 변환만. sfid resolve (UserRole.DeveloperName → sfid) 은 Stage 1 적재 후 별도 단계.
 * - Stage 2 fk substep (sharing_rule_target.target_sfid → target_id) 은 SfFkResolveTables.kt 의 polymorphic 분기 처리.
 */

@file:DependsOn("com.opencsv:opencsv:5.9")

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.PrintWriter
import javax.xml.parsers.DocumentBuilderFactory

// =============================================================================
// 인자 파싱
// =============================================================================

var srcDir: String? = null
var outDir: String? = null

val argList = args.toList()
var idx = 0
while (idx < argList.size) {
    when (val arg = argList[idx]) {
        "--src-dir" -> { srcDir = argList[idx + 1]; idx += 2 }
        "--out-dir" -> { outDir = argList[idx + 1]; idx += 2 }
        else -> { System.err.println("Unknown arg: $arg"); idx++ }
    }
}

if (srcDir == null || outDir == null) {
    System.err.println("Usage: extract-sharing-meta.main.kts --src-dir <retrieve 루트> --out-dir <CSV 출력>")
    System.err.println("  retrieve 루트는 force-app/main/default/{sharingRules,roles,profiles,permissionsets} 4 폴더를 보유")
    kotlin.system.exitProcess(1)
}

val src = File(srcDir!!).resolve("force-app/main/default")
val out = File(outDir!!)
out.mkdirs()

require(src.isDirectory) { "src-dir 가 retrieve 루트가 아님 — force-app/main/default 부재: ${src.absolutePath}" }

// =============================================================================
// XML 유틸
// =============================================================================

val docFactory = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = false
}

fun parseXml(file: File): Element {
    val doc = docFactory.newDocumentBuilder().parse(file)
    return doc.documentElement
}

fun NodeList.elements(): Sequence<Element> = sequence {
    for (i in 0 until length) {
        val node = item(i)
        if (node.nodeType == Node.ELEMENT_NODE) yield(node as Element)
    }
}

fun Element.childElements(name: String): Sequence<Element> =
    getElementsByTagName(name).elements().filter { it.parentNode == this }

fun Element.childText(name: String): String? =
    childElements(name).firstOrNull()?.textContent?.takeIf { it.isNotBlank() }

// =============================================================================
// 자연 키 디코딩
// =============================================================================

// SF retrieve 는 메타데이터 파일명의 특수문자를 URL 인코딩한다 (예: '6.조장' → '6%2E조장',
// '마케팅(super)' → '마케팅%28super%29', 'A + B' → 'A %2B B'). 파일명에서 추출한 자연 키
// (profileName / permissionSetName 등) 는 디코딩해야 SOQL 출처의 평문 Name (profile.name,
// permission_set.name) 과 Stage2 fk resolve 에서 정확 일치 매칭된다. 디코딩 누락 시 '.' 등 특수문자
// 포함 Profile (1.본부장 ~ 12.마케팅, 6.조장 등) 의 profile_id resolve 가 전량 실패한다.
fun decodeMetaName(fileBaseName: String): String =
    try {
        java.net.URLDecoder.decode(fileBaseName, "UTF-8")
    } catch (_: Exception) {
        fileBaseName
    }

// =============================================================================
// 1) sharingRules — sharing-rule.csv + sharing-rule-condition.csv + sharing-rule-target.csv
// =============================================================================

val sharingRulesDir = src.resolve("sharingRules")
val sharingRuleCsv = out.resolve("sharing-rule.csv")
val sharingRuleConditionCsv = out.resolve("sharing-rule-condition.csv")
val sharingRuleTargetCsv = out.resolve("sharing-rule-target.csv")

var sharingRuleCount = 0
var sharingRuleConditionCount = 0
var sharingRuleTargetCount = 0

CSVWriter(PrintWriter(sharingRuleCsv)).use { ruleW ->
    CSVWriter(PrintWriter(sharingRuleConditionCsv)).use { condW ->
        CSVWriter(PrintWriter(sharingRuleTargetCsv)).use { tgtW ->

            ruleW.writeNext(arrayOf("developerName", "sObjectName", "ruleType", "label", "accessLevel", "includeOwnedByAll"))
            condW.writeNext(arrayOf("sObjectName", "parentDeveloperName", "field", "operator", "value", "conditionOrder", "logicConnector"))
            tgtW.writeNext(arrayOf("sObjectName", "parentDeveloperName", "targetType", "targetDeveloperName"))

            if (sharingRulesDir.isDirectory) {
                sharingRulesDir.listFiles { f -> f.name.endsWith(".sharingRules-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
                    val sObjectName = file.name.removeSuffix(".sharingRules-meta.xml")
                    val root = parseXml(file)

                    fun processRule(elem: Element, ruleType: String) {
                        val fullName = elem.childText("fullName") ?: return
                        val accessLevel = elem.childText("accessLevel") ?: "Read"
                        val label = elem.childText("label")
                        val includeOwnedByAll = elem.childText("includeRecordsOwnedByAll")?.toBoolean() ?: false

                        ruleW.writeNext(arrayOf(
                            fullName, sObjectName, ruleType, label ?: "", accessLevel, includeOwnedByAll.toString(),
                        ))
                        sharingRuleCount++

                        // 조건 (CRITERIA 만)
                        if (ruleType == "CRITERIA") {
                            elem.childElements("criteriaItems").forEachIndexed { order, item ->
                                val field = item.childText("field") ?: return@forEachIndexed
                                val operation = item.childText("operation") ?: return@forEachIndexed
                                val value = item.childText("value")
                                condW.writeNext(arrayOf(
                                    sObjectName, fullName, field, operation, value ?: "",
                                    (order + 1).toString(), elem.childText("booleanFilter"),
                                ))
                                sharingRuleConditionCount++
                            }
                        }

                        // 대상 — <sharedTo> 내부 element 1건
                        val sharedTo = elem.childElements("sharedTo").firstOrNull() ?: return
                        sharedTo.childNodes.elements().firstOrNull()?.let { targetElem ->
                            val (targetType, targetDevName) = when (targetElem.tagName) {
                                "role" -> "ROLE" to targetElem.textContent
                                "roleAndSubordinates" -> "ROLE_AND_SUBORDINATES" to targetElem.textContent
                                "roleAndSubordinatesInternal" -> "ROLE_AND_SUBORDINATES_INTERNAL" to targetElem.textContent
                                "group" -> "GROUP" to targetElem.textContent
                                "allInternalUsers" -> "ALL_INTERNAL_USERS" to ""
                                else -> return@let
                            }
                            tgtW.writeNext(arrayOf(sObjectName, fullName, targetType, targetDevName))
                            sharingRuleTargetCount++
                        }
                    }

                    root.childElements("sharingCriteriaRules").forEach { processRule(it, "CRITERIA") }
                    root.childElements("sharingOwnerRules").forEach { processRule(it, "OWNER") }
                }
            }
        }
    }
}

println("[sharing-rule] $sharingRuleCount 건 / [condition] $sharingRuleConditionCount 건 / [target] $sharingRuleTargetCount 건 → $sharingRuleCsv")

// =============================================================================
// 2) roles — user-role-hierarchy.csv (parent DeveloperName 트리)
// =============================================================================

val rolesDir = src.resolve("roles")
val roleHierarchyCsv = out.resolve("user-role-hierarchy.csv")

var roleCount = 0

CSVWriter(PrintWriter(roleHierarchyCsv)).use { w ->
    w.writeNext(arrayOf("developerName", "name", "parentDeveloperName", "description", "mayForecastManagerShare"))

    if (rolesDir.isDirectory) {
        rolesDir.listFiles { f -> f.name.endsWith(".role-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val developerName = file.name.removeSuffix(".role-meta.xml")
            val root = parseXml(file)

            w.writeNext(arrayOf(
                developerName,
                root.childText("name") ?: "",
                root.childText("parentRole") ?: "",
                root.childText("description") ?: "",
                root.childText("mayForecastManagerShare") ?: "false",
            ))
            roleCount++
        }
    }
}

println("[user-role-hierarchy] $roleCount 건 → $roleHierarchyCsv")

// =============================================================================
// 3) profiles — profile-flags.csv (5 권한 비트)
// =============================================================================

val profilesDir = src.resolve("profiles")
val profileFlagsCsv = out.resolve("profile-flags.csv")

val PROFILE_PERMISSIONS_OF_INTEREST = listOf(
    "ViewAllData", "ModifyAllData", "ViewAllUsers", "ManageUsers", "ApiEnabled",
)

// profile_object_permissions.csv (extract-csv.sh ObjectPermissions SOQL 출력) 를 Profile 이름별로 묶는다.
// 헤더: Parent.Profile.Name, SobjectType, PermissionsRead, PermissionsCreate, PermissionsEdit,
//       PermissionsDelete, PermissionsViewAllRecords, PermissionsModifyAllRecords.
// 결과: profileName → objectPermissions JSON (PermissionSet XML 직렬화와 동일 구조/키).
// CSV 부재 시 빈 맵 — XML-only 하위호환 (모든 Profile 의 object_permissions 가 "{}").
fun loadProfileObjectPermissions(): Map<String, String> {
    val csv = out.resolve("profile_object_permissions.csv")
    if (!csv.isFile) {
        println("[profile-flags] profile_object_permissions.csv 부재 — object_permissions 는 빈 JSON 으로 적재")
        return emptyMap()
    }
    // profileName → (SobjectType → 6 비트 맵)
    val byProfile = LinkedHashMap<String, LinkedHashMap<String, Map<String, Boolean>>>()
    CSVReader(csv.reader()).use { r ->
        val header = r.readNext() ?: return emptyMap()
        val col = header.withIndex().associate { (i, h) -> h.trim() to i }
        fun idx(vararg names: String): Int = names.firstNotNullOfOrNull { col[it] } ?: -1
        val cProfile = idx("Parent.Profile.Name", "Profile.Name")
        val cObject = idx("SobjectType")
        val cRead = idx("PermissionsRead")
        val cCreate = idx("PermissionsCreate")
        val cEdit = idx("PermissionsEdit")
        val cDelete = idx("PermissionsDelete")
        val cViewAll = idx("PermissionsViewAllRecords")
        val cModifyAll = idx("PermissionsModifyAllRecords")
        require(cProfile >= 0 && cObject >= 0) {
            "profile_object_permissions.csv 헤더에 Parent.Profile.Name / SobjectType 부재: ${header.toList()}"
        }
        fun cell(row: Array<String>, i: Int): Boolean =
            i >= 0 && i < row.size && row[i].trim().equals("true", ignoreCase = true)
        var row = r.readNext()
        while (row != null) {
            val profileName = row.getOrNull(cProfile)?.trim().orEmpty()
            val sobject = row.getOrNull(cObject)?.trim().orEmpty()
            if (profileName.isNotEmpty() && sobject.isNotEmpty()) {
                byProfile.getOrPut(profileName) { LinkedHashMap() }[sobject] = linkedMapOf(
                    "viewAllRecords" to cell(row, cViewAll),
                    "modifyAllRecords" to cell(row, cModifyAll),
                    "allowRead" to cell(row, cRead),
                    "allowCreate" to cell(row, cCreate),
                    "allowEdit" to cell(row, cEdit),
                    "allowDelete" to cell(row, cDelete),
                )
            }
            row = r.readNext()
        }
    }
    // SObject → 6비트 맵을 JSON 으로 직렬화 (PermissionSet 블록과 동일 형식).
    return byProfile.mapValues { (_, objects) ->
        val sb = StringBuilder("{")
        objects.entries.forEachIndexed { oi, (obj, perms) ->
            if (oi > 0) sb.append(",")
            sb.append("\"").append(obj).append("\":{")
            perms.entries.forEachIndexed { pi, (k, v) ->
                if (pi > 0) sb.append(",")
                sb.append("\"").append(k).append("\":").append(v)
            }
            sb.append("}")
        }
        sb.append("}")
        sb.toString()
    }
}

val profileObjectPermissions = loadProfileObjectPermissions()
var profileCount = 0

CSVWriter(PrintWriter(profileFlagsCsv)).use { w ->
    w.writeNext(arrayOf(
        "profileName",
        "permissionsViewAllData", "permissionsModifyAllData", "permissionsViewAllUsers",
        "permissionsManageUsers", "permissionsApiEnabled",
        "objectPermissionsJson",
    ))

    if (profilesDir.isDirectory) {
        profilesDir.listFiles { f -> f.name.endsWith(".profile-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val profileName = decodeMetaName(file.name.removeSuffix(".profile-meta.xml"))
            val root = parseXml(file)

            val flags = PROFILE_PERMISSIONS_OF_INTEREST.map { permName -> permName to false }.toMap().toMutableMap()
            root.childElements("userPermissions").forEach { perm ->
                val name = perm.childText("name") ?: return@forEach
                val enabled = perm.childText("enabled")?.toBoolean() ?: false
                if (name in flags) flags[name] = enabled
            }

            w.writeNext(arrayOf(
                profileName,
                flags["ViewAllData"].toString(),
                flags["ModifyAllData"].toString(),
                flags["ViewAllUsers"].toString(),
                flags["ManageUsers"].toString(),
                flags["ApiEnabled"].toString(),
                profileObjectPermissions[profileName] ?: "{}",
            ))
            profileCount++
        }
    }
}

val profileObjPermCount = profileObjectPermissions.size
println("[profile-flags] $profileCount 건 → $profileFlagsCsv (object_permissions 보유 Profile $profileObjPermCount 건)")

// =============================================================================
// 4) permissionsets — permission-set-flags.csv (시스템 권한 비트 + object_permissions JSON)
// =============================================================================

val permsetsDir = src.resolve("permissionsets")
val permsetFlagsCsv = out.resolve("permission-set-flags.csv")

var permsetCount = 0

CSVWriter(PrintWriter(permsetFlagsCsv)).use { w ->
    w.writeNext(arrayOf(
        "permissionSetName", "permissionsViewAllData", "permissionsModifyAllData", "objectPermissionsJson",
    ))

    if (permsetsDir.isDirectory) {
        permsetsDir.listFiles { f -> f.name.endsWith(".permissionset-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val permsetName = decodeMetaName(file.name.removeSuffix(".permissionset-meta.xml"))
            val root = parseXml(file)

            var viewAllData = false
            var modifyAllData = false
            root.childElements("userPermissions").forEach { perm ->
                val name = perm.childText("name") ?: return@forEach
                val enabled = perm.childText("enabled")?.toBoolean() ?: false
                if (name == "ViewAllData") viewAllData = enabled
                if (name == "ModifyAllData") modifyAllData = enabled
            }

            // objectPermissions 를 단순 JSON 으로 직렬화 — { "SObject": { "viewAllRecords": true, "modifyAllRecords": false, "read": true, "create": ..., "edit": ..., "delete": ... }, ... }
            val sb = StringBuilder("{")
            var first = true
            root.childElements("objectPermissions").forEach { op ->
                val obj = op.childText("object") ?: return@forEach
                if (!first) sb.append(",")
                first = false
                sb.append("\"").append(obj).append("\":{")
                listOf(
                    "viewAllRecords" to op.childText("viewAllRecords"),
                    "modifyAllRecords" to op.childText("modifyAllRecords"),
                    "allowRead" to op.childText("allowRead"),
                    "allowCreate" to op.childText("allowCreate"),
                    "allowEdit" to op.childText("allowEdit"),
                    "allowDelete" to op.childText("allowDelete"),
                ).filter { it.second != null }
                    .forEachIndexed { i, (k, v) ->
                        if (i > 0) sb.append(",")
                        sb.append("\"").append(k).append("\":").append(v?.toBoolean() ?: false)
                    }
                sb.append("}")
            }
            sb.append("}")

            w.writeNext(arrayOf(
                permsetName,
                viewAllData.toString(),
                modifyAllData.toString(),
                sb.toString(),
            ))
            permsetCount++
        }
    }
}

println("[permission-set-flags] $permsetCount 건 → $permsetFlagsCsv")

// =============================================================================
// 5) sobject-setting — OWD + hierarchy 옵트인 (spec #791)
//
// XML 메타 3 출처 정규화:
//   - Custom SObject: objects/<Name>__c/<Name>__c.object-meta.xml 의 <sharingModel>
//   - Standard SObject: settings/Sharing.settings-meta.xml 의 <sharingSettings>
//   - Hierarchy 옵트인: settings/Sharing.settings-meta.xml 의 <sharingHierarchy>
// =============================================================================

val objectsDir = src.resolve("objects")
val sharingSettingsFile = src.resolve("settings/Sharing.settings-meta.xml")
val sObjectSettingCsv = out.resolve("sobject-setting.csv")

// Custom SObject 의 <sharingModel> 1차 수집 — sObjectName → owd
val customOwd = mutableMapOf<String, String>()
if (objectsDir.isDirectory) {
    objectsDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
        val sObjectName = dir.name
        val metaFile = dir.resolve("$sObjectName.object-meta.xml")
        if (metaFile.isFile) {
            try {
                val root = parseXml(metaFile)
                val owd = root.childText("sharingModel")
                if (owd != null) customOwd[sObjectName] = owd
            } catch (_: Exception) {
                // 깨진 파일 skip
            }
        }
    }
}

// Standard SObject — Sharing.settings-meta.xml 의 <sharingSettings> 블록
val standardOwd = mutableMapOf<String, String>()
val hierarchyGrant = mutableMapOf<String, Boolean>()
if (sharingSettingsFile.isFile) {
    val root = parseXml(sharingSettingsFile)
    root.childElements("sharingSettings").forEach { ss ->
        val obj = ss.childText("object") ?: return@forEach
        val owd = ss.childText("internalSharingModel") ?: return@forEach
        standardOwd[obj] = owd
    }
    root.childElements("sharingHierarchy").forEach { sh ->
        val obj = sh.childText("object") ?: return@forEach
        val grant = sh.childText("grantAccessUsingHierarchies")?.toBoolean() ?: true
        hierarchyGrant[obj] = grant
    }
}

// 합집합 출력 — Custom + Standard
var sObjectSettingCount = 0
CSVWriter(PrintWriter(sObjectSettingCsv)).use { w ->
    w.writeNext(arrayOf("sObjectName", "orgWideDefault", "allowHierarchyGrant", "parentSObjectName"))
    val all = (customOwd.keys + standardOwd.keys).sorted()
    all.forEach { name ->
        val owd = customOwd[name] ?: standardOwd[name] ?: "Private"
        val grant = hierarchyGrant[name] ?: true
        w.writeNext(arrayOf(name, owd, grant.toString(), ""))
        sObjectSettingCount++
    }
}
println("[sobject-setting] $sObjectSettingCount 건 → $sObjectSettingCsv")

// =============================================================================
// 6) sobject-relation — master-detail relationship (spec #791 Q2 옵션 1)
//
// objects/<SObject>/fields/<Field>__c.field-meta.xml 의 <type>MasterDetail</type> + <referenceTo>
// =============================================================================

val sObjectRelationCsv = out.resolve("sobject-relation.csv")

var sObjectRelationCount = 0
CSVWriter(PrintWriter(sObjectRelationCsv)).use { w ->
    w.writeNext(arrayOf("childSObjectName", "parentSObjectName", "relationFieldName", "isMasterDetail"))

    if (objectsDir.isDirectory) {
        objectsDir.listFiles { f -> f.isDirectory }?.sortedBy { it.name }?.forEach { dir ->
            val childName = dir.name
            val fieldsDir = dir.resolve("fields")
            if (fieldsDir.isDirectory) {
                fieldsDir.listFiles { f -> f.name.endsWith(".field-meta.xml") }?.forEach { fieldFile ->
                    try {
                        val root = parseXml(fieldFile)
                        val type = root.childText("type") ?: return@forEach
                        if (type != "MasterDetail") return@forEach
                        val fullName = root.childText("fullName") ?: return@forEach
                        val referenceTo = root.childText("referenceTo") ?: return@forEach
                        w.writeNext(arrayOf(childName, referenceTo, fullName, "true"))
                        sObjectRelationCount++
                    } catch (_: Exception) {
                        // 깨진 파일 skip
                    }
                }
            }
        }
    }
}
println("[sobject-relation] $sObjectRelationCount 건 → $sObjectRelationCsv")

// =============================================================================
// 6-b) branch-mapping — cost_center_code 이력 합집합 매핑 (BranchMapping__mdt)
//
// Custom Metadata Type: customMetadata/BranchMapping.<DeveloperName>.md-meta.xml
//   <label> + <values><field>BranchCode__c</field><value>...</value></values>
//                     + <values><field>IncludedBranchCode__c</field><value>...</value></values>
//
// 신규 branch_mapping 테이블 (PK = branch_code) 로 Stage1 적재. 직전엔 BranchMappingMatrix
// Kotlin object 박제 + 부팅 ApplicationRunner sync 였으나, SharingRule/SObjectSetting 등 다른
// XML 메타와 동일하게 CSV 적재 경로로 일원화 (코드 박제 제거 + 매 부팅 sync 로그 제거).
// =============================================================================

val customMetadataDir = src.resolve("customMetadata")
val branchMappingCsv = out.resolve("branch-mapping.csv")

// CustomMetadata 의 <values><field>X</field><value>Y</value></values> → field X 의 value Y.
fun Element.customMetadataValue(field: String): String? =
    childElements("values")
        .firstOrNull { it.childText("field") == field }
        ?.childText("value")

var branchMappingCount = 0
CSVWriter(PrintWriter(branchMappingCsv)).use { w ->
    w.writeNext(arrayOf("branchCode", "includedBranchCodes", "label"))
    if (customMetadataDir.isDirectory) {
        customMetadataDir
            .listFiles { f -> f.name.startsWith("BranchMapping.") && f.name.endsWith(".md-meta.xml") }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                try {
                    val root = parseXml(file)
                    val branchCode = root.customMetadataValue("BranchCode__c") ?: return@forEach
                    val includedBranchCodes = root.customMetadataValue("IncludedBranchCode__c") ?: return@forEach
                    val label = root.childText("label") ?: ""
                    w.writeNext(arrayOf(branchCode, includedBranchCodes, label))
                    branchMappingCount++
                } catch (_: Exception) {
                    // 깨진 파일 skip
                }
            }
    }
}
println("[branch-mapping] $branchMappingCount 건 → $branchMappingCsv")

// =============================================================================
// 7) record-type — extract-csv.sh 의 RECORD_TYPE_SOQL 로 이관됨.
//
// 기존엔 XML 출처(objects/<SObject>/recordTypes/<DeveloperName>.recordType-meta.xml)로
// record-type.csv 를 만들었으나 XML 에는 18자리 RecordTypeId 가 없어 record_type.sfid 가 NULL →
// SObject row 의 record_type_sfid → record_type_id FK resolve(sfid 매칭)가 전량 미해소였다.
// record-type.csv 는 이제 extract-csv.sh 가 `SELECT Id, SobjectType, DeveloperName, Name,
// Description, IsActive FROM RecordType` 으로 생성한다 (Id=sfid 포함). 자연키 (SobjectType,
// DeveloperName) 는 XML 출처와 동일해 아래 profile-record-type / permission-set-record-type 의
// 자연키 join 정합은 유지된다. Master RT 도 SOQL 에 포함되나 record_type_id 매칭에만 쓰여 무해.
// =============================================================================

// =============================================================================
// 8) profile-record-type — Profile × RecordType visibility (spec #794)
//
// XML 출처: profiles/<Name>.profile-meta.xml 의 <recordTypeVisibilities>
// 운영 0건 (Profile 위임 패턴 — PermissionSet 위임)
// =============================================================================

val profileRecordTypeCsv = out.resolve("profile-record-type.csv")
var profileRecordTypeCount = 0

CSVWriter(PrintWriter(profileRecordTypeCsv)).use { w ->
    w.writeNext(arrayOf("profileName", "sObjectName", "recordTypeDeveloperName", "visible", "isDefault"))

    if (profilesDir.isDirectory) {
        profilesDir.listFiles { f -> f.name.endsWith(".profile-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val profileName = decodeMetaName(file.name.removeSuffix(".profile-meta.xml"))
            try {
                val root = parseXml(file)
                root.childElements("recordTypeVisibilities").forEach { rtv ->
                    // <recordType> 값은 "SObject.DeveloperName" 형식 — split 으로 분리
                    val recordTypeFull = rtv.childText("recordType") ?: return@forEach
                    val parts = recordTypeFull.split(".", limit = 2)
                    if (parts.size != 2) return@forEach
                    val sObjectName = parts[0]
                    val rtDevName = parts[1]
                    val visible = rtv.childText("visible")?.toBoolean() ?: false
                    val isDefault = rtv.childText("default")?.toBoolean() ?: false
                    w.writeNext(arrayOf(profileName, sObjectName, rtDevName, visible.toString(), isDefault.toString()))
                    profileRecordTypeCount++
                }
            } catch (_: Exception) {
                // 깨진 파일 skip
            }
        }
    }
}
println("[profile-record-type] $profileRecordTypeCount 건 → $profileRecordTypeCsv")

// =============================================================================
// 9) permission-set-record-type — PermissionSet × RecordType visibility (spec #794)
// =============================================================================

val permSetRecordTypeCsv = out.resolve("permission-set-record-type.csv")
var permSetRecordTypeCount = 0

CSVWriter(PrintWriter(permSetRecordTypeCsv)).use { w ->
    w.writeNext(arrayOf("permissionSetName", "sObjectName", "recordTypeDeveloperName", "visible", "isDefault"))

    if (permsetsDir.isDirectory) {
        permsetsDir.listFiles { f -> f.name.endsWith(".permissionset-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val permsetName = decodeMetaName(file.name.removeSuffix(".permissionset-meta.xml"))
            try {
                val root = parseXml(file)
                root.childElements("recordTypeVisibilities").forEach { rtv ->
                    val recordTypeFull = rtv.childText("recordType") ?: return@forEach
                    val parts = recordTypeFull.split(".", limit = 2)
                    if (parts.size != 2) return@forEach
                    val sObjectName = parts[0]
                    val rtDevName = parts[1]
                    val visible = rtv.childText("visible")?.toBoolean() ?: false
                    val isDefault = rtv.childText("default")?.toBoolean() ?: false
                    w.writeNext(arrayOf(permsetName, sObjectName, rtDevName, visible.toString(), isDefault.toString()))
                    permSetRecordTypeCount++
                }
            } catch (_: Exception) {
                // 깨진 파일 skip
            }
        }
    }
}
println("[permission-set-record-type] $permSetRecordTypeCount 건 → $permSetRecordTypeCsv")

// =============================================================================
// 10) profile-field-permission — Profile × Field FLS (spec #795)
//
// XML 출처: profiles/<Name>.profile-meta.xml 의 <fieldPermissions>
// 운영 0건 (Profile 위임 패턴 — PermissionSet 측 사용)
// =============================================================================

val profileFieldPermissionCsv = out.resolve("profile-field-permission.csv")
var profileFieldPermissionCount = 0

CSVWriter(PrintWriter(profileFieldPermissionCsv)).use { w ->
    w.writeNext(arrayOf("profileName", "sObjectName", "fieldName", "readable", "editable"))

    if (profilesDir.isDirectory) {
        profilesDir.listFiles { f -> f.name.endsWith(".profile-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val profileName = decodeMetaName(file.name.removeSuffix(".profile-meta.xml"))
            try {
                val root = parseXml(file)
                root.childElements("fieldPermissions").forEach { fp ->
                    // <field> 값은 "SObject.FieldApiName" 형식
                    val fieldFull = fp.childText("field") ?: return@forEach
                    val parts = fieldFull.split(".", limit = 2)
                    if (parts.size != 2) return@forEach
                    val sObjectName = parts[0]
                    val fieldName = parts[1]
                    val readable = fp.childText("readable")?.toBoolean() ?: false
                    val editable = fp.childText("editable")?.toBoolean() ?: false
                    w.writeNext(arrayOf(profileName, sObjectName, fieldName, readable.toString(), editable.toString()))
                    profileFieldPermissionCount++
                }
            } catch (_: Exception) {
                // 깨진 파일 skip
            }
        }
    }
}
println("[profile-field-permission] $profileFieldPermissionCount 건 → $profileFieldPermissionCsv")

// =============================================================================
// 11) permission-set-field-permission — PermissionSet × Field FLS (spec #795)
// =============================================================================

val permSetFieldPermissionCsv = out.resolve("permission-set-field-permission.csv")
var permSetFieldPermissionCount = 0

CSVWriter(PrintWriter(permSetFieldPermissionCsv)).use { w ->
    w.writeNext(arrayOf("permissionSetName", "sObjectName", "fieldName", "readable", "editable"))

    if (permsetsDir.isDirectory) {
        permsetsDir.listFiles { f -> f.name.endsWith(".permissionset-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val permsetName = decodeMetaName(file.name.removeSuffix(".permissionset-meta.xml"))
            try {
                val root = parseXml(file)
                root.childElements("fieldPermissions").forEach { fp ->
                    val fieldFull = fp.childText("field") ?: return@forEach
                    val parts = fieldFull.split(".", limit = 2)
                    if (parts.size != 2) return@forEach
                    val sObjectName = parts[0]
                    val fieldName = parts[1]
                    val readable = fp.childText("readable")?.toBoolean() ?: false
                    val editable = fp.childText("editable")?.toBoolean() ?: false
                    w.writeNext(arrayOf(permsetName, sObjectName, fieldName, readable.toString(), editable.toString()))
                    permSetFieldPermissionCount++
                }
            } catch (_: Exception) {
                // 깨진 파일 skip
            }
        }
    }
}
println("[permission-set-field-permission] $permSetFieldPermissionCount 건 → $permSetFieldPermissionCsv")

// =============================================================================
// 요약
// =============================================================================

println()
println("=== extract-sharing-meta 완료 ===")
println("  sharing-rule.csv          : $sharingRuleCount 건")
println("  sharing-rule-condition.csv: $sharingRuleConditionCount 건")
println("  sharing-rule-target.csv   : $sharingRuleTargetCount 건")
println("  user-role-hierarchy.csv   : $roleCount 건")
println("  profile-flags.csv         : $profileCount 건")
println("  permission-set-flags.csv  : $permsetCount 건")
println("  sobject-setting.csv       : $sObjectSettingCount 건  [spec #791]")
println("  sobject-relation.csv      : $sObjectRelationCount 건  [spec #791]")
println("  branch-mapping.csv        : $branchMappingCount 건  [BranchMapping__mdt]")
println("  record-type.csv           : (extract-csv.sh RECORD_TYPE_SOQL 로 이관)  [spec #794]")
println("  profile-record-type.csv   : $profileRecordTypeCount 건  [spec #794]")
println("  permission-set-record-type.csv: $permSetRecordTypeCount 건  [spec #794]")
println("  profile-field-permission.csv: $profileFieldPermissionCount 건  [spec #795]")
println("  permission-set-field-permission.csv: $permSetFieldPermissionCount 건  [spec #795]")
println()
println("후속 단계: Stage 1 적재 (migrate-stage1.main.kts) + Stage 2 fk substep (sharing_rule_target.target_sfid 분기)")
