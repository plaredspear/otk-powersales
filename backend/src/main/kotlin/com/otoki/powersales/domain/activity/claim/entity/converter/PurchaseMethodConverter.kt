package com.otoki.powersales.domain.activity.claim.entity.converter

import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * SF picklist `DKRetail__PurchaseMethod__c` (label/value 분리) AttributeConverter.
 * DB ↔ enum 양방향. DB 저장값은 SF value (A/B/C).
 */
@Converter
class PurchaseMethodConverter : AttributeConverter<PurchaseMethod, String> {
    override fun convertToDatabaseColumn(attribute: PurchaseMethod?): String? = attribute?.sfValue

    override fun convertToEntityAttribute(dbData: String?): PurchaseMethod? =
        PurchaseMethod.fromSfValueOrNull(dbData)
}
