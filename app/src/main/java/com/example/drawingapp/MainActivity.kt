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

fun createSvgFileInDocuments(context: Context, filename: String): File {
    val documentsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Scoped storage: use app Documents dir
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    } else {
        // Legacy external directory
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    }

    if (documentsDir != null && !documentsDir.exists()) documentsDir.mkdirs()

    val file = File(documentsDir, filename)
    if (!file.exists()) file.createNewFile()

    // Make sure file appears in Files/My Files
    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)

    return file
}


@Composable
fun UploadPhotoScreen(img2code: (String, String, Int) -> Int) {
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
            // Save directly to native Documents folder
            val outputFile = createSvgFileInDocuments(context, "output.svg")
            val outputPath = outputFile.absolutePath

            val threshVal = 128 // example threshold
            val result = img2code(inputPath, outputPath, threshVal)
            resultText = "C++ result: $result\nOutput saved at $outputPath"
            resultText = "C++ result: $result\nOutput saved at:\n$outputPath"
            Toast.makeText(context, "Saved to Documents/output.svg", Toast.LENGTH_LONG).show()
            Log.d("UploadPhotoScreen", resultText)
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
    external fun img2code(inputPath: String, outputPath: String, threshVal: Int): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrawingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                )
                {
                    UploadPhotoScreen { inputPath, outputPath, threshVal ->
                        img2code(inputPath, outputPath, threshVal)
                    }
                }
            }
        }
    }
}
