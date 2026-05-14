package com.otoki.powersales.employee.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Group.Type` picklist enum (Spec #755).
 *
 * SF picklist 17개 옵션값을 그대로 보존. DB 저장 / JSON 직렬화는 [displayName] (SF 원본값).
 * JPA 매핑은 [GroupTypeConverter] 경유.
 */
enum class GroupType(val displayName: String) {
    ALL_CUSTOMER_PORTAL("AllCustomerPortal"),
    CHANNEL_PROGRAM_GROUP("ChannelProgramGroup"),
    COLLABORATION_GROUP("CollaborationGroup"),
    DATA_ANALYTICS("DataAnalytics"),
    MANAGER("Manager"),
    MANAGER_AND_SUBORDINATES_INTERNAL("ManagerAndSubordinatesInternal"),
    ORGANIZATION("Organization"),
    PARTICIPANT("Participant"),
    PRM_ORGANIZATION("PRMOrganization"),
    QUEUE("Queue"),
    REGULAR("Regular"),
    ROLE("Role"),
    ROLE_AND_SUBORDINATES("RoleAndSubordinates"),
    ROLE_AND_SUBORDINATES_INTERNAL("RoleAndSubordinatesInternal"),
    SHARING_RECORD_COLL_GROUP("SharingRecordCollGroup"),
    TERRITORY("Territory"),
    TERRITORY_AND_SUBORDINATES("TerritoryAndSubordinates");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): GroupType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 GroupType: $value")

        fun fromDisplayNameOrNull(value: String?): GroupType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
