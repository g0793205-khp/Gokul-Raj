package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.components.InteractiveTradingChart
import com.example.ui.components.RsiSubChart
import com.example.viewmodel.TradingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingDashboardScreen(
    viewModel: TradingViewModel
) {
    val candles by viewModel.candles.collectAsState()
    val selectedPair by viewModel.selectedPair.collectAsState()
    val activeIndicators by viewModel.activeIndicators.collectAsState()
    val simulationRunning by viewModel.simulationRunning.collectAsState()
    val currentPrice by viewModel.currentPrice.collectAsState()
    val hoveredCandle by viewModel.hoveredCandle.collectAsState()

    val aiResult by viewModel.aiResult.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    val uploadedBitmap by viewModel.uploadedBitmap.collectAsState()
    val analyzerResult by viewModel.analyzerResult.collectAsState()
    val analyzerLoading by viewModel.analyzerLoading.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Charts & AI Scanner, 1 = Photo analysis
    val context = LocalContext.current

    val upColor = Color(0xFF00E676)
    val downColor = Color(0xFFFF1744)

    // Handlers for Camera capture and Gallery uploads
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.setUploadedBitmap(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // Decode the selected Uri into a Bitmap
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, it)
                val bitmap = android.graphics.ImageDecoder.decodeBitmap(source)
                viewModel.setUploadedBitmap(bitmap)
            } catch (e: Exception) {
                // Backward compatibility fallback for older Android versions
                @Suppress("DEPRECATION")
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                viewModel.setUploadedBitmap(bitmap)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = upColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "TRADING ANALYZER",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                if (simulationRunning) Color(0x3300E676) else Color(0x33FF9800),
                                CircleShape
                            )
                            .clickable { viewModel.toggleSimulation() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (simulationRunning) upColor else Color(0xFFFF9800), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (simulationRunning) "LIVE FEED" else "PAUSED",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0E14),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF08090D)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Tab Header Layout
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF0C0E14),
                contentColor = upColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = upColor
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Realtime Charts", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Photo CMT Scanner", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> RealtimeChartScope(
                        viewModel = viewModel,
                        candles = candles,
                        selectedPair = selectedPair,
                        activeIndicators = activeIndicators,
                        currentPrice = currentPrice,
                        hoveredCandle = hoveredCandle,
                        aiResult = aiResult,
                        aiLoading = aiLoading,
                        aiError = aiError,
                        upColor = upColor,
                        downColor = downColor
                    )
                    1 -> PhotoAnalysisScope(
                        viewModel = viewModel,
                        uploadedBitmap = uploadedBitmap,
                        analyzerResult = analyzerResult,
                        analyzerLoading = analyzerLoading,
                        onCameraClicked = { cameraLauncher.launch() },
                        onGalleryClicked = { galleryLauncher.launch("image/*") }
                    )
                }
            }
        }
    }
}

