package no.nav.syfo.kafka

import kotlinx.coroutines.CoroutineScope
import no.nav.syfo.*
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oppfolgingstilfelle.retry.*
import org.apache.kafka.clients.producer.KafkaProducer

suspend fun CoroutineScope.setupKafka(
    vaultSecrets: VaultSecrets,
    aktorService: AktorService,
    pdlClient: PdlClient,
    syketilfelleClient: SyketilfelleClient
) {
    val producerProperties = kafkaOversikthendelsetilfelleProducerProperties(env, vaultSecrets)
    val oversikthendelseTilfelleProducer: KafkaProducer<String, KOversikthendelsetilfelle>

    val producerOppfolgingstilfelleRetryProperties = kafkaOppfolgingstilfelleRetryProducerConfig(env, vaultSecrets)
    val oppfolgingstilfelleRetryRecordProducer: KafkaProducer<String, KOppfolgingstilfelleRetry>

    try {
        oversikthendelseTilfelleProducer = KafkaProducer<String, KOversikthendelsetilfelle>(producerProperties)
        oppfolgingstilfelleRetryRecordProducer = KafkaProducer<String, KOppfolgingstilfelleRetry>(producerOppfolgingstilfelleRetryProperties)
    } catch (e: Exception) {
        log.error("Fikk en feil ved oppretting av Kafka Producer, restarter pod", e)
        state.running = false
        throw e
    }

    val oppfolgingstilfelleRetryProducer = OppfolgingstilfelleRetryProducer(oppfolgingstilfelleRetryRecordProducer)

    val oppfolgingstilfelleService = OppfolgingstilfelleService(
        aktorService,
        pdlClient,
        syketilfelleClient,
        oppfolgingstilfelleRetryProducer,
        oversikthendelseTilfelleProducer
    )

    createListenerOppfolgingstilfelle(state) {
        blockingApplicationLogicOppfolgingstilfelle(
            state,
            env,
            vaultSecrets,
            oppfolgingstilfelleService
        )
    }

    val oppfolgingstilfelleRetryService = OppfolgingstilfelleRetryService(
        oppfolgingstilfelleService = oppfolgingstilfelleService,
        oppfolgingstilfelleRetryProducer = oppfolgingstilfelleRetryProducer
    )

    createListenerOppfolgingstilfelleRetry(state) {
        blockingApplicationLogicOppfolgingstilfelleRetry(
            state,
            env,
            vaultSecrets,
            oppfolgingstilfelleRetryService
        )
    }

    state.initialized = true
}
