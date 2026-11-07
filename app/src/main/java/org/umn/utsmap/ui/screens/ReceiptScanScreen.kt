package org.umn.utsmap.ui.screens



import androidx.activity.compose.BackHandler

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.LinearEasing

import androidx.compose.ui.viewinterop.AndroidView

import android.widget.ImageView

import android.net.Uri

import android.graphics.Bitmap

import android.graphics.BitmapFactory

import androidx.compose.ui.platform.LocalContext

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.rotate

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.semantics.*

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import org.umn.utsmap.ui.finance.*

import org.umn.utsmap.data.rupiah

import org.umn.utsmap.ui.theme.JetBrainsMono

import java.time.LocalDate

import java.time.YearMonth

import java.time.format.DateTimeFormatter

import java.util.Locale



@Composable

fun ReceiptScanScreen(scan: ReceiptScanViewModel, finance: FinanceViewModel, onBack: () -> Unit, onSaved: (String) -> Unit, onSplit: () -> Unit = {}) {

    var edit by rememberSaveable { mutableStateOf(false) }

    val back = { if (scan.stage == ScanStage.CAMERA) onBack() else scan.retake() }

    BackHandler { back() }

    Surface(color = MaterialTheme.colorScheme.background) {

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {

            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                AppBackButton(back)

                Text("Scan Struk", style = MaterialTheme.typography.titleLarge, fontSize = 20.sp)

            }

            when (scan.stage) {

                ScanStage.CAMERA -> ReceiptCamera(scan, Modifier.weight(1f))

                ScanStage.PROCESSING -> ScanProcessing(scan, Modifier.weight(1f))

                ScanStage.RESULT -> ReceiptResult(scan, finance, onEdit = { edit = true }, onSplit = onSplit, onSaved = onSaved)

            }

        }

    }

    if (edit) ReceiptEditor(scan) { edit = false }

}



@Composable

