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
            condW.writeNext(arrayOf("parentDeveloperName", "field", "operator", "value", "conditionOrder", "logicConnector"))
            tgtW.writeNext(arrayOf("parentDeveloperName", "targetType", "targetDeveloperName"))

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
                                    fullName, field, operation, value ?: "",
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
                            tgtW.writeNext(arrayOf(fullName, targetType, targetDevName))
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
println()
println("후속 단계: Stage 1 적재 (migrate-stage1.main.kts) + Stage 2 fk substep (sharing_rule_target.target_sfid 분기)")
