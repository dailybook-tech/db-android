package co.dailybook.keep.screen.advance.viewmodel

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import co.dailybook.boilerplate.network.model.NetworkResultStatus
import co.dailybook.base.BaseViewModel
import co.dailybook.base.Logger
import co.dailybook.keep.model.AddAdvanceRequestBody
import co.dailybook.keep.screen.advance.uistate.AddAdvanceUiState
import co.dailybook.keep.usecase.KeepUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAdvanceViewModel(val keepUseCase: KeepUseCase): BaseViewModel<AddAdvanceUiState>() {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = AddAdvanceUiState.Error(exception.localizedMessage)
    }

    fun addAdvance(id: String, addAdvanceRequestBody: AddAdvanceRequestBody) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        keepUseCase.addAdvance(id, addAdvanceRequestBody).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = AddAdvanceUiState.Success(it.data ?: "")
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = AddAdvanceUiState.Error(it.message ?: "")
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = AddAdvanceUiState.Loading
                    }
                }
            }
        })
    }

    fun attachAdvanceTextWatcher(textView : TextView){
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                uiState.value = AddAdvanceUiState.AdvanceEntered(s.toString() != "0" && s.toString()
                    .isNotEmpty()
                )
            }

        }
        textView.addTextChangedListener(textWatcher)
    }
}