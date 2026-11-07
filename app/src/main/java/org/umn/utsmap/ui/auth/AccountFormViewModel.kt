package org.umn.utsmap.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Retains secret input during configuration changes without putting passwords in saved state. */
class AccountFormViewModel : ViewModel() {
    var old by mutableStateOf("")
    var next by mutableStateOf("")
    var confirmation by mutableStateOf("")
}
