package com.example.data.repository

import com.example.data.model.MonthlyReport
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MonthlyReportService {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private fun requireOwner() {
        check(FirebaseAuth.getInstance().currentUser != null) { "Please sign in to view monthly reports." }
    }

    suspend fun listReports(barId: String): List<MonthlyReport> {
        requireOwner()
        return firestore.collection("monthlyReports")
            .document(barId)
            .collection("reports")
            .orderBy("period", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .map { doc ->
                MonthlyReport(
                    id = doc.id,
                    barId = barId,
                    period = doc.getString("period") ?: doc.id,
                    generatedAt = doc.getLong("generatedAt") ?: 0L,
                    pdfUrl = doc.getString("pdfUrl") ?: "",
                    csvUrl = doc.getString("csvUrl") ?: "",
                    expectedSales = doc.getDouble("expectedSales") ?: 0.0,
                    submittedCash = doc.getDouble("submittedCash") ?: 0.0,
                    variance = doc.getDouble("variance") ?: 0.0,
                    salaryDeductionReview = doc.getDouble("salaryDeductionReview") ?: 0.0,
                    status = doc.getString("status") ?: "READY"
                )
            }
    }
}
