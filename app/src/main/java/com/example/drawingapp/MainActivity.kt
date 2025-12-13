package com.example.drawingapp

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drawingapp.ui.theme.DrawingAppTheme
import java.io.InputStream


// NTAG216 has 888 bytes of user memory. NDEF overhead is ~30 bytes.
private const val BUF_SIZE = 850

enum class NfcWriteStatus {
    IDLE,
    READY,
    WRITING,
    SUCCESS,
    FAIL
}

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
fun NfcWriteDialog(status: NfcWriteStatus, onDismiss: () -> Unit) {
    if (status == NfcWriteStatus.IDLE) return

    val title: String
    val text: String
    when (status) {
        NfcWriteStatus.READY -> {
            title = "Ready to Write"
            text = "Please hold the NFC card to the back of your phone."
        }
        NfcWriteStatus.WRITING -> {
            title = "Writing to Card"
            text = "Card detected. Writing in progress. Please don't move the card."
        }
        NfcWriteStatus.SUCCESS -> {
            title = "Success"
            text = "The data was written successfully."
        }
        NfcWriteStatus.FAIL -> {
            title = "Error"
            text = "Write failed. The card may be unsupported, write-protected, or have insufficient space."
        }
        else -> return
    }

    AlertDialog(
        onDismissRequest = { if (status != NfcWriteStatus.WRITING) onDismiss() },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            if (status == NfcWriteStatus.SUCCESS || status == NfcWriteStatus.FAIL) {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (status == NfcWriteStatus.READY) {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun UploadPhotoScreen(
    img2code: (String, Int, ByteArray, Int) -> Int,
    nfcWriteStatus: NfcWriteStatus,
    onWriteToNfc: (ByteArray) -> Unit,
    onNfcDialogDismiss: () -> Unit
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var resultText by remember { mutableStateOf("") }

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("Permissions", "Granted: $permissions")
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        imageBytes = null
        resultText = ""

        val inputPath = uri?.let { uriToFilePath(context, it) }

        if (inputPath != null) {
            val threshVal = 128
            val buffer = ByteArray(BUF_SIZE)
            val usedBytes = img2code(inputPath, threshVal, buffer, BUF_SIZE)

            if (usedBytes <= 0) {
                resultText = "Could not convert to points"
                imageBytes = null
            } else {
                imageBytes = buffer.copyOf(usedBytes)
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

    NfcWriteDialog(status = nfcWriteStatus, onDismiss = onNfcDialogDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (imageUri == null) Arrangement.Center else Arrangement.Top
    ) {
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
                Spacer(Modifier.height(16.dp))
            }
        }

        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.size(width = 200.dp, height = 50.dp)
        ) {
            Text("Upload Photo", fontSize = 18.sp)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                imageBytes?.let {
                    if (it.size > 800) {
                        resultText = "Data too large for NTAG216 (${it.size} bytes)"
                    } else {
                        onWriteToNfc(it)
                    }
                }
            },
            enabled = imageBytes != null,
            modifier = Modifier.size(width = 200.dp, height = 50.dp)
        ) {
            Text("Write To Card", fontSize = 18.sp)
        }

        if (resultText.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(resultText)
        }
    }
}

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var byteArrayToWrite: ByteArray? = null
    private var nfcWriteStatus by mutableStateOf(NfcWriteStatus.IDLE)

    init {
        System.loadLibrary("native-lib")
    }

    external fun img2code(inputPath: String, threshVal: Int, buffer: ByteArray, bufSize: Int): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            DrawingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UploadPhotoScreen(
                        img2code = this::img2code,
                        nfcWriteStatus = nfcWriteStatus,
                        onWriteToNfc = { bytes ->
                            byteArrayToWrite = bytes
                            nfcWriteStatus = NfcWriteStatus.READY
                        },
                        onNfcDialogDismiss = {
                            nfcWriteStatus = NfcWriteStatus.IDLE
                            byteArrayToWrite = null
                        }
                    )
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            addDataType("application/octet-stream")
        }

        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)

        val filters = arrayOf(ndefFilter, techFilter)

        val techLists = arrayOf(
            arrayOf(Ndef::class.java.name)
        )

        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            filters,
            techLists
        )
    }



    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val action = intent.action
        if (
            (action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                    action == NfcAdapter.ACTION_NDEF_DISCOVERED) &&
            nfcWriteStatus == NfcWriteStatus.READY
        ) {
            nfcWriteStatus = NfcWriteStatus.WRITING

            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag != null && byteArrayToWrite != null) {
                writeNfcTag(tag, byteArrayToWrite!!)
            } else {
                nfcWriteStatus = NfcWriteStatus.FAIL
            }
        }
    }


    private fun writeNfcTag(tag: Tag, data: ByteArray) {
        val record = NdefRecord.createMime(
            "application/octet-stream",
            data
        )
        val message = NdefMessage(arrayOf(record))

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()

                Log.d("NFC", "maxSize=${ndef.maxSize}, msgSize=${message.toByteArray().size}")

                if (!ndef.isWritable || ndef.maxSize < message.toByteArray().size) {
                    ndef.close()
                    nfcWriteStatus = NfcWriteStatus.FAIL
                    return
                }

                ndef.writeNdefMessage(message)
                ndef.close()
            } else {
                // Tag not formatted yet
                val formatable = android.nfc.tech.NdefFormatable.get(tag)
                    ?: throw Exception("Tag does not support NDEF")

                formatable.connect()
                formatable.format(message)
                formatable.close()
            }

            nfcWriteStatus = NfcWriteStatus.SUCCESS
        } catch (e: Exception) {
            Log.e("NFC", "Write failed", e)
            nfcWriteStatus = NfcWriteStatus.FAIL
        }
    }
}