private fun ReceiptCamera(scan: ReceiptScanViewModel, modifier: Modifier) {

    val cameraContext = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val executor = remember(cameraContext) { androidx.core.content.ContextCompat.getMainExecutor(cameraContext) }
    val controller = remember(cameraContext) {
        androidx.camera.view.LifecycleCameraController(cameraContext).apply {
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(androidx.camera.view.CameraController.IMAGE_CAPTURE)
        }
    }
    var permissionGranted by remember {
        mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(cameraContext,
            android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    var cameraReady by remember { mutableStateOf(false) }
    var takingPhoto by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        cameraError = if (granted) null else "Izinkan kamera agar dapat memotret langsung, atau upload gambar struk."
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted && scan.selectedImage == null) permission.launch(android.Manifest.permission.CAMERA)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                permissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(cameraContext,
                    android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(controller, lifecycleOwner, permissionGranted, scan.selectedImage) {
        var active = true
        cameraReady = false
        if (permissionGranted && scan.selectedImage == null) {
            try {
                controller.bindToLifecycle(lifecycleOwner)
                controller.initializationFuture.addListener({
                    if (active) try {
                        controller.initializationFuture.get()
                        cameraError = null
                    } catch (_: Exception) {
                        cameraError = "Kamera tidak tersedia. Coba kembali atau upload gambar struk."
                    }
                }, executor)
            } catch (_: Exception) {
                cameraError = "Kamera tidak tersedia. Coba kembali atau upload gambar struk."
            }
        }
        onDispose {
            active = false
            controller.unbind()
            scan.flash = false
        }
    }
    LaunchedEffect(scan.flash, cameraReady) {
        if (cameraReady && controller.cameraInfo?.hasFlashUnit() == true) {
            controller.enableTorch(scan.flash).addListener({
                // CameraX disables the torch when its lifecycle is released.
            }, executor)
        }
    }
    val takePhoto: () -> Unit = {
        if (!takingPhoto && cameraReady) {
            takingPhoto = true
            cameraError = null
            var output: java.io.File? = null
            try {
                val folder = java.io.File(cameraContext.cacheDir, "receipts").apply { mkdirs() }
                val file = java.io.File.createTempFile("receipt-", ".jpg", folder)
                output = file
                controller.takePicture(androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file).build(), executor,
                    object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: androidx.camera.core.ImageCapture.OutputFileResults) {
                            takingPhoto = false
                            scan.selectedImage = androidx.core.content.FileProvider.getUriForFile(cameraContext,
                                "${cameraContext.packageName}.receipts", file).toString()
                            scan.capture()
                        }
                        override fun onError(error: androidx.camera.core.ImageCaptureException) {
                            takingPhoto = false
                            file.delete()
                            cameraError = "Foto gagal diambil. Silakan coba lagi."
                        }
                    })
            } catch (_: Exception) {
                takingPhoto = false
                output?.delete()
                cameraError = "Foto gagal diambil. Silakan coba lagi atau upload gambar."
            }
        }
    }

    var uploadError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->

        if (uri != null) { scan.selectedImage = uri.toString(); uploadError = null }

    }

    val sweep by rememberInfiniteTransition(label = "Garis scan").animateFloat(

        initialValue = .12f, targetValue = .88f,

        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "Posisi garis")

    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {

        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp))

            .background(Brush.verticalGradient(listOf(if (scan.flash) Color(0xFF122D2C) else Color(0xFF08201F), Color(0xFF07101B))))) {

            Box(Modifier.fillMaxSize().padding(horizontal = 50.dp, vertical = 50.dp)

                .background(Color(0xFF0D1923), RoundedCornerShape(6.dp))

                .border(1.dp, Color(0x3316A34A), RoundedCornerShape(6.dp)))

            if (permissionGranted && scan.selectedImage == null) {
                AndroidView(factory = { context ->
                    androidx.camera.view.PreviewView(context).apply {
                        implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                        this.controller = controller
                        previewStreamState.observe(lifecycleOwner) { state ->
                            cameraReady = state == androidx.camera.view.PreviewView.StreamState.STREAMING
                        }
                    }
                }, update = { view ->
                    view.controller = controller
                }, onRelease = { view ->
                    view.previewStreamState.removeObservers(lifecycleOwner)
                    view.controller = null
                },
                    modifier = Modifier.fillMaxSize().semantics {
                        contentDescription = "Pratinjau kamera"
                        stateDescription = if (cameraReady) "Siap" else "Menyiapkan kamera"
                    })
            }

            scan.selectedImage?.let { selected ->

                val context = LocalContext.current

                var preview by remember(selected) { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(selected) {

                    preview = withContext(Dispatchers.IO) {

                        try {

                            val uri = Uri.parse(selected)

                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

                            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

                            var sample = 1

                            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2

                            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }

                        } catch (_: Exception) { null }

                    }

                    if (preview == null) uploadError = "Gambar tidak dapat dibuka. Pilih gambar lain."

                }

                AndroidView(factory = { viewContext -> ImageView(viewContext).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },

                    update = { it.setImageBitmap(preview) }, modifier = Modifier.fillMaxSize().padding(50.dp))

            }

            Canvas(Modifier.fillMaxSize().padding(22.dp)) {

                val length = 28.dp.toPx()

                val stroke = 3.dp.toPx()

                val green = Color(0xFF16A34A)

                listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height)).forEach { point ->

                    val dx = if (point.x == 0f) length else -length

                    val dy = if (point.y == 0f) length else -length

                    drawLine(green, point, point + Offset(dx, 0f), stroke, StrokeCap.Round)

                    drawLine(green, point, point + Offset(0f, dy), stroke, StrokeCap.Round)

                }

                drawLine(green.copy(alpha = .75f), Offset(0f, size.height * sweep), Offset(size.width, size.height * sweep), 1.dp.toPx())

            }

            Text(if (scan.selectedImage == null) if (cameraReady) "Arahkan kamera ke struk belanja" else if (!permissionGranted) "Izinkan kamera untuk mulai memindai" else "Menyiapkan kamera..." else "Gambar siap • Tekan tombol untuk memproses", Modifier.align(Alignment.Center).offset(y = 20.dp),

                fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFF15773B))

            Text("💡 Pastikan seluruh struk terlihat jelas dalam bingkai", Modifier.align(Alignment.BottomCenter).padding(16.dp),

                fontSize = 10.sp, lineHeight = 14.sp, color = Color(0xFF64748B))

        }


        uploadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        cameraError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            TextButton(onClick = {
                cameraContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${cameraContext.packageName}")))
            }) { Text("Pengaturan izin kamera") }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),

            verticalAlignment = Alignment.CenterVertically) {

            Surface(onClick = { picker.launch("image/*") }, modifier = Modifier.size(44.dp).semantics { contentDescription = "Upload gambar struk" },

                color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small,

                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {

                Box(contentAlignment = Alignment.Center) { ScanGlyph("upload", MaterialTheme.colorScheme.onSurfaceVariant) }

            }

            Surface(onClick = {
                if (scan.selectedImage != null) scan.capture()
                else if (!permissionGranted) permission.launch(android.Manifest.permission.CAMERA)
                else takePhoto()
            }, enabled = !takingPhoto && (scan.selectedImage != null || !permissionGranted || cameraReady), shape = CircleShape, color = Color.Transparent,

                modifier = Modifier.size(72.dp).figmaCardShadow(Color(0xFF16A34A), 36f).semantics { contentDescription = "Foto struk" }) {

                Box(Modifier.background(BrandGradient), contentAlignment = Alignment.Center) { ScanGlyph("camera", Color.White, Modifier.size(28.dp)) }

            }

            Surface(onClick = {
                if (cameraReady && controller.cameraInfo?.hasFlashUnit() == true) scan.flash = !scan.flash
                else cameraError = "Lampu kamera tidak tersedia pada perangkat ini."
            }, enabled = cameraReady && scan.selectedImage == null, modifier = Modifier.size(44.dp).semantics {

                contentDescription = "Cahaya pemindai"; stateDescription = if (scan.flash) "Aktif" else "Nonaktif"

            }, color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small,

                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {

                Box(contentAlignment = Alignment.Center) { ScanGlyph("sun", Color(0xFFF4B942)) }

            }

        }

    }

}



