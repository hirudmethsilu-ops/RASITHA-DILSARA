package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlaidUser(
    @Json(name = "client_user_id") val clientUserId: String
)

@JsonClass(generateAdapter = true)
data class PlaidLinkTokenRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "client_name") val clientName: String = "Budget Sync",
    @Json(name = "user") val user: PlaidUser,
    @Json(name = "products") val products: List<String> = listOf("transactions"),
    @Json(name = "country_codes") val countryCodes: List<String> = listOf("US"),
    @Json(name = "language") val language: String = "en"
)

@JsonClass(generateAdapter = true)
data class PlaidLinkTokenResponse(
    @Json(name = "link_token") val linkToken: String,
    @Json(name = "expiration") val expiration: String,
    @Json(name = "request_id") val requestId: String
)

@JsonClass(generateAdapter = true)
data class PlaidExchangeTokenRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "public_token") val publicToken: String
)

@JsonClass(generateAdapter = true)
data class PlaidExchangeTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "item_id") val itemId: String,
    @Json(name = "request_id") val requestId: String
)

@JsonClass(generateAdapter = true)
data class PlaidBalance(
    @Json(name = "available") val available: Double?,
    @Json(name = "current") val current: Double,
    @Json(name = "iso_currency_code") val isoCurrencyCode: String?
)

@JsonClass(generateAdapter = true)
data class PlaidAccount(
    @Json(name = "account_id") val accountId: String,
    @Json(name = "name") val name: String,
    @Json(name = "mask") val mask: String?,
    @Json(name = "type") val type: String,
    @Json(name = "subtype") val subtype: String?,
    @Json(name = "balances") val balances: PlaidBalance
)

@JsonClass(generateAdapter = true)
data class PlaidAccountsResponse(
    @Json(name = "accounts") val accounts: List<PlaidAccount>,
    @Json(name = "request_id") val requestId: String
)

@JsonClass(generateAdapter = true)
data class PlaidTransaction(
    @Json(name = "transaction_id") val transactionId: String,
    @Json(name = "account_id") val accountId: String,
    @Json(name = "amount") val amount: Double, // Note: positive in Plaid is EXPENSE, negative is INCOME/DEPOSIT
    @Json(name = "name") val name: String,
    @Json(name = "date") val date: String,     // YYYY-MM-DD
    @Json(name = "category") val category: List<String>?,
    @Json(name = "pending") val pending: Boolean
)

@JsonClass(generateAdapter = true)
data class PlaidTransactionsSyncRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "cursor") val cursor: String? = null,
    @Json(name = "count") val count: Int = 100
)

@JsonClass(generateAdapter = true)
data class PlaidTransactionsSyncResponse(
    @Json(name = "added") val added: List<PlaidTransaction>,
    @Json(name = "modified") val modified: List<PlaidTransaction>,
    @Json(name = "removed") val removed: List<Map<String, String>>,
    @Json(name = "next_cursor") val nextCursor: String,
    @Json(name = "has_more") val hasMore: Boolean
)
