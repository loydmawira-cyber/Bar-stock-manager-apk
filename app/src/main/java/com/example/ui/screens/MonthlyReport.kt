package com.example.data.model

data class MonthlyReport(
    val id: String = "",
    val barId: String = "",
    val period: String = "",
    val generatedAt: Long = 0L,
    val pdfUrl: String = "",
    val csvUrl: String = "",
    val expectedSales: Double = 0.0,
    val submittedCash: Double = 0.0,
    val variance: Double = 0.0,
    val salaryDeductionReview: Double = 0.0,
    val status: String = "READY"
)
