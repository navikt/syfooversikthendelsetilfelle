package no.nav.syfo.testutil

import no.nav.syfo.VaultSecrets
import java.net.ServerSocket

val vaultSecrets = VaultSecrets(
    "username",
    "password"
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
