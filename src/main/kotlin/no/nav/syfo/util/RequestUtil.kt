package no.nav.syfo.util

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import net.logstash.logback.argument.StructuredArguments
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

const val APP_CONSUMER_ID = "syfooversikthendelsetilfelle"
const val NAV_CONSUMER_ID = "Nav-Consumer-Id"

const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
const val TEMA = "Tema"
const val ALLE_TEMA_HEADERVERDI = "GEN"

const val NAV_PERSONIDENTER = "Nav-Personidenter"
const val NAV_CALL_ID = "Nav-Call-Id"

fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    return this.call.request.headers[NAV_CALL_ID].toString()
}

fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

private val kafkaCounter = AtomicInteger(0)

fun kafkaCallId(): String = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-HHmm"))}-syfooversikthendelsetilfelle-kafka-${kafkaCounter.incrementAndGet()}"
