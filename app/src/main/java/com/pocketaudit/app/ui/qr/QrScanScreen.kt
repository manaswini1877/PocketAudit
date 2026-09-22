package com.pocketaudit.app.ui.qr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pocketaudit.app.R
import com.pocketaudit.app.qr.QrBarcodeAnalyzer
import com.pocketaudit.app.ui.theme.ElectricBlue
import com.pocketaudit.app.ui.theme.Typography
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    isAnalyzing: Boolean,
    onQrCaptured: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var showRationale by remember {
        mutableStateOf(
            activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        )
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var capturedPayload by remember { mutableStateOf<String?>(null) }
    var scanSession by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
        showRationale = !granted && activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted && !showRationale && !permissionDenied) {
            showRationale = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_qr_title), style = Typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            !permissionGranted -> {
                PermissionPane(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    denied = permissionDenied && !showRationale,
                    onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    CameraPreview(
                        enabled = capturedPayload == null,
                        scanSession = scanSession,
                        lifecycleOwner = lifecycleOwner,
                        onQr = { raw ->
                            if (capturedPayload == null) {
                                capturedPayload = raw
                                onQrCaptured(raw)
                            }
                        },
                        onCameraError = { cameraError = it }
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cameraError?.let {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(stringResource(R.string.scan_qr_subtitle), style = Typography.bodyMedium)
                                if (capturedPayload != null) {
                                    Spacer(Modifier.height(8.dp))
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = stringResource(R.string.qr_captured),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = capturedPayload.orEmpty().take(140),
                                        style = Typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        capturedPayload = null
                                        cameraError = null
                                        scanSession += 1
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = capturedPayload != null && !isAnalyzing
                                ) {
                                    Icon(Icons.Default.Cameraswitch, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.rescan_qr))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPane(
    modifier: Modifier,
    denied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = ElectricBlue)
        Spacer(Modifier.height(12.dp))
        if (denied) {
            Text(stringResource(R.string.camera_permission_denied), style = Typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.camera_permission_open_settings))
            }
        } else {
            Text(stringResource(R.string.scan_qr_rationale), style = Typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.camera_permission_allow))
            }
        }
    }
}

@Composable
private fun CameraPreview(
    enabled: Boolean,
    scanSession: Int,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onQr: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember(scanSession) {
        QrBarcodeAnalyzer { raw ->
            ContextCompat.getMainExecutor(context).execute { onQr(raw) }
        }
    }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(enabled, scanSession, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                if (!enabled) {
                    return@Runnable
                }
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { useCase ->
                        analyzer.reset()
                        useCase.setAnalyzer(cameraExecutor, analyzer)
                    }
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (t: Throwable) {
                onCameraError(context.getString(R.string.camera_unavailable))
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(analyzer) {
        onDispose { analyzer.close() }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
