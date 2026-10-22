package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.core.calc.CalculationMethod
import io.github.ahmedzakefit.noor.data.AppSettings
import io.github.ahmedzakefit.noor.data.calculationParametersFor
import io.github.ahmedzakefit.noor.data.NominatimClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NominatimParserTest {

    @Test
    fun `parses search results`() {
        val body = """[
            {"place_id":1,"display_name":"Giza, Egypt","lat":"29.98","lon":"31.13"},
            {"place_id":2,"display_name":"Luxor, Egypt","lat":"25.68","lon":"32.63"}
        ]"""
        val results = NominatimClient().parse(body)
        assertEquals(2, results.size)
        assertEquals("Giza, Egypt", results[0].displayName)
        assertEquals(29.98, results[0].latitude, 1e-9)
        assertEquals(32.63, results[1].longitude, 1e-9)
    }
}

class AppSettingsTest {

    @Test
    fun `umm al-qura ramadan rule flips isha to 120 minutes`() {
        val settings = AppSettings(method = CalculationMethod.UMM_AL_QURA)
        // 2026-03-01 is inside Ramadan 1447; 2026-01-15 is not.
        val ramadanParams = settings.calculationParametersFor(LocalDate.of(2026, 3, 1))
        val normalParams = settings.calculationParametersFor(LocalDate.of(2026, 1, 15))
        assertEquals(120, ramadanParams.ishaMinutesAfterMaghrib)
        assertEquals(90, normalParams.ishaMinutesAfterMaghrib)
    }

    @Test
    fun `other methods are untouched by the ramadan rule`() {
        val settings = AppSettings(method = CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY)
        val params = settings.calculationParametersFor(LocalDate.of(2026, 3, 1))
        assertNull(params.ishaMinutesAfterMaghrib)
        assertEquals(19.5, params.fajrAngle, 1e-9)
        assertEquals(1, params.asrFactor)
    }

    @Test
    fun `custom angles override the preset`() {
        val settings = AppSettings(
            method = CalculationMethod.CUSTOM,
            customFajrAngle = 19.0,
            customIshaAngle = 18.0,
        )
        val params = settings.calculationParametersFor(LocalDate.of(2026, 6, 1))
        assertEquals(19.0, params.fajrAngle, 1e-9)
        assertEquals(18.0, params.ishaAngle!!, 1e-9)
    }
}
