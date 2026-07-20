package com.otoki.powersales.external.ovip.auth.audit

object OvipInboundAuditEventType {
    const val TOKEN_ISSUED = "TOKEN_ISSUED"
    const val TOKEN_REJECTED = "TOKEN_REJECTED"
    const val REQUEST_ACCEPTED = "REQUEST_ACCEPTED"
    const val REQUEST_REJECTED_AUTH = "REQUEST_REJECTED_AUTH"
    const val REQUEST_REJECTED_SCOPE = "REQUEST_REJECTED_SCOPE"
    const val REQUEST_REJECTED_SANITY = "REQUEST_REJECTED_SANITY"
}
