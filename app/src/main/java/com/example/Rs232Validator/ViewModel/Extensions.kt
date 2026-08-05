package com.example.Rs232Validator.ViewModel

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resumeWithException

suspend fun <T> CompletableFuture<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        whenComplete { result, exception ->
            if (exception == null) {
                cont.resume(result, null)
            } else {
                cont.resumeWithException(exception)
            }
        }
    }

data class PayloadExchange(
    val Timestamp: String,
    val RequestPayload: String,
    val RequestDecodedInfo: String,
    val ResponseString: String,
    val ResponseDecodedInfo: String
)

