package com.example.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DepositRepository
import com.example.data.repository.DepositStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentProcessingViewModel(
    private val depositRepository: DepositRepository = DepositRepository()
) : ViewModel() {

    private val _statusState = MutableStateFlow<DepositStatus>(DepositStatus.Pending)
    val statusState: StateFlow<DepositStatus> = _statusState.asStateFlow()

    private var watchJob: Job? = null
    private var timeoutJob: Job? = null

    fun watch(depositId: String, onTimeout: () -> Unit) {
        if (watchJob != null) return

        timeoutJob = viewModelScope.launch {
            delay(60_000) // 60s timeout
            if (_statusState.value is DepositStatus.Pending) {
                onTimeout()
            }
        }

        watchJob = viewModelScope.launch {
            depositRepository.observeDepositStatus(depositId).collect { status ->
                _statusState.value = status
                if (status is DepositStatus.Success || status is DepositStatus.Failed) {
                    timeoutJob?.cancel()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        watchJob?.cancel()
        timeoutJob?.cancel()
    }
}
