package com.example.drawingapp


import android.Manifest
import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.drawingapp.ui.theme.DrawingAppTheme
import java.io.File
import java.io.InputStream

private const val BUF_SIZE = 867


fun uriToFilePath(context: Context, uri: Uri): String? {
    var path: String? = null
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            path = it.getString(columnIndex)
        }
    }
    return path
}


@Composable
fun UploadPhotoScreen(img2code: (String, Int, ByteArray, Int) -> Int) {
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var resultText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as? Activity

    // Ask for storage permissions on older Androids
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("Permissions", "Granted: $permissions")
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        imageUri = uri

        // Convert Uri to file path
        val inputPath = uri?.let { uriToFilePath(context, it) }

        if (inputPath != null) {

            val threshVal = 128 // example threshold
            val buffer = ByteArray(BUF_SIZE)                  // allocate buffer
            val ret = img2code(inputPath, threshVal, buffer, BUF_SIZE)

            if (ret == 1) {
                resultText = "Could not convert to points"
            } else {
                // Convert buffer to comma-separated ASCII integers
                resultText = buffer.joinToString(separator = ",") { (it.toInt() and 0xFF).toString() }
            }

        } else {
            resultText = "Could not resolve image path"
        }

    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Upload photo")
        }

        imageUri?.let { uri ->
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
        if (resultText.isNotEmpty()) {
            Text(resultText)
        }
    }
}

class MainActivity : ComponentActivity() {
    // Load the native library
    init {
        System.loadLibrary("native-lib") // matches CMake add_library name
    }

    // Declare the external function
    external fun img2code(inputPath: String, threshVal: Int, buffer: ByteArray, bufSize: Int): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrawingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UploadPhotoScreen { inputPath, threshVal, buffer, bufSize ->
                        val result = img2code(inputPath, threshVal, buffer, bufSize)

                        val outputString = buffer.decodeToString().substringBefore('\u0000')

                        Log.d("MainActivity", "C++ result: $result")
                        Log.d("MainActivity", "Output string: $outputString")

                        result
                    }
                }
            }
        }
    }
}
