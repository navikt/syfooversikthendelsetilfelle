package no.nav.syfo.oppfolgingstilfelle.retry

import java.time.LocalDateTime

const val RETRY_OPPFOLGINGSTILFELLE_LIMIT_HOURS = 24
const val RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES = 5L
const val RETRY_OPPFOLGINGSTILFELLE_COUNT_LIMIT = (RETRY_OPPFOLGINGSTILFELLE_LIMIT_HOURS / (RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES / 60.0)).toInt()

data class KOppfolgingstilfelleRetry(
    val created: LocalDateTime,
    val retryTime: LocalDateTime,
    val retriedCount: Int,
    val aktorId: String,
    val orgnummer: String
)

fun KOppfolgingstilfelleRetry.hasExceededRetryLimit(): Boolean {
    return this.retriedCount >= RETRY_OPPFOLGINGSTILFELLE_COUNT_LIMIT
}

fun KOppfolgingstilfelleRetry.isReadyToRetry(): Boolean {
    return LocalDateTime.now().isAfter(retryTime)
}