@Composable

private fun ScanProcessing(scan: ReceiptScanViewModel, modifier: Modifier) {

    val rotation by animateFloatAsState(scan.quarterTurns * 90f, tween(450), label = "Rotasi pemindai 90 derajat")

    Box(modifier.fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),

            modifier = Modifier.fillMaxWidth().offset(y = (-20).dp)) {

            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {

                Box(Modifier.size(96.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .09f), RoundedCornerShape(26.dp))

                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = .3f), RoundedCornerShape(26.dp)),

                    contentAlignment = Alignment.Center) { ScanGlyph("target", MaterialTheme.colorScheme.primary, Modifier.size(36.dp).rotate(rotation)) }

            }

            Text("Memproses Struk...", style = MaterialTheme.typography.titleLarge, fontSize = 20.sp)

            Text("AI sedang membaca dan mengekstrak data dari\nstruk kamu", style = MaterialTheme.typography.bodyMedium,

                textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(2.dp))

            LinearProgressIndicator(progress = { scan.progress / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp),

                gapSize = 0.dp, drawStopIndicator = {}, trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))

            Text("Mengenali teks... ${scan.progress}%", fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFF94A3B8),

                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })

        }

    }

}



@Composable

private fun ReceiptResult(scan: ReceiptScanViewModel, finance: FinanceViewModel, onEdit: () -> Unit, onSplit: () -> Unit, onSaved: (String) -> Unit) {

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .08f), shape = CircleShape,

                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .25f))) {

                Text("◎ Akurasi 94%", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, lineHeight = 16.sp,

                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

            }

            Text("Hasil pemindaian", fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        }

        ReceiptPanel {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {

                    Text(scan.merchant, style = MaterialTheme.typography.titleMedium)

                    Text(LocalDate.parse(scan.date).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))) + " · 12:28",

                        fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                }

                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(28.dp)) {

                    Text("Edit", fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFFE6A822))

                }

            }

        }

        ReceiptPanel {

            Text("ITEM (${scan.items.size})", fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFF94A3B8), letterSpacing = .5.sp)

            scan.items.forEachIndexed { index, item ->

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Column(Modifier.weight(1f)) {

                        Text(item.name, fontSize = 12.sp, lineHeight = 16.sp)

                        Text("x${item.quantity}", fontSize = 10.sp, lineHeight = 12.sp, color = Color(0xFF94A3B8))

                    }

                    Text(rupiah(item.total), fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp)

                }

                if (index < scan.items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))

            }

            MoneyRow("Subtotal", scan.subtotal)

            MoneyRow("PPN 10%", scan.tax)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MoneyRow("Total", scan.total, true)

        }

        ReceiptPanel {

            Text("PENGATURAN TRANSAKSI", fontSize = 10.sp, lineHeight = 14.sp, color = Color(0xFF94A3B8), letterSpacing = .5.sp)

            val available = finance.budgetsFor(YearMonth.from(LocalDate.parse(scan.date)))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                finance.expenseCategories(scan.date).map { it.category }.forEach { choice ->

                    val source = if (choice == "Belanja") "Lainnya" else choice

                    val label = if (choice == "Belanja") choice else available.find { it.sourceCategory == source }?.category ?: choice

                    FilterChip(selected = scan.category == choice || available.any{it.category==choice && it.sourceCategory==scan.category}, onClick = { scan.category = choice },

                        label = { Text(label, fontSize = 12.sp, lineHeight = 16.sp) }, shape = RoundedCornerShape(8.dp),

                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .13f),

                            selectedLabelColor = MaterialTheme.colorScheme.primary))

                }

            }

            FigmaInput(scan.note, { scan.note = it }, "Catatan", "Tambah catatan...", error = if (scan.note.length > 240) "Catatan maksimal 240 karakter." else null)

        }

        scan.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

        GradientButton(onClick = { scan.save(finance)?.let(onSaved) }, modifier = Modifier.fillMaxWidth(), enabled = scan.savedId == null) {

            Text("Simpan Transaksi")

        }

        OutlinedButton(onClick = onSplit, modifier = Modifier.fillMaxWidth().height(46.dp), shape = MaterialTheme.shapes.small,

            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .05f)),

            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .3f))) { Text("Split Bill dari Struk Ini", fontSize = 13.sp) }

    }

}



