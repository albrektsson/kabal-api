package no.nav.klage.oppgave.clients.kabaldocument

import no.nav.klage.dokument.api.input.FilInput
import no.nav.klage.oppgave.clients.kabaldocument.model.response.DokumentEnhetOutput
import no.nav.klage.oppgave.domain.DokumentInnhold
import no.nav.klage.oppgave.domain.DokumentInnholdOgTittel
import no.nav.klage.oppgave.domain.DokumentMetadata
import no.nav.klage.oppgave.domain.klage.Klagebehandling
import no.nav.klage.oppgave.util.getLogger
import no.nav.klage.oppgave.util.getSecureLogger
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.*

@Service
class KabalDocumentGateway(
    private val kabalDocumentClient: KabalDocumentClient,
    private val kabalDocumentMapper: KabalDocumentMapper
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()

        private const val BREV_TITTEL = "Brev fra Klageinstans"
        private const val BREVKODE = "BREV_FRA_KLAGEINSTANS"
        private const val BEHANDLINGSTEMA_KLAGE_KLAGEINSTANS = "ab0164"
        private const val KLAGEBEHANDLING_ID_KEY = "klagebehandling_id"
    }

    fun createDokumentEnhet(
        klagebehandling: Klagebehandling
    ): UUID {
        return UUID.fromString(
            kabalDocumentClient.createDokumentEnhet(
                kabalDocumentMapper.mapKlagebehandlingToDokumentEnhetInput(klagebehandling)
            ).id
        )
    }

    fun getDokumentEnhet(
        dokumentEnhetId: UUID,
    ): DokumentEnhetOutput? {
        return null
    }

    fun uploadHovedDokument(
        dokumentEnhetId: UUID,
        multipartFile: MultipartFile
    ): LocalDateTime {
        val response = kabalDocumentClient.uploadHovedDokument(dokumentEnhetId, FilInput(file = multipartFile))
        return response.fileMetadata!!.opplastet
    }

    fun getHovedDokument(dokumentEnhetId: UUID): DokumentInnhold {
        val response = kabalDocumentClient.getHovedDokument(dokumentEnhetId)
        return DokumentInnhold(content = response.first, contentType = response.second)
    }

    fun getMetadataOmHovedDokument(dokumentEnhetId: UUID): DokumentMetadata {
        val dokumentEnhet = kabalDocumentClient.getDokumentEnhet(dokumentEnhetId)

        return DokumentMetadata(
            title = dokumentEnhet.hovedDokument!!.name,
            size = dokumentEnhet.hovedDokument.size,
            opplastet = dokumentEnhet.hovedDokument.opplastet
        )
    }

    fun getHovedDokumentOgMetadata(dokumentEnhetId: UUID): DokumentInnholdOgTittel {
        val dokumentMetadata = getMetadataOmHovedDokument(dokumentEnhetId)
        val dokumentInnhold = getHovedDokument(dokumentEnhetId)
        return DokumentInnholdOgTittel(
            title = dokumentMetadata.title, content = dokumentInnhold.content, contentType = dokumentInnhold.contentType
        )
    }

    fun deleteHovedDokument(dokumentEnhetId: UUID): LocalDateTime =
        kabalDocumentClient.deleteHovedDokument(dokumentEnhetId).modified

    fun fullfoerDokumentEnhet(dokumentEnhetId: UUID): Boolean =
        kabalDocumentClient.fullfoerDokumentEnhet(dokumentEnhetId).avsluttet

}