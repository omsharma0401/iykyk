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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

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
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (collage == null) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Image(
                bitmap = collage.asImageBitmap(),
                contentDescription = "Collage of everyone found in the video",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IykykButton(text = "Save", onClick = onSave, modifier = Modifier.weight(1f), filled = false)
            IykykButton(text = "Share", onClick = onShare, modifier = Modifier.weight(1f))
        }
        IykykButton(
            text = "New Video",
            onClick = onNewVideo,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            filled = false
        )
    }
}
