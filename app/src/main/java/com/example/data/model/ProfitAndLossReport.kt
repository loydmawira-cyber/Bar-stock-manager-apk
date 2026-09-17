package com.example.data.model

data class ItemProfitability(
    val itemId: Long,
    val itemName: String,
    val category: ItemCategory,
    val weightedAvgCost: Double,
    val sellingPrice: Double,
    val profitPerUnit: Double,
    val profitMarginPct: Double,
    val unitsSold: Int,
    val salesRevenue: Double,
    val cogs: Double,
    val grossProfit: Double,
    val isLossMaking: Boolean,
    val costIsEstimated: Boolean = false // true when no purchase receipt backs this cost yet (casePrice guess)
)

enum class PnlTimeframe {
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
}

data class ProfitAndLossReport(
    val timeframe: PnlTimeframe = PnlTimeframe.THIS_MONTH,
    val startDate: Long = 0L,
    val endDate: Long = System.currentTimeMillis(),
    val salesRevenue: Double = 0.0,
    val costOfGoodsSold: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossMarginPct: Double = 0.0,
    val operatingExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val openingStockQty: Int = 0,
    val openingStockValue: Double = 0.0,
    val closingStockQty: Int = 0,
    val closingStockValue: Double = 0.0,
    val stockVarianceQty: Int = 0,
    val itemProfitabilities: List<ItemProfitability> = emptyList(),
    val lossMakingItems: List<ItemProfitability> = emptyList(),
    val itemsWithEstimatedCost: List<ItemProfitability> = emptyList() // no purchase receipt on file yet
)
