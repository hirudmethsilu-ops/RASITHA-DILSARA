package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface PlaidApiService {

    @POST("/link/token/create")
    suspend fun createLinkToken(
        @Body request: PlaidLinkTokenRequest
    ): PlaidLinkTokenResponse

    @POST("/item/public_token/exchange")
    suspend fun exchangePublicToken(
        @Body request: PlaidExchangeTokenRequest
    ): PlaidExchangeTokenResponse

    @POST("/accounts/balance/get")
    suspend fun getBalances(
        @Body request: Map<String, String> // client_id, secret, access_token
    ): PlaidAccountsResponse

    @POST("/transactions/sync")
    suspend fun syncTransactions(
        @Body request: PlaidTransactionsSyncRequest
    ): PlaidTransactionsSyncResponse

    companion object {
        fun create(environment: String): PlaidApiService {
            val baseUrl = when (environment.lowercase()) {
                "production" -> "https://production.plaid.com"
                "development" -> "https://development.plaid.com"
                else -> "https://sandbox.plaid.com"
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(PlaidApiService::class.java)
        }
    }
}
