package no.nav.klage.oppgave.domain.kafka

import no.nav.klage.oppgave.domain.kodeverk.Utfall
import java.util.*

/**
 * Brukes for å sende info om vedtak fattet til 1. instans
 */
data class KlagevedtakFattet(
    /** Kan brukes til idempotency av konsumenter */
    val transactionId: UUID,
    val kildeReferanse: String,
    val kilde: String,
    val utfall: Utfall,
    val vedtaksbrevReferanse: String?,
    /** Per i dag, vedtak_id */
    val kabalReferanse: String
)
