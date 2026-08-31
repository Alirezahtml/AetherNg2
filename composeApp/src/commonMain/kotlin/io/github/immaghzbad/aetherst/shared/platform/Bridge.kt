package io.github.immaghzbad.aetherst.shared.platform

import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import io.github.immaghzbad.aetherst.shared.model.SessionTraffic
import kotlinx.coroutines.flow.MutableStateFlow

object Bridge {
    var submitLoginCode: ((String) -> Unit)? = null
    
    val statusOverride = MutableStateFlow<ConnectionStatus?>(null)
    val trafficOverride = MutableStateFlow<SessionTraffic?>(null)
    val elapsedOverride = MutableStateFlow<Long?>(null)

    var pickFile: ((onResult: (String?) -> Unit) -> Unit)? = null
    var saveFile: ((fileName: String, content: String, onResult: (Boolean) -> Unit) -> Unit)? = null
}
