package com.otoki.internal.sap.dto

data class SapOrganizeMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val ccCd2: String? = null,
        val orgCd2: String? = null,
        val orgNm2: String? = null,
        val ccCd3: String? = null,
        val orgCd3: String? = null,
        val orgNm3: String? = null,
        val ccCd4: String? = null,
        val orgCd4: String? = null,
        val orgNm4: String? = null,
        val ccCd5: String? = null,
        val orgCd5: String? = null,
        val orgNm5: String? = null
    )
}
