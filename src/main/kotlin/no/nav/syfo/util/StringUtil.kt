package no.nav.syfo.util

fun String.lowerCapitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}
