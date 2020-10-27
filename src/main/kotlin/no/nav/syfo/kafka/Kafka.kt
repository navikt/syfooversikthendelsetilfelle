package no.nav.syfo.kafka

import kotlinx.coroutines.CoroutineScope
import no.nav.syfo.*
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oppfolgingstilfelle.retry.*
import org.apache.kafka.clients.producer.KafkaProducer

suspend fun CoroutineScope.setupKafka(
    vaultSecrets: VaultSecrets,
    aktorService: AktorService,
    eregService: EregService,
    behandlendeEnhetClient: BehandlendeEnhetClient,
    pdlClient: PdlClient,
    syketilfelleClient: SyketilfelleClient
) {
    val producerProperties = kafkaOversikthendelsetilfelleProducerProperties(env, vaultSecrets)
    val oversikthendelseTilfelleProducer = KafkaProducer<String, KOversikthendelsetilfelle>(producerProperties)

    val producerOppfolgingstilfelleRetryProperties = kafkaOppfolgingstilfelleRetryProducerConfig(env, vaultSecrets)
    val oppfolgingstilfelleRetryRecordProducer = KafkaProducer<String, KOppfolgingstilfelleRetry>(producerOppfolgingstilfelleRetryProperties)
    val oppfolgingstilfelleRetryProducer = OppfolgingstilfelleRetryProducer(oppfolgingstilfelleRetryRecordProducer)

    val oppfolgingstilfelleService = OppfolgingstilfelleService(
        aktorService,
        eregService,
        behandlendeEnhetClient,
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
