package com.example.data.repository

import com.example.data.remote.SupabaseClient
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class DepositRow(
    @Json(name = "id") val id: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "status") val status: String,
    @Json(name = "gateway") val gateway: String = "zapupi"
)

@JsonClass(generateAdapter = true)
data class WalletInfo(
    @Json(name = "wallet_balance") val walletBalance: Double = 0.0,
    @Json(name = "locked_balance") val lockedBalance: Double = 0.0,
    @Json(name = "total_deposited") val totalDeposited: Double = 0.0,
    @Json(name = "total_winnings") val totalWinnings: Double = 0.0,
    @Json(name = "total_withdrawn") val totalWithdrawn: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DepositStatusRow(
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "verified_at") val verifiedAt: String? = null
)

data class CheckoutResult(
    val orderId: String,
    val checkoutUrl: String,
    val depositId: String
)

sealed class DepositStatus {
    object Pending : DepositStatus()
    object Failed : DepositStatus()
    object NotFound : DepositStatus()
    data class Success(val amount: Double) : DepositStatus()
}

class DepositRepository {

    suspend fun createDepositOrder(amount: Double): CheckoutResult = withContext(Dispatchers.IO) {
        // 1. Validation
        if (amount < 10) {
            throw Exception("Minimum deposit is ₹10")
        }
        if (amount > 200000) {
            throw Exception("Maximum deposit is ₹2,00,000")
        }

        // 2. Get current user ID & session
        val userId = SupabaseClient.currentUserId ?: throw Exception("Please sign in first")
        val token = SupabaseClient.authToken ?: throw Exception("Session expired. Please sign in again")

        // 3. Insert into deposits table
        val depositPayload = mapOf(
            "user_id" to userId,
            "amount" to amount,
            "gateway" to "zapupi",
            "status" to "PENDING"
        )

        val createRes = try {
            SupabaseClient.restApi.insertDepositRow(deposit = depositPayload)
        } catch (e: Exception) {
            throw Exception("Could not create deposit record: ${e.message}")
        }

        if (!createRes.isSuccessful || createRes.body().isNullOrEmpty()) {
            throw Exception("Could not create deposit record")
        }

        val depositRow = createRes.body()!!.first()
        val depositId = depositRow.id

        // 4 & 5. HTTP POST to ${SUPABASE_URL}/functions/v1/create-deposit-order
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val bodyJson = JSONObject().apply {
            put("deposit_id", depositId)
            put("amount", amount)
            put("currency", "INR")
        }.toString()

        val request = Request.Builder()
            .url("${SupabaseClient.BASE_URL}functions/v1/create-deposit-order")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("apikey", SupabaseClient.ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val response = try {
            SupabaseClient.okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw Exception("Failed to reach payment gateway: ${e.message}")
        }

        val responseStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            var errorMsg: String? = null
            try {
                if (responseStr.isNotBlank()) {
                    val json = JSONObject(responseStr)
                    if (json.has("error")) {
                        errorMsg = json.getString("error")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            throw Exception(errorMsg ?: "Payment gateway error (${response.code})")
        }

        val resJson = try {
            JSONObject(responseStr)
        } catch (e: Exception) {
            throw Exception("Invalid response from payment gateway")
        }

        if (!resJson.optBoolean("success", false)) {
            val err = resJson.optString("error", "Could not create payment order")
            throw Exception(err)
        }

        val orderId = resJson.optString("order_id", "")
        val checkoutUrl = resJson.optString("checkout_url", "")

        if (orderId.isBlank() || checkoutUrl.isBlank()) {
            throw Exception("No checkout URL returned. Contact support.")
        }

        return@withContext CheckoutResult(
            orderId = orderId,
            checkoutUrl = checkoutUrl,
            depositId = depositId
        )
    }

    suspend fun getWalletBalance(): WalletInfo = withContext(Dispatchers.IO) {
        val userId = SupabaseClient.currentUserId ?: return@withContext WalletInfo()
        try {
            val res = SupabaseClient.restApi.getWalletInfo("eq.$userId")
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                return@withContext res.body()!!.first()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext WalletInfo()
    }

    suspend fun pollDepositStatus(depositId: String): DepositStatus = withContext(Dispatchers.IO) {
        var attempts = 0
        while (attempts < 40) {
            try {
                val res = SupabaseClient.restApi.getDepositStatus("eq.$depositId")
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val row = res.body()!!.first()
                    when (row.status.uppercase()) {
                        "SUCCESS", "COMPLETED" -> return@withContext DepositStatus.Success(row.amount)
                        "FAILED", "CANCELLED" -> return@withContext DepositStatus.Failed
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            attempts++
            delay(3000)
        }
        throw Exception("Verification taking longer than expected. Check transaction history.")
    }

    fun observeDepositStatus(depositId: String): Flow<DepositStatus> = flow {
        emit(DepositStatus.Pending)
        var attempts = 0
        while (attempts < 20) {
            delay(3000)
            attempts++
            try {
                val res = SupabaseClient.restApi.getDepositStatus("eq.$depositId")
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val row = res.body()!!.first()
                    when (row.status.uppercase()) {
                        "SUCCESS", "COMPLETED" -> {
                            emit(DepositStatus.Success(row.amount))
                            return@flow
                        }
                        "FAILED", "CANCELLED" -> {
                            emit(DepositStatus.Failed)
                            return@flow
                        }
                    }
                } else if (res.isSuccessful && res.body().isNullOrEmpty()) {
                    emit(DepositStatus.NotFound)
                    return@flow
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        emit(DepositStatus.Pending)
    }
}