@Composable
fun RealtimeChartScope(
    viewModel: TradingViewModel,
    candles: List<CandleStick>,
    selectedPair: TradingPair,
    activeIndicators: Set<IndicatorType>,
    currentPrice: Float,
    hoveredCandle: CandleStick?,
    aiResult: TradeSignal?,
    aiLoading: Boolean,
    aiError: String?,
    upColor: Color,
    downColor: Color
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        // 1. Selector of common assets
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TradingPair.values()) { pair ->
                val isSelected = pair == selectedPair
                Box(
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isSelected) upColor else Color(0x33FFFFFF),
                            RoundedCornerShape(20.dp)
                        )
                        .background(
                            if (isSelected) Color(0x1A00E676) else Color(0xFF0C0E14),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.selectPair(pair) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            pair.symbol,
                            color = if (isSelected) upColor else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            pair.displayName,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large price badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E14)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(selectedPair.displayName, color = Color.Gray, fontSize = 12.sp)
                    Text(
                        String.format(
                            if (currentPrice > 1000) "$%,.2f" else if (currentPrice > 5) "$%,.2f" else "$%,.4f",
                            currentPrice
                        ),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0x1F2196F3), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Live Ticks Sync",
                        color = Color(0xFF2196F3),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Interactive Main Chart Renders
        InteractiveTradingChart(
            candles = candles,
            activeIndicators = activeIndicators,
            smaValues = viewModel.getSmaValues(),
            emaValues = viewModel.getEmaValues(),
            bollingerBands = viewModel.getBollingerBands(),
            onCandleSelected = { viewModel.setHoveredCandle(it) },
            selectedCandle = hoveredCandle,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Oscillators / Sub-chart block
        if (activeIndicators.contains(IndicatorType.RSI)) {
            RsiSubChart(
                rsiValues = viewModel.getRsiValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Toggles of indicators
        Text(
            "Overlay Technical Indicators",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IndicatorType.values().forEach { indicator ->
                val isActive = activeIndicators.contains(indicator)
                FilterChip(
                    selected = isActive,
                    onClick = { viewModel.toggleIndicator(indicator) },
                    label = { Text(indicator.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = indicator.color.copy(alpha = 0.35f),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF0C0E14),
                        labelColor = Color.Gray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. AI Smart scanner control and result display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10131F)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x22FFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF6200EE).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFBB86FC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "AI Real-Time Analyst",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Scans active indicators instantly",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.scanCurrentLiveChart() },
                        enabled = !aiLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = upColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        if (aiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 1.5.dp)
                        } else {
                            Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (aiLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(color = upColor, trackColor = Color.DarkGray, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Formulating target prices ...", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                } else if (aiResult != null) {
                    val result = aiResult!!
                    Column {
                        // Rating Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(result.bias.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, result.bias.color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Market Bias Sentiment", color = Color.LightGray, fontSize = 9.sp)
                                Text(
                                    result.bias.displayName,
                                    color = result.bias.color,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Bullish Confidence", color = Color.LightGray, fontSize = 9.sp)
                                Text(
                                    "${result.score}%",
                                    color = result.bias.color,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Target levels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TradeValueCard(
                                title = "Entry Zone",
                                value = result.entryRange,
                                icon = Icons.Outlined.Input,
                                modifier = Modifier.weight(1f)
                            )
                            TradeValueCard(
                                title = "Target (TP)",
                                value = result.targetLevel,
                                icon = Icons.Outlined.Gavel,
                                color = upColor,
                                modifier = Modifier.weight(1f)
                            )
                            TradeValueCard(
                                title = "Stop Loss (SL)",
                                value = result.stopLoss,
                                icon = Icons.Outlined.Shield,
                                color = downColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // diagnostics
                        Text("DIAGNOSTICS & CATALYSTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            result.reason,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("INTELLIGENT PATTERNS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            result.patternDiagnostics,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A0B0E), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap SCAN to compute technical targets & pattern signals immediately.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoAnalysisScope(
    viewModel: TradingViewModel,
    uploadedBitmap: Bitmap?,
    analyzerResult: String?,
    analyzerLoading: Boolean,
    onCameraClicked: () -> Unit,
    onGalleryClicked: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Preloaded standard templates that help demoing the photo updates
    val templates = remember {
        listOf(
            ChartPatternTemplate(
                "double_bottom",
                "Double Bottom Reversal",
                "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=300&q=80",
                "Bullish reversal structure composed of two consecutive minimum points.",
                "Hammer detected on second rebound support."
            ),
            ChartPatternTemplate(
                "bull_flag",
                "Bull Flag Consolidation",
                "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?auto=format&fit=crop&w=300&q=80",
                "Bullish breakout continuation structure following a steep markup.",
                "Hammer and bullish engulfing consolidating near support."
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E14)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "CMT LIVE PHOTO ANALYZER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    "Take a mobile photo of any trading setup or computer screen stock chart. Gemini analyzes the camera shot directly for formations & patterns.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCameraClicked,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Camera Shot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onGalleryClicked,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Template shortcuts
        Text(
            "Demo Shortcuts (Simulated Snapshots)",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEach { template ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            // Automatically mock an image loading for this template pattern!
                            // We construct a mini mock bitmap or fetch a structured representation
                            val conf = Bitmap.Config.ARGB_8888
                            val mockBitmap = Bitmap.createBitmap(300, 200, conf)
                            val canvas = android.graphics.Canvas(mockBitmap)
                            val paint = android.graphics.Paint()
                            paint.color = android.graphics.Color.DKGRAY
                            canvas.drawRect(0f, 0f, 300f, 200f, paint)
                            paint.color = android.graphics.Color.GREEN
                            paint.textSize = 20f
                            canvas.drawText(template.name, 20f, 100f, paint)

                            viewModel.setUploadedBitmap(mockBitmap)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E14)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        AsyncImage(
                            model = template.imageUrl,
                            contentDescription = template.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(template.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(template.description, color = Color.Gray, fontSize = 9.sp, maxLines = 2)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display current target photo being analyzed
        uploadedBitmap?.let { bitmap ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E14)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Target Chart Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        IconButton(
                            onClick = { viewModel.setUploadedBitmap(null) },
                            modifier = Modifier
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(30.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.analyzeUploadedPhoto() },
                        enabled = !analyzerLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        if (analyzerLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RUN CMT AI CLOUD SCAN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Photo scanning results
        if (analyzerLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF00C853))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Deconstructing chart layout shapes via Gemini API...", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        } else if (analyzerResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161C2C)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x3300C853))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FactCheck, contentDescription = null, tint = Color(0xFF00C853))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "DETAILED CANDLESTICK & INDICATOR DIAGNOSIS",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFF00C853),
                            letterSpacing = 0.5.sp
                        )
                    }

                    HorizontalDivider(color = Color(0x22FFFFFF))

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        analyzerResult!!,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TradeValueCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
        border = BorderStroke(1.dp, Color(0x11FFFFFF))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
