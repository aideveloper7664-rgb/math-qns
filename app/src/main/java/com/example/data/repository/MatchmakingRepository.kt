package com.example.data.repository

import android.util.Log
import com.example.data.remote.CancelMatchmakingResponse
import com.example.data.remote.JoinMatchmakingRequest
import com.example.data.remote.JoinMatchmakingResponse
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MatchmakingRepository {

    /**
     * Calls the join-matchmaking Edge Function.
     * Returns a JoinMatchmakingResponse indicating whether a match was found
     * immediately or the user is now queued.
     */
    suspend fun joinMatchmaking(gameMode: String, entryFee: Double): JoinMatchmakingResponse {
        return try {
            val response = SupabaseClient.matchmakingApi.joinMatchmaking(
                JoinMatchmakingRequest(gameMode = gameMode, entryFee = entryFee)
            )
            if (response.isSuccessful) {
                response.body() ?: JoinMatchmakingResponse(
                    success = false,
                    error = "Empty response from server"
                )
            } else {
                val errBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("Matchmaking", "joinMatchmaking failed ${response.code()}: $errBody")
                JoinMatchmakingResponse(
                    success = false,
                    error = "Server error (${response.code()})"
                )
            }
        } catch (e: Exception) {
            Log.e("Matchmaking", "joinMatchmaking exception", e)
            JoinMatchmakingResponse(success = false, error = e.message ?: "Network error")
        }
    }

    /**
     * Calls the cancel-matchmaking Edge Function.
     */
    suspend fun cancelMatchmaking(): CancelMatchmakingResponse {
        return try {
            val response = SupabaseClient.matchmakingApi.cancelMatchmaking()
            if (response.isSuccessful) {
                response.body() ?: CancelMatchmakingResponse(success = false)
            } else {
                Log.e("Matchmaking", "cancelMatchmaking failed ${response.code()}")
                CancelMatchmakingResponse(success = false)
            }
        } catch (e: Exception) {
            Log.e("Matchmaking", "cancelMatchmaking exception", e)
            CancelMatchmakingResponse(success = false)
        }
    }

    /**
     * Polls matchmaking_queue every 2 seconds for this user.
     * Emits match_id as soon as status == "matched" and match_id is not null.
     * Stops automatically after 120 seconds (timeout).
     *
     * NOTE: This project uses Retrofit/OkHttp (not Supabase Realtime SDK),
     * so polling is used instead of WebSocket subscriptions.
     */
    fun observeMatchFound(userId: String): Flow<String> = flow {
        val maxWaitMs = 120_000L   // 2 minutes max
        val pollIntervalMs = 2_000L
        var elapsed = 0L

        while (elapsed < maxWaitMs) {
            try {
                val response = SupabaseClient.matchmakingApi.pollQueue(
                    userIdFilter = "eq.$userId"
                )
                if (response.isSuccessful) {
                    val row = response.body()?.firstOrNull()
                    if (row != null && row.status == "matched" && !row.matchId.isNullOrBlank()) {
                        Log.d("Matchmaking", "✅ Match found: ${row.matchId}")
                        emit(row.matchId)
                        return@flow
                    }
                }
            } catch (e: Exception) {
                Log.w("Matchmaking", "Poll error (will retry): ${e.message}")
            }

            delay(pollIntervalMs)
            elapsed += pollIntervalMs
        }

        // Timed out — emit empty string so ViewModel can show error
        Log.w("Matchmaking", "⏰ Matchmaking timed out after ${maxWaitMs / 1000}s")
        emit("")
    }
}
