package com.omsharma.iykyk.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omsharma.iykyk.data.model.CollageResult
import com.omsharma.iykyk.state.UiState
import com.omsharma.iykyk.vm.FaceCollageViewModel
import kotlinx.coroutines.launch

@Composable
fun CollageScreen(
    viewModel: FaceCollageViewModel,
    onRecordAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val processingState by viewModel.processingState.collectAsStateWithLifecycle()
    val collageResult = (processingState as? UiState.Success)?.data

    if (collageResult == null) {
        // This screen is only ever navigated to right after a Success state - if it's
        // missing, there's nothing to show, so bounce back to the start of the flow.
        LaunchedEffect(Unit) { onRecordAgain() }
        return
    }

    CollageContent(
        viewModel = viewModel,
        collageResult = collageResult,
        onRecordAgain = onRecordAgain,
        modifier = modifier
    )
}

@Composable
private fun CollageContent(
    viewModel: FaceCollageViewModel,
    collageResult: CollageResult,
    onRecordAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    val savePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.saveToGallery(collageResult.collageBitmap)
        } else {
            Toast.makeText(context, "Storage permission is needed to save", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is UiState.Success -> {
                Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }

            is UiState.Failed -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }

            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val peopleLabel = if (collageResult.peopleCount == 1) "person" else "people"
        Text(
            text = "${collageResult.peopleCount} $peopleLabel found",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Image(
            bitmap = collageResult.collageBitmap.asImageBitmap(),
            contentDescription = "Face collage",
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                enabled = saveState !is UiState.Loading,
                onClick = {
                    val needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED

                    if (needsLegacyPermission) {
                        savePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        viewModel.saveToGallery(collageResult.collageBitmap)
                    }
                }
            ) {
                Text("Save")
            }

            Button(onClick = {
                coroutineScope.launch {
                    val intent = viewModel.createShareIntent(collageResult.collageBitmap)
                    context.startActivity(Intent.createChooser(intent, null))
                }
            }) {
                Text("Share")
            }
        }

        TextButton(onClick = {
            viewModel.resetProcessingState()
            onRecordAgain()
        }) {
            Text("Record Again")
        }
    }
}
