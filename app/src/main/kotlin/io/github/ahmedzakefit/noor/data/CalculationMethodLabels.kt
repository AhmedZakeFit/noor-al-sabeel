package io.github.ahmedzakefit.noor.data

import android.content.Context
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.core.calc.CalculationMethod

/** Localized display names for the calculation methods. */
object CalculationMethodLabels {

    fun label(context: Context, method: CalculationMethod): String = when (method) {
        CalculationMethod.MUSLIM_WORLD_LEAGUE -> context.getString(R.string.method_mwl)
        CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY -> context.getString(R.string.method_egypt)
        CalculationMethod.ISNA -> context.getString(R.string.method_isna)
        CalculationMethod.UMM_AL_QURA -> context.getString(R.string.method_umm_alqura)
        CalculationMethod.KARACHI -> context.getString(R.string.method_karachi)
        CalculationMethod.GULF_REGION -> context.getString(R.string.method_gulf)
        CalculationMethod.KUWAIT -> context.getString(R.string.method_kuwait)
        CalculationMethod.QATAR -> context.getString(R.string.method_qatar)
        CalculationMethod.SINGAPORE_MUIS -> context.getString(R.string.method_singapore)
        CalculationMethod.TURKEY_DIYANET -> context.getString(R.string.method_turkey)
        CalculationMethod.FRANCE_UOIF -> context.getString(R.string.method_france)
        CalculationMethod.CUSTOM -> context.getString(R.string.method_custom)
    }
}
