package no.nav.syfo.testutil

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import java.net.ServerSocket
import java.util.*

fun testEnvironment(port: Int, kafkaBootstrapServers: String) = Environment(
    applicationPort = port,
    applicationThreads = 1,
    oppfolgingstilfelleTopic = "",
    kafkaBootstrapServers = kafkaBootstrapServers,
    applicationName = "syfooversikthendelsetilfelle",
    behandlendeenhetUrl = "behandlendeenhet",
    pdlUrl = "pdlurl",
    syketilfelleUrl = "syketilfelle",
    toggleOversikthendelsetilfelle = true,
    oversikthendelseOppfolgingstilfelleTopicSeekToBeginning = false,
    aktoerregisterV1Url = "aktorurl",
    stsRestUrl = "stsurl",
    eregApiBaseUrl = ""
)

val vaultSecrets = VaultSecrets(
    "username",
    "password"
)

fun Properties.overrideForTest(): Properties = apply {
    remove("security.protocol")
    remove("sasl.mechanism")
}

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
