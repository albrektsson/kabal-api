package no.nav.klage.oppgave.domain.trygderetten

import java.time.LocalDate
import java.time.LocalDateTime


class Arkivmelding(
    val system: String,
    val meldingId: String,
    val tidspunkt: LocalDateTime,
    val antallFiler: Int,
    val mappe: Mappe
) {

    fun toXml(): String {
        return ""
//        return xml("people") {
//            xmlns = "http://example.com/people"
//            "person" {
//                attribute("id", id)
//                "firstName" {
//                    -firstName
//                }
//                "lastName" {
//                    -lastName
//                }
//                "phone" {
//                    -phone
//                }
//            }
//        }.toString()
    }

    data class Mappe(
        val tittel: String,
        val opprettetDato: LocalDateTime,
        val virksomhetsspesifikkeMetadata: String,
        val part: Part,
        val registrering: Registrering,
        val saksdato: LocalDate,
        val administrativEnhet: String,
        val saksansvarlig: String,
        val journalenhet: String,
        val saksstatus: String

    ) {
        data class Part(
            val partNavn: String,
            val partRolle: String,
            val organisasjonsnummer: Organisasjonsnummer,
            val foedselsnummer: Foedselsnummer,
            val kontaktperson: String
        ) {

            data class Foedselsnummer(
                val foedselsnummer: String
            )
        }

        data class Registrering(
            val opprettetDato: LocalDateTime,
            val opprettetAv: String,
            val dokumentbeskrivelse: Dokumentbeskrivelse,
            val tittel: String,
            val korrespondansepart: Korrespondansepart,
            val journalposttype: String,
            val journalstatus: String,
            val journaldato: LocalDate

        ) {
            data class Dokumentbeskrivelse(
                val dokumenttype: String,
                val dokumentstatus: String,
                val tittel: String,
                val opprettetDato: LocalDateTime,
                val opprettetAv: String,
                val tilknyttetRegistreringSom: String,
                val dokumentnummer: String,
                val tilknyttetDato: LocalDateTime,
                val tilknyttetAv: String,
                val dokumentobjekt: Dokumentobjekt

            ) {
                data class Dokumentobjekt(
                    val versjonsnummer: String,
                    val variantformat: String,
                    val opprettetDato: LocalDateTime,
                    val opprettetAv: String,
                    val referanseDokumentfil: String
                )
            }

            data class Korrespondansepart(
                val korrespondanseparttype: String,
                val korrespondansepartNavn: String,
                val organisasjonsnummer: Organisasjonsnummer

            )
        }
    }

    data class Organisasjonsnummer(
        val organisasjonsnummer: String
    )
}

