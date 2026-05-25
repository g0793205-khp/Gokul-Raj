package com.example.model

import androidx.compose.ui.graphics.Color

data class CandleStick(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float,
    val timestamp: Long,
    val detectedPattern: String? = null
)

enum class IndicatorType(val label: String, val color: Color) {
    SMA("SMA (14)", Color(0xFF2196F3)),
    EMA("EMA (14)", Color(0xFFFF9800)),
    BOLLINGER("Bollinger Bands", Color(0x334CAF50)), // Greenish channel
    RSI("RSI (14)", Color(0xFF9C27B0)),
    MACD("MACD (12, 26, 9)", Color(0xFF00BCD4))
}

enum class TradingPair(
    val symbol: String,
    val displayName: String,
    val basePrice: Float,
    val volatility: Float,
    val description: String
) {
    BTC_USDT("BTC/USDT", "Bitcoin", 68500f, 450f, "Crypto giant leading international market trends."),
    ETH_USDT("ETH/USDT", "Ethereum", 3450f, 45f, "Smart contract ledger displaying moderate volatility."),
    AAPL("AAPL", "Apple Inc.", 182.5f, 1.2f, "US blue-chip tech stock with solid structural trendlines."),
    EUR_USD("EUR/USD", "EUR/USD Forex", 1.085f, 0.0015f, "Most traded global currency pair showing sharp trends.")
}

enum class MarketBias(val displayName: String, val color: Color) {
    BULLISH("Strong Bullish", Color(0xFF4CAF50)),
    MODERATE_BULLISH("Bullish Pivot", Color(0xFF8BC34A)),
    NEUTRAL("Neutral Consolidation", Color(0xFF9E9E9E)),
    MODERATE_BEARISH("Bearish Shift", Color(0xFFF44336)),
    BEARISH("Strong Bearish", Color(0xFFD32F2F))
}

data class TradeSignal(
    val bias: MarketBias,
    val score: Int, // 0 to 100
    val reason: String,
    val entryRange: String,
    val targetLevel: String,
    val stopLoss: String,
    val patternDiagnostics: String,
    val rsiValue: Float,
    val macdSignal: String
)

data class ChartPatternTemplate(
    val id: String,
    val name: String,
    val imageUrl: String, // Predefined web screenshots of standard trading structures
    val description: String,
    val patternClass: String
)
