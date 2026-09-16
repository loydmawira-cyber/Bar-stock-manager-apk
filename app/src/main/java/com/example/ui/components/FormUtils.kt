package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.ui.theme.CrimsonRed

@Composable
fun requiredLabel(label: String): @Composable () -> Unit = {
    Text(
        text = buildAnnotatedString {
            append(label)
            withStyle(SpanStyle(color = CrimsonRed, fontWeight = FontWeight.Bold)) {
                append(" *")
            }
        }
    )
}

@Composable
fun optionalLabel(label: String): @Composable () -> Unit = {
    Text(text = "$label (Optional)")
}
