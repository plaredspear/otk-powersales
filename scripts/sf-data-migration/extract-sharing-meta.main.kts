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
 *   3) profiles/<profile>.profile-meta.xml
 *      → input/profile-flags.csv (Profile Name + 5 권한 비트)
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

var profileCount = 0

CSVWriter(PrintWriter(profileFlagsCsv)).use { w ->
    w.writeNext(arrayOf(
        "profileName",
        "permissionsViewAllData", "permissionsModifyAllData", "permissionsViewAllUsers",
        "permissionsManageUsers", "permissionsApiEnabled",
    ))

    if (profilesDir.isDirectory) {
        profilesDir.listFiles { f -> f.name.endsWith(".profile-meta.xml") }?.sortedBy { it.name }?.forEach { file ->
            val profileName = file.name.removeSuffix(".profile-meta.xml")
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
            ))
            profileCount++
        }
    }
}

println("[profile-flags] $profileCount 건 → $profileFlagsCsv")

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
            val permsetName = file.name.removeSuffix(".permissionset-meta.xml")
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
// 7) record-type — RecordType 정의 (spec #794)
//
// XML 출처: objects/<SObject>/recordTypes/<DeveloperName>.recordType-meta.xml
// Master RT 는 적재하지 않음 (Q4 옵션 1 — record_type_id IS NULL 이 곧 Master 의미)
// =============================================================================

val recordTypeCsv = out.resolve("record-type.csv")
var recordTypeCount = 0

CSVWriter(PrintWriter(recordTypeCsv)).use { w ->
    w.writeNext(arrayOf("sObjectName", "developerName", "label", "description", "isActive"))

    if (objectsDir.isDirectory) {
        objectsDir.listFiles { f -> f.isDirectory }?.sortedBy { it.name }?.forEach { dir ->
            val sObjectName = dir.name
            val recordTypesDir = dir.resolve("recordTypes")
            if (recordTypesDir.isDirectory) {
                recordTypesDir.listFiles { f -> f.name.endsWith(".recordType-meta.xml") }?.forEach { file ->
                    try {
                        val root = parseXml(file)
                        val developerName = root.childText("fullName") ?: return@forEach
                        val label = root.childText("label") ?: developerName
                        val description = root.childText("description")
                        val isActive = root.childText("active")?.toBoolean() ?: true
                        w.writeNext(arrayOf(sObjectName, developerName, label, description ?: "", isActive.toString()))
                        recordTypeCount++
                    } catch (_: Exception) {
                        // 깨진 파일 skip
                    }
                }
            }
        }
    }
}
println("[record-type] $recordTypeCount 건 → $recordTypeCsv")

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
            val profileName = file.name.removeSuffix(".profile-meta.xml")
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
            val permsetName = file.name.removeSuffix(".permissionset-meta.xml")
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
            val profileName = file.name.removeSuffix(".profile-meta.xml")
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
            val permsetName = file.name.removeSuffix(".permissionset-meta.xml")
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
println("  record-type.csv           : $recordTypeCount 건  [spec #794]")
println("  profile-record-type.csv   : $profileRecordTypeCount 건  [spec #794]")
println("  permission-set-record-type.csv: $permSetRecordTypeCount 건  [spec #794]")
println("  profile-field-permission.csv: $profileFieldPermissionCount 건  [spec #795]")
println("  permission-set-field-permission.csv: $permSetFieldPermissionCount 건  [spec #795]")
println()
println("후속 단계: Stage 1 적재 (migrate-stage1.main.kts) + Stage 2 fk substep (sharing_rule_target.target_sfid 분기)")
