package com.otoki.powersales.domain.activity.order.entity

import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * SF picklist `DKRetail__RequestStatus__c` AttributeConverter.
 * DB ↔ enum 양방향. DB 저장값은 SF 한국어 원본 (displayName).
 */
@Converter
class OrderRequestStatusConverter : AttributeConverter<OrderRequestStatus, String> {
    override fun convertToDatabaseColumn(attribute: OrderRequestStatus?): String? = attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): OrderRequestStatus? =
        OrderRequestStatus.fromDisplayNameOrNull(dbData)
}
