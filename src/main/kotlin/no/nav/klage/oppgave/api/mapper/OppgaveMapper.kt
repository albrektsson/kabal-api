package no.nav.klage.oppgave.api.mapper


import no.nav.klage.oppgave.api.internal.OppgaveKopiAPIModel
import no.nav.klage.oppgave.clients.egenansatt.EgenAnsattService
import no.nav.klage.oppgave.clients.pdl.PdlFacade
import no.nav.klage.oppgave.domain.elasticsearch.EsKlagebehandling
import no.nav.klage.oppgave.domain.oppgavekopi.*
import no.nav.klage.oppgave.util.getLogger
import no.nav.klage.oppgave.util.getSecureLogger
import org.springframework.stereotype.Service
import no.nav.klage.oppgave.api.view.Oppgave as OppgaveView

@Service
class OppgaveMapper(
    private val pdlFacade: PdlFacade,
    private val egenAnsattService: EgenAnsattService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun mapEsKlagebehandlingerToView(
        esKlagebehandlinger: List<EsKlagebehandling>,
        fetchPersoner: Boolean
    ): List<OppgaveView> {
        return esKlagebehandlinger.map { esKlagebehandling ->
            OppgaveView(
                id = esKlagebehandling.id,
                person = if (fetchPersoner) {
                    OppgaveView.Person(
                        esKlagebehandling.foedselsnummer ?: "mangler",
                        esKlagebehandling.navn ?: "mangler"
                    )
                } else {
                    null
                },
                type = esKlagebehandling.sakstype.navn,
                tema = esKlagebehandling.tema.navn,
                hjemmel = esKlagebehandling.hjemler?.firstOrNull() ?: "mangler",
                frist = esKlagebehandling.frist,
                mottatt = esKlagebehandling.mottattKlageinstans,
                versjon = esKlagebehandling.versjon!!.toInt()
            )
        }
    }

    fun mapOppgaveKopiAPIModelToOppgaveKopi(oppgave: OppgaveKopiAPIModel): OppgaveKopi {
        return OppgaveKopi(
            id = oppgave.id,
            versjon = oppgave.versjon,
            journalpostId = oppgave.journalpostId,
            saksreferanse = oppgave.saksreferanse,
            mappeId = oppgave.mappeId,
            status = Status.valueOf(oppgave.status.name),
            tildeltEnhetsnr = oppgave.tildeltEnhetsnr,
            opprettetAvEnhetsnr = oppgave.opprettetAvEnhetsnr,
            endretAvEnhetsnr = oppgave.endretAvEnhetsnr,
            tema = oppgave.tema,
            temagruppe = oppgave.temagruppe,
            behandlingstema = oppgave.behandlingstema,
            oppgavetype = oppgave.oppgavetype,
            behandlingstype = oppgave.behandlingstype,
            prioritet = Prioritet.valueOf(oppgave.prioritet.name),
            tilordnetRessurs = oppgave.tilordnetRessurs,
            beskrivelse = oppgave.beskrivelse,
            fristFerdigstillelse = oppgave.fristFerdigstillelse,
            aktivDato = oppgave.aktivDato,
            opprettetAv = oppgave.opprettetAv,
            endretAv = oppgave.endretAv,
            opprettetTidspunkt = oppgave.opprettetTidspunkt,
            endretTidspunkt = oppgave.endretTidspunkt,
            ferdigstiltTidspunkt = oppgave.ferdigstiltTidspunkt,
            behandlesAvApplikasjon = oppgave.behandlesAvApplikasjon,
            journalpostkilde = oppgave.journalpostkilde,
            ident = Ident(
                id = oppgave.ident.id,
                identType = IdentType.valueOf(oppgave.ident.identType.name),
                verdi = oppgave.ident.verdi,
                folkeregisterident = oppgave.ident.folkeregisterident,
                registrertDato = null
            ),
            metadata = oppgave.metadata?.map { (k, v) ->
                MetadataNoekkel.valueOf(k.name) to v
            }?.toMap() ?: emptyMap()
        )
    }
}
