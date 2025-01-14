package no.nav.klage.oppgave.clients.kabaldocument.model.request

import java.time.LocalDateTime

data class DokumentEnhetInput(
    val brevMottakere: List<BrevMottakerInput>,
    val journalfoeringData: JournalfoeringDataInput
)

data class DokumentEnhetWithDokumentreferanserInput(
    val brevMottakere: List<BrevMottakerInput>,
    val journalfoeringData: JournalfoeringDataInput,
    val dokumentreferanser: DokumentInput,
) {
    data class DokumentInput(
        val hoveddokument: Dokument,
        val vedlegg: List<Dokument>?,
    ) {
        data class Dokument(
            val mellomlagerId: String,
            val opplastet: LocalDateTime,
            val size: Long,
            val name: String
        )
    }
}
