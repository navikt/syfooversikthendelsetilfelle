package no.nav.syfo

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.AktorregisterClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.kafka.setupKafka
import no.nav.syfo.client.sts.StsRestClient
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

        val stsClientRest = StsRestClient(env.stsRestUrl, vaultSecrets.kafkaUsername, vaultSecrets.kafkaPassword)

        val eregClient = EregClient(env.eregApiBaseUrl, stsClientRest)
        val eregService = EregService(eregClient)

        val aktorregisterClient = AktorregisterClient(env.aktoerregisterV1Url, stsClientRest)
        val aktorService = AktorService(aktorregisterClient)

        module {
            init()
            kafkaModule(vaultSecrets, aktorService, eregService)
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

fun Application.init() {
    isDev {
        state.running = true
    }

    isProd {
        state.running = true
    }
}

fun Application.kafkaModule(
        vaultSecrets: VaultSecrets,
        aktorService: AktorService,
        eregService: EregService
) {

    isDev {
    }

    isProd {
        launch(backgroundTasksContext) {
            setupKafka(vaultSecrets, aktorService, eregService)
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
        registerNaisApi(state)
    }

    state.initialized = true
}


fun CoroutineScope.createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
        launch {
            try {
                action()
            } finally {
                applicationState.running = false
            }
        }

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
