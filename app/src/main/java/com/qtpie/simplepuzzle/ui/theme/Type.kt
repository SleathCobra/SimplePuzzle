package com.qtpie.simplepuzzle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.R


val FredokaOne = FontFamily(
    Font(R.font.fredoka_one)
)

val Nunito = FontFamily(
    Font(R.font.nunito_regular),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_semibold, FontWeight.SemiBold)
)

val Typography  = androidx.compose.material3.Typography (
    displayLarge = TextStyle(
        fontFamily = FredokaOne,
        fontSize = 42.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FredokaOne,
        fontSize = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Nunito,
        fontSize = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Nunito,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Nunito,
        fontSize = 14.sp
    )
)