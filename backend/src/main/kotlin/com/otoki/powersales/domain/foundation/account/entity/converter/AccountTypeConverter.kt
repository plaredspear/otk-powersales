package com.otoki.powersales.domain.foundation.account.entity.converter

import com.otoki.powersales.domain.foundation.account.entity.AccountType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class AccountTypeConverter : AttributeConverter<AccountType, String> {

    override fun convertToDatabaseColumn(attribute: AccountType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): AccountType? =
        AccountType.Companion.fromDisplayNameOrNull(dbData)
}
