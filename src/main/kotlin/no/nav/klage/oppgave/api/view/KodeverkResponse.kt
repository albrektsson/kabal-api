package no.nav.klage.oppgave.api.view

import no.nav.klage.kodeverk.*
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.hjemmel.ytelseTilRegistreringshjemler

data class KodeverkResponse(
    val hjemmel: List<Kode> = getHjemler(),
    val type: List<Kode> = Type.values().asList().toDto(),
    val tema: List<Kode> = Tema.values().asList().toDto(),
    val ytelse: List<Kode> = Ytelse.values().asList().toDto(),
    val utfall: List<Kode> = Utfall.values().asList().toDto(),
    val hjemlerPerTema: List<HjemlerPerTema> = hjemlerPerTema(),
    val hjemlerPerYtelse: List<HjemlerPerYtelse> = hjemlerPerYtelse(),
    val partIdType: List<Kode> = PartIdType.values().asList().toDto(),
//    val rolle: List<Kode> = Rolle.values().asList().toDto(),
    val fagsystem: List<Kode> = Fagsystem.values().asList().toDto(),
)

data class KodeDto(override val id: String, override val navn: String, override val beskrivelse: String) : Kode

data class HjemlerPerTema(val temaId: String, val hjemler: List<KodeDto>)

data class HjemlerPerYtelse(val ytelseId: String, val hjemler: List<KodeDto>)

fun Kode.toDto() = KodeDto(id, navn, beskrivelse)

fun Registreringshjemmel.toDto() = KodeDto(
    id = this.id,
    navn = this.lovKilde.beskrivelse + " - " + this.spesifikasjon,
    beskrivelse = this.lovKilde.navn + " - " + this.spesifikasjon,
)

fun List<Kode>.toDto() = map { it.toDto() }

fun hjemlerPerTema(): List<HjemlerPerTema> =
    temaToHjemmelList.map { HjemlerPerTema(it.tema.id, it.hjemler.map { hjemmel -> hjemmel.toDto() }) }

fun hjemlerPerYtelse(): List<HjemlerPerYtelse> =
    ytelseToHjemmelList.map { HjemlerPerYtelse(it.ytelse.id, it.hjemler.map { hjemmel -> hjemmel.toDto() }) }


fun getHjemler(): List<KodeDto> =
    Registreringshjemmel.values().map {
        KodeDto(
            id = it.id,
            navn = it.lovKilde.beskrivelse + " - " + it.spesifikasjon,
            beskrivelse = it.lovKilde.navn + " - " + it.spesifikasjon,
        )
    }

data class TemaToRegistreringshjemler(val tema: Tema, val hjemler: List<Registreringshjemmel>)

val temaToHjemmelList: List<TemaToRegistreringshjemler> = listOf(
    TemaToRegistreringshjemler(
        Tema.OMS,
        ytelseTilRegistreringshjemler[Ytelse.OMS_PLS]!!.plus(
            ytelseTilRegistreringshjemler[Ytelse.OMS_OLP]!!.plus(
                ytelseTilRegistreringshjemler[Ytelse.OMS_OMP]!!.plus(
                    ytelseTilRegistreringshjemler[Ytelse.OMS_PSB]!!
                )
            )
        ).distinct()
    ),
    TemaToRegistreringshjemler(
        Tema.SYK,
        ytelseTilRegistreringshjemler[Ytelse.SYK_SYK]!!
    )
)

data class YtelseToRegistreringshjemler(val ytelse: Ytelse, val hjemler: List<Registreringshjemmel>)

val ytelseToHjemmelList: List<YtelseToRegistreringshjemler> = listOf(
    YtelseToRegistreringshjemler(
        Ytelse.OMS_PLS,
        ytelseTilRegistreringshjemler[Ytelse.OMS_PLS]!!
    ),
    YtelseToRegistreringshjemler(
        Ytelse.OMS_PSB,
        ytelseTilRegistreringshjemler[Ytelse.OMS_PSB]!!
    ),
    YtelseToRegistreringshjemler(
        Ytelse.OMS_OMP,
        ytelseTilRegistreringshjemler[Ytelse.OMS_OMP]!!
    ),
    YtelseToRegistreringshjemler(
        Ytelse.OMS_OLP,
        ytelseTilRegistreringshjemler[Ytelse.OMS_OLP]!!
    ),
    YtelseToRegistreringshjemler(
        Ytelse.SYK_SYK,
        ytelseTilRegistreringshjemler[Ytelse.SYK_SYK]!!
    )
)
