package no.nav.klage.oppgave.api.view

import no.nav.klage.oppgave.domain.kodeverk.Grunn
import no.nav.klage.oppgave.domain.kodeverk.Utfall
import org.springframework.web.multipart.MultipartFile

data class VedtakUtfallInput(val utfall: Utfall, val klagebehandlingVersjon: Long? = null)

data class VedtakGrunnInput(val grunn: Grunn?, val klagebehandlingVersjon: Long? = null)

data class VedtakVedleggInput(
    val vedlegg: MultipartFile,
    val journalfoerendeEnhet: String,
    val klagebehandlingVersjon: Long? = null
)

data class VedtakFullfoerInput(val journalfoerendeEnhet: String, val klagebehandlingVersjon: Long? = null)