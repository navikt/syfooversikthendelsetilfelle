package no.nav.syfo

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.syfo.api.registerPodApi
import no.nav.syfo.api.registerPrometheusApi
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.AktorregisterClient
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.kafka.setupKafka
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.getCallId
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.MainApplicationKt")

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

fun main() {
    val vaultSecrets =
        objectMapper.readValue<VaultSecrets>(Paths.get("/var/run/secrets/nais.io/vault/credentials.json").toFile())
    val server = embeddedServer(Netty, applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = env.applicationPort
        }

        val stsClientRest = StsRestClient(env.stsRestUrl, vaultSecrets.serviceuserUsername, vaultSecrets.serviceuserPassword)

        val eregClient = EregClient(env.eregApiBaseUrl, stsClientRest)
        val eregService = EregService(eregClient)

        val aktorregisterClient = AktorregisterClient(env.aktoerregisterV1Url, stsClientRest)
        val aktorService = AktorService(aktorregisterClient)
        val behandlendeEnhetClient = BehandlendeEnhetClient(env.behandlendeenhetUrl, stsClientRest)
        val pdlClient = PdlClient(env.pdlUrl, stsClientRest)
        val syketilfelleClient = SyketilfelleClient(env.syketilfelleUrl, stsClientRest)

        state.running = true

        module {
            kafkaModule(
                vaultSecrets,
                aktorService,
                eregService,
                behandlendeEnhetClient,
                pdlClient,
                syketilfelleClient
            )
            serverModule()
        }
    })
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(10, 10, TimeUnit.SECONDS)
    })

    server.start(wait = false)
}

val state: ApplicationState = ApplicationState(running = false, initialized = false)
val env: Environment = getEnvironment()

fun Application.kafkaModule(
    vaultSecrets: VaultSecrets,
    aktorService: AktorService,
    eregService: EregService,
    behandlendeEnhetClient: BehandlendeEnhetClient,
    pdlClient: PdlClient,
    syketilfelleClient: SyketilfelleClient
) {
    isDev {
    }

    isProd {
        launch(backgroundTasksContext) {
            setupKafka(
                vaultSecrets,
                aktorService,
                eregService,
                behandlendeEnhetClient,
                pdlClient,
                syketilfelleClient
            )
        }
    }
}

@KtorExperimentalAPI
fun Application.serverModule() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }

    install(CallId) {
        retrieve { it.request.headers["X-Nav-CallId"] }
        retrieve { it.request.headers[HttpHeaders.XCorrelationId] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            log.error("Caught exception", cause, getCallId())
            throw cause
        }
    }

    isProd {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.uri.contains(Regex("is_alive|is_ready|prometheus"))) {
                proceed()
                return@intercept
            }
        }
    }

    routing {
        registerPodApi(state)
        registerPrometheusApi()
    }

    state.initialized = true
}

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
