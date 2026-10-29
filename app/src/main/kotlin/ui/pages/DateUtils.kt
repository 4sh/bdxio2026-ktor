package fr.qsh.bdxio2026.ui.pages

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneOffset.UTC)

fun Instant.format(): String =
    dateTimeFormat.format(java.time.Instant.ofEpochSecond(epochSeconds)) + " UTC"
