package com.omsharma.iykyk.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IykykButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    val height = ButtonDefaults.MediumContainerHeight
    val padding = ButtonDefaults.contentPaddingFor(height)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = height),
            shapes = ButtonDefaults.shapes(),
            contentPadding = padding
        ) {
            Text(text, style = ButtonDefaults.textStyleFor(height))
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = height),
            shapes = ButtonDefaults.shapes(),
            contentPadding = padding
        ) {
            Text(text, style = ButtonDefaults.textStyleFor(height))
        }
    }
}
