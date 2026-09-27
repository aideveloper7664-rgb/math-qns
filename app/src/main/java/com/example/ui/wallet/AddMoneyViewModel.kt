package com.example.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DepositRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddMoneyUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val checkoutUrl: String? = null,
    val depositId: String? = null
)

class AddMoneyViewModel(
    private val depositRepository: DepositRepository = DepositRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMoneyUiState())
    val uiState: StateFlow<AddMoneyUiState> = _uiState.asStateFlow()

    fun createDeposit(amount: Double) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = depositRepository.createDepositOrder(amount)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        checkoutUrl = result.checkoutUrl,
                        depositId = result.depositId,
                        error = null
                    )
                }
            } catch (e: Exception) {
                val rawMsg = e.message ?: "Could not create payment order"
                val friendlyError = when {
                    rawMsg.contains("Unauthorized", ignoreCase = true) -> "Please sign in again"
                    rawMsg.contains("Gateway not configured", ignoreCase = true) -> "Payment is not available right now"
                    rawMsg.contains("Gateway disabled", ignoreCase = true) -> "Payment is temporarily disabled"
                    rawMsg.contains("Invalid input", ignoreCase = true) -> "Please enter a valid amount"
                    else -> rawMsg
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = friendlyError,
                        checkoutUrl = null,
                        depositId = null
                    )
                }
            }
        }
    }

    fun clearCheckoutUrl() {
        _uiState.update { it.copy(checkoutUrl = null, depositId = null) }
    }
}
