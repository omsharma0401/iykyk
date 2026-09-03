package com.omsharma.iykyk.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import com.omsharma.iykyk.ui.theme.AppDimensions

// Collage with Save / Share / New Video
@Composable
fun CollageContent(
    collage: Bitmap?,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onNewVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimensions.screenInset, vertical = AppDimensions.screenInset)
            .widthIn(max = AppDimensions.contentMaxWidth),
        verticalArrangement = Arrangement.Center
    ) {
        if (collage == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimensions.collagePlaceholderHeight),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Image(
                bitmap = collage.asImageBitmap(),
                contentDescription = "Collage of everyone found in the video",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimensions.imageCornerRadius))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimensions.xLarge),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.medium)
        ) {
            IykykButton(
                text = "Save",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                filled = false
            )
            IykykButton(text = "Share", onClick = onShare, modifier = Modifier.weight(1f))
        }
        IykykButton(
            text = "New Video",
            onClick = onNewVideo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimensions.medium),
            filled = false
        )
    }
}
