package no.nav.klage.oppgave.api.mapper


import no.nav.klage.oppgave.api.view.KlagebehandlingDetaljerView
import no.nav.klage.oppgave.api.view.KlagebehandlingListView
import no.nav.klage.oppgave.api.view.KvalitetsvurderingView
import no.nav.klage.oppgave.clients.egenansatt.EgenAnsattService
import no.nav.klage.oppgave.clients.pdl.PdlFacade
import no.nav.klage.oppgave.domain.elasticsearch.EsKlagebehandling
import no.nav.klage.oppgave.domain.klage.Klagebehandling
import no.nav.klage.oppgave.domain.klage.PartId
import no.nav.klage.oppgave.domain.klage.PartIdType
import no.nav.klage.oppgave.domain.kodeverk.Hjemmel
import no.nav.klage.oppgave.util.getLogger
import no.nav.klage.oppgave.util.getSecureLogger
import org.springframework.stereotype.Service

@Service
class KlagebehandlingMapper(
    private val pdlFacade: PdlFacade,
    private val egenAnsattService: EgenAnsattService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun mapKlagebehandlingOgMottakToEsKlagebehandling(klagebehandling: Klagebehandling): EsKlagebehandling {
        val foedselsnummer = foedselsnummer(klagebehandling.klager.partId)

        val personInfo = foedselsnummer?.let { pdlFacade.getPersonInfo(it) }
        val erFortrolig = personInfo?.harBeskyttelsesbehovFortrolig() ?: false
        val erStrengtFortrolig = personInfo?.harBeskyttelsesbehovStrengtFortrolig() ?: false
        val erEgenAnsatt = foedselsnummer?.let { egenAnsattService.erEgenAnsatt(it) } ?: false
        val navn = personInfo?.navn

        return EsKlagebehandling(
            id = klagebehandling.id.toString(),
            versjon = klagebehandling.versjon,
            journalpostId = klagebehandling.saksdokumenter.map { it.journalpostId },
            saksreferanse = klagebehandling.referanseId,
            tildeltEnhet = klagebehandling.tildeltEnhet,
            tema = klagebehandling.tema,
            sakstype = klagebehandling.type,
            tildeltSaksbehandlerident = klagebehandling.tildeltSaksbehandlerident,
            medunderskriverident = klagebehandling.medunderskriverident,
            innsendt = klagebehandling.innsendt,
            mottattFoersteinstans = klagebehandling.mottattFoersteinstans,
            mottattKlageinstans = klagebehandling.mottattKlageinstans,
            frist = klagebehandling.frist,
            startet = klagebehandling.startet,
            avsluttet = klagebehandling.avsluttet,
            hjemler = klagebehandling.hjemler.map { it.id },
            foedselsnummer = foedselsnummer,
            virksomhetsnummer = virksomhetsnummer(klagebehandling.klager.partId),
            navn = navn,
            egenAnsatt = erEgenAnsatt,
            fortrolig = erFortrolig,
            strengtFortrolig = erStrengtFortrolig
        )
    }

    fun mapEsKlagebehandlingerToListView(
        esKlagebehandlinger: List<EsKlagebehandling>,
        fetchPersoner: Boolean,
        saksbehandler: String?
    ): List<KlagebehandlingListView> {
        return esKlagebehandlinger.map { esKlagebehandling ->
            KlagebehandlingListView(
                id = esKlagebehandling.id,
                person = if (fetchPersoner) {
                    KlagebehandlingListView.Person(
                        esKlagebehandling.foedselsnummer,
                        esKlagebehandling.navn
                    )
                } else {
                    null
                },
                type = esKlagebehandling.sakstype.id,
                tema = esKlagebehandling.tema.id,
                hjemmel = esKlagebehandling.hjemler?.firstOrNull(),
                frist = esKlagebehandling.frist,
                mottatt = esKlagebehandling.mottattKlageinstans,
                versjon = esKlagebehandling.versjon!!.toInt(),
                klagebehandlingVersjon = esKlagebehandling.versjon,
                erMedunderskriver = esKlagebehandling.medunderskriverident?.equals(saksbehandler) ?: false
            )
        }
    }

    fun mapKlagebehandlingToKlagebehandlingDetaljerView(klagebehandling: Klagebehandling): KlagebehandlingDetaljerView {
        return KlagebehandlingDetaljerView(
            id = klagebehandling.id,
            klageInnsendtdato = klagebehandling.innsendt,
            //TODO get name from norg2
            fraNAVEnhet = klagebehandling.avsenderEnhetFoersteinstans,
            fraSaksbehandlerident = klagebehandling.avsenderSaksbehandleridentFoersteinstans,
            mottattFoersteinstans = klagebehandling.mottattFoersteinstans,
            sakenGjelderFoedselsnummer = foedselsnummer(klagebehandling.sakenGjelder.partId),
            sakenGjelderVirksomhetsnummer = virksomhetsnummer(klagebehandling.sakenGjelder.partId),
            foedselsnummer = foedselsnummer(klagebehandling.klager.partId),
            virksomhetsnummer = virksomhetsnummer(klagebehandling.klager.partId),
            tema = klagebehandling.tema.id,
            sakstype = klagebehandling.type.id,
            type = klagebehandling.type.id,
            mottatt = klagebehandling.mottattKlageinstans,
            startet = klagebehandling.startet,
            avsluttet = klagebehandling.avsluttet,
            frist = klagebehandling.frist,
            tildeltSaksbehandlerident = klagebehandling.tildeltSaksbehandlerident,
            medunderskriverident = klagebehandling.medunderskriverident,
            hjemler = hjemmelToHjemmelView(klagebehandling.hjemler),
            modified = klagebehandling.modified,
            created = klagebehandling.created,
            grunn = klagebehandling.kvalitetsvurdering?.grunn?.id,
            eoes = klagebehandling.kvalitetsvurdering?.eoes?.id,
            raadfoertMedLege = klagebehandling.kvalitetsvurdering?.raadfoertMedLege?.id,
            internVurdering = klagebehandling.kvalitetsvurdering?.internVurdering,
            sendTilbakemelding = klagebehandling.kvalitetsvurdering?.sendTilbakemelding,
            tilbakemelding = klagebehandling.kvalitetsvurdering?.tilbakemelding,
            klagebehandlingVersjon = klagebehandling.versjon
        )
    }

    private fun hjemmelToHjemmelView(hjemler: Set<Hjemmel>): List<Int> = hjemler.map { it.id }


    fun mapKlagebehandlingToKvalitetsvurderingView(klagebehandling: Klagebehandling): KvalitetsvurderingView {
        val kvalitetsvurdering = klagebehandling.kvalitetsvurdering
        return KvalitetsvurderingView(
            grunn = kvalitetsvurdering?.grunn?.id,
            eoes = kvalitetsvurdering?.eoes?.id,
            raadfoertMedLege = kvalitetsvurdering?.raadfoertMedLege?.id,
            internVurdering = kvalitetsvurdering?.internVurdering,
            sendTilbakemelding = kvalitetsvurdering?.sendTilbakemelding,
            tilbakemelding = kvalitetsvurdering?.tilbakemelding,
            klagebehandlingVersjon = klagebehandling.versjon
        )
    }

    private fun foedselsnummer(partId: PartId) =
        if (partId.type == PartIdType.PERSON) {
            partId.value
        } else {
            null
        }

    private fun virksomhetsnummer(partId: PartId) =
        if (partId.type == PartIdType.VIRKSOMHET) {
            partId.value
        } else {
            null
        }
}
