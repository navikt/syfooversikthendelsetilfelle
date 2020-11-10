package no.nav.syfo.oppfolgingstilfelle.retry

import no.nav.syfo.domain.AktorId
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_RETRY_SKIPPED
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import org.slf4j.LoggerFactory

class OppfolgingstilfelleRetryService(
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val oppfolgingstilfelleRetryProducer: OppfolgingstilfelleRetryProducer
) {
    suspend fun receiveOversikthendelseRetry(
        kOppfolgingstilfelleRetry: KOppfolgingstilfelleRetry,
        callId: String = ""
    ) {
        when {
            kOppfolgingstilfelleRetry.hasExceededRetryLimit() -> {
                skip(kOppfolgingstilfelleRetry)
            }
            kOppfolgingstilfelleRetry.isReadyToRetry() -> {
                val isMissingMandatoryValue = !oppfolgingstilfelleService.processOppfolgingstilfelle(
                    AktorId(kOppfolgingstilfelleRetry.aktorId),
                    Virksomhetsnummer(kOppfolgingstilfelleRetry.orgnummer),
                    callId
                )
                if (isMissingMandatoryValue) {
                    oppfolgingstilfelleRetryProducer.sendRetriedOppfolgingstilfelleRetry(
                        kOppfolgingstilfelleRetry,
                        callId
                    )
                } else {
                    LOG.info("Sent Oversikthendelsetilfelle after retry with count ${kOppfolgingstilfelleRetry.retriedCount}")
                }
            }
            else -> {
                oppfolgingstilfelleRetryProducer.sendAgainOppfolgingstilfelleRetry(
                    kOppfolgingstilfelleRetry,
                    callId
                )
            }
        }
    }

    private fun skip(oppfolgingstilfelleRetry: KOppfolgingstilfelleRetry) {
        LOG.error("Retry limit $RETRY_OPPFOLGINGSTILFELLE_COUNT_LIMIT reached, skipping oversikthendelseRetry with retryCounter=${oppfolgingstilfelleRetry.retriedCount}")
        COUNT_OPPFOLGINGSTILFELLE_RETRY_SKIPPED.inc()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OppfolgingstilfelleRetryService::class.java)
    }
}
