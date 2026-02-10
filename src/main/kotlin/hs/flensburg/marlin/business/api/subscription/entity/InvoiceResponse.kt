package hs.flensburg.marlin.business.api.subscription.entity

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceResponse(
    val id: String,
    val amountDue: Long,
    val amountPaid: Long,
    val currency: String,
    val status: String?,
    val invoicePdf: String?,
    val created: Long,
    val periodStart: Long,
    val periodEnd: Long
)