@Composable

private fun ReceiptPanel(content: @Composable ColumnScope.() -> Unit) {

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface,

        shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {

        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)

    }

}



@Composable

private fun MoneyRow(label: String, amount: Long, bold: Boolean = false) {

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

        Text(label, fontSize = if (bold) 14.sp else 12.sp, lineHeight = if (bold) 20.sp else 16.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,

            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)

        Text(rupiah(amount), fontFamily = JetBrainsMono, fontSize = if (bold) 14.sp else 12.sp, lineHeight = if (bold) 20.sp else 16.sp,

            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,

            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)

    }

}



@Composable

private fun ScanGlyph(kind: String, color: Color, modifier: Modifier = Modifier.size(24.dp)) {

    Canvas(modifier) {

        val unit = size.width / 24f

        val stroke = Stroke(1.7f * unit, cap = StrokeCap.Round)

        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x * unit, y * unit), Offset(x2 * unit, y2 * unit), 1.7f * unit, StrokeCap.Round)

        when (kind) {

            "camera" -> {

                val path = Path().apply { moveTo(3*unit,7*unit); lineTo(7*unit,7*unit); lineTo(9*unit,4*unit); lineTo(15*unit,4*unit); lineTo(17*unit,7*unit); lineTo(21*unit,7*unit); lineTo(21*unit,20*unit); lineTo(3*unit,20*unit); close() }

                drawPath(path, color, style = stroke)

                drawCircle(color, 4*unit, Offset(12*unit,13*unit), style = stroke)

            }

            "upload" -> { line(12f,16f,12f,3f); line(8f,7f,12f,3f); line(16f,7f,12f,3f); line(5f,14f,5f,21f); line(5f,21f,19f,21f); line(19f,21f,19f,14f) }

            "sun" -> {

                drawCircle(color, 3.5f*unit, center, style = stroke)

                repeat(8) { i -> val angle = i * Math.PI / 4; val dx = kotlin.math.cos(angle).toFloat(); val dy = kotlin.math.sin(angle).toFloat()

                    drawLine(color, center + Offset(dx*7*unit,dy*7*unit), center + Offset(dx*9*unit,dy*9*unit), 1.2f*unit)

                }

            }

            else -> { drawCircle(color.copy(alpha = .25f),5*unit,center); drawCircle(color,5*unit,center,style=stroke)

                line(12f,2f,12f,7f); line(12f,17f,12f,22f); line(2f,12f,7f,12f); line(17f,12f,22f,12f) }

        }

    }

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

private fun ReceiptEditor(scan: ReceiptScanViewModel, onDismiss: () -> Unit) {

    var name by rememberSaveable { mutableStateOf(scan.merchant) }

    var date by rememberSaveable { mutableStateOf(scan.date) }

    var quantities by rememberSaveable { mutableStateOf(ArrayList(scan.items.map { it.quantity.toString() })) }

    var prices by rememberSaveable { mutableStateOf(ArrayList(scan.items.map { it.unitPrice.toString() })) }

    var submitted by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),

        containerColor = MaterialTheme.colorScheme.surface) {

        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),

            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Text("Edit Hasil Scan", style = MaterialTheme.typography.headlineSmall)

            FigmaInput(name, { name = it }, "Nama toko", error = if (submitted) TransactionValidation.title(name) else null)

            FigmaInput(date, { date = it }, "Tanggal struk", "YYYY-MM-DD", error = if (submitted) TransactionValidation.date(date) else null)

            scan.items.forEachIndexed { index, item ->

                Text(item.name, style = MaterialTheme.typography.titleSmall)

                FigmaInput(quantities[index], { value -> quantities = ArrayList(quantities).apply { set(index, value) } }, "Jumlah ${item.name}",

                    keyboard = KeyboardType.Number, error = if (submitted && (quantities[index].toIntOrNull() ?: 0) !in 1..99) "Jumlah harus 1–99." else null)

                FigmaInput(prices[index], { value -> prices = ArrayList(prices).apply { set(index, value) } }, "Harga satuan ${item.name}",

                    keyboard = KeyboardType.Number, error = if (submitted) TransactionValidation.amount(prices[index]) else null)

            }

            GradientButton(onClick = {

                submitted = true

                val revised = scan.items.mapIndexed { index, item -> item.copy(quantity = quantities[index].toIntOrNull() ?: 0, unitPrice = prices[index].toLongOrNull() ?: 0) }

                if (scan.updateReceipt(name, date, revised)) onDismiss()

            }, modifier = Modifier.fillMaxWidth()) { Text("Terapkan Perubahan") }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Batal") }

        }

    }

}



