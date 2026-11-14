package com.farmsaathi.core.designsystem.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class FarmPhotoLauncherState(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit,
)

/**
 * Shared offline camera + gallery picker helper respecting on-demand CAMERA permission.
 * Eliminates duplicated ActivityResult boilerplate across Farm, Diary, Expense, and Soil screens.
 */
@Composable
fun rememberFarmPhotoLaunchers(
    createTempFile: () -> File,
    onCapturedFile: (File) -> Unit,
    onGalleryUri: (Uri) -> Unit,
    onCameraPermissionDenied: () -> Unit = {},
): FarmPhotoLauncherState {
    val context = LocalContext.current
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            val file = pendingFile
            pendingFile = null
            if (success && file != null && file.exists()) {
                onCapturedFile(file)
            } else {
                file?.delete()
            }
        }

    val startCameraInternal: () -> Unit = {
        val file = createTempFile()
        pendingFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        takePictureLauncher.launch(uri)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) startCameraInternal() else onCameraPermissionDenied()
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) onGalleryUri(uri)
        }

    return remember {
        FarmPhotoLauncherState(
            launchCamera = {
                val granted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED
                if (granted) startCameraInternal() else permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            launchGallery = { galleryLauncher.launch("image/*") },
        )
    }
}
