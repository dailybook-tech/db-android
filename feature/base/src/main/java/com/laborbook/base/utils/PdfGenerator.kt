package com.laborbook.base.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.action.PdfAction
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Link
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.laborbook.base.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

class PdfGenerator {
    
    companion object {
        private const val TAG = "PdfGenerator"
        
        fun generateStaffReport(
            context: Context,
            staffName: String,
            staffPhone: String,
            monthYear: String,
            presentCount: Int,
            absentCount: Int,
            overtimeCount: Double,
            halfdayCount: Int,
            pPlusHalf: String,
            pPlusP: String,
            advanceAmount: Double,
            totalEarnings: Double
        ): File? {
            
            // Create English locale context for PDF generation
            val englishContext = context.createConfigurationContext(
                context.resources.configuration.apply {
                    setLocale(Locale.ENGLISH)
                }
            )
            
            var writer: PdfWriter? = null
            var pdf: PdfDocument? = null
            var document: Document? = null
            var file: File? = null
            
            try {
                // Validate input parameters
                if (staffName.isBlank()) {
                    Log.e(TAG, "Staff name is blank")
                    return null
                }
                
                if (monthYear.isBlank()) {
                    Log.e(TAG, "Month year is blank")
                    return null
                }
                
                // Create file with safe filename
                val safeStaffName = staffName.replace(Regex("[^a-zA-Z0-9\\s_-]"), "_")
                val safeMonthYear = monthYear.replace(Regex("[^a-zA-Z0-9\\s_-]"), "_")
                val fileName = "Staff_Report_${safeStaffName.replace(" ", "_")}_$safeMonthYear.pdf"
                file = File(context.cacheDir, fileName)
                
                // Ensure cache directory exists
                if (!context.cacheDir.exists()) {
                    context.cacheDir.mkdirs()
                }
                
                // Create PDF writer with exception handling
                try {
                    writer = PdfWriter(FileOutputStream(file))
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to create PDF writer: ${e.message}", e)
                    return null
                }
                
                // Create PDF document
                try {
                    pdf = PdfDocument(writer)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create PDF document: ${e.message}", e)
                    return null
                }
                
                // Create document
                try {
                    document = Document(pdf)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create document: ${e.message}", e)
                    return null
                }
                
                // Set page margins
                document.setMargins(50f, 50f, 50f, 50f)
                
                // Add title - matching the screenshot style
                try {
                    val title = Paragraph("$monthYear Report")
                        .setFontSize(24f)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30f)
                    document.add(title)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add title: ${e.message}", e)
                }
                
                // Add staff information section
                try {
                    val staffInfoTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                        .setMarginBottom(25f)
                        .setWidth(UnitValue.createPercentValue(100f))
                    
                    staffInfoTable.addCell(createInfoCell(englishContext.getString(R.string.name), true))
                    staffInfoTable.addCell(createInfoCell(staffName, false))
                    staffInfoTable.addCell(createInfoCell(englishContext.getString(R.string.phone_number_label), true))
                    staffInfoTable.addCell(createInfoCell(staffPhone, false))
                    
                    document.add(staffInfoTable)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add staff information: ${e.message}", e)
                }
                
                // Add attendance summary section with bold title
                try {
                    val attendanceTitle = Paragraph(englishContext.getString(R.string.attendance_summary))
                        .setFontSize(20f)
                        .setBold()
                        .setMarginBottom(15f)
                    document.add(attendanceTitle)
                    
                    // Create attendance table with better formatting
                    val attendanceTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                        .setMarginBottom(25f)
                        .setWidth(UnitValue.createPercentValue(100f))
                        .setBorder(null)
                    
                    // Add attendance data with bold labels
                    attendanceTable.addCell(createAttendanceCell("${englishContext.getString(R.string.present_label)} (${englishContext.getString(R.string.p)})", presentCount.toString(), true))
                    attendanceTable.addCell(createAttendanceCell("${englishContext.getString(R.string.absent_label)} (${englishContext.getString(R.string.a)})", absentCount.toString(), true))
                    
                    // Convert overtime to hours format
                    val overtimeHours = if (overtimeCount > 0) {
                        // If the value is already in hours (e.g., 9.0 for 9 hours)
                        if (overtimeCount >= 1) {
                            val hours = overtimeCount.toInt()
                            val remainingMinutes = ((overtimeCount - hours) * 60).toInt()
                            if (remainingMinutes > 0) {
                                "${hours}h${remainingMinutes}m"
                            } else {
                                "${hours}h"
                            }
                        } else {
                            // If the value is less than 1, it might be in hours (e.g., 0.5 for 30 minutes)
                            val totalMinutes = (overtimeCount * 60).toInt()
                            val hours = totalMinutes / 60
                            val minutes = totalMinutes % 60
                            if (hours > 0) {
                                if (minutes > 0) {
                                    "${hours}h${minutes}m"
                                } else {
                                    "${hours}h"
                                }
                            } else {
                                "${minutes}m"
                            }
                        }
                    } else {
                        "0h"
                    }
                    attendanceTable.addCell(createAttendanceCell("${englishContext.getString(R.string.overtime_label)} (${englishContext.getString(R.string.ot)})", overtimeHours, true))
                    attendanceTable.addCell(createAttendanceCell("${englishContext.getString(R.string.half_day_label)} (${englishContext.getString(R.string._1_2)})", halfdayCount.toString(), true))
                    attendanceTable.addCell(createAttendanceCell(englishContext.getString(R.string.p_plus_half_label), pPlusHalf, true))
                    attendanceTable.addCell(createAttendanceCell(englishContext.getString(R.string.p_plus_p_label), pPlusP, true))
                    
                    document.add(attendanceTable)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add attendance summary: ${e.message}", e)
                }
                
                // Add payment summary section
                try {
                    val paymentTitle = Paragraph(englishContext.getString(R.string.payment_summary))
                        .setFontSize(18f)
                        .setBold()
                        .setMarginBottom(15f)
                    document.add(paymentTitle)
                    
                    val paymentTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                        .setMarginBottom(30f)
                        .setWidth(UnitValue.createPercentValue(100f))
                    
                    paymentTable.addCell(createPaymentCell(englishContext.getString(R.string.advance_amount_label), formatCurrency(advanceAmount)))
                    paymentTable.addCell(createPaymentCell(englishContext.getString(R.string.total_earnings_label), formatCurrency(totalEarnings)))
                    
                    document.add(paymentTable)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add payment summary: ${e.message}", e)
                }
                
                // Add footer section with Google Play Store link
                try {
                    val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                        .setWidth(UnitValue.createPercentValue(100f))
                        .setBackgroundColor(ColorConstants.BLUE)
                        .setMarginBottom(20f)
                    
                    footerTable.addCell(createFooterCell(englishContext.getString(R.string.report_generated_by_laborbook), true))
                    footerTable.addCell(createFooterCellWithLink(englishContext.getString(R.string.download_app_for_free_newline), "https://play.google.com/store/apps/details?id=com.laborbook", false))
                    
                    document.add(footerTable)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add footer: ${e.message}", e)
                }
                
                Log.d(TAG, "PDF generated successfully: ${file.absolutePath}")
                return file
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during PDF generation: ${e.message}", e)
                return null
            } finally {
                // Clean up resources
                try {
                    document?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing document: ${e.message}", e)
                }
                
                try {
                    pdf?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing PDF: ${e.message}", e)
                }
                
                try {
                    writer?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing writer: ${e.message}", e)
                }
                
                // If there was an error and file was created, delete it
                if (file != null && file.exists() && !file.canRead()) {
                    try {
                        file.delete()
                        Log.d(TAG, "Deleted corrupted PDF file")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting corrupted file: ${e.message}", e)
                    }
                }
            }
        }
        
        private fun createInfoCell(text: String, isLabel: Boolean): com.itextpdf.layout.element.Cell {
            return try {
                val cell = com.itextpdf.layout.element.Cell()
                    .add(Paragraph(text))
                    .setPadding(8f)
                    .setBorder(null)
                
                if (isLabel) {
                    cell.setBold()
                }
                
                cell
            } catch (e: Exception) {
                Log.e(TAG, "Error creating info cell: ${e.message}", e)
                com.itextpdf.layout.element.Cell().add(Paragraph("Error"))
            }
        }
        
        private fun createAttendanceCell(label: String, value: String, isBold: Boolean): com.itextpdf.layout.element.Cell {
            return try {
                val cell = com.itextpdf.layout.element.Cell()
                    .add(Paragraph("$label: $value"))
                    .setPadding(10f)
                    .setBorder(null)
                    .setTextAlignment(TextAlignment.LEFT)
                
                if (isBold) {
                    cell.setBold()
                }
                
                cell
            } catch (e: Exception) {
                Log.e(TAG, "Error creating attendance cell: ${e.message}", e)
                com.itextpdf.layout.element.Cell().add(Paragraph("Error"))
            }
        }
        
        private fun createPaymentCell(label: String, value: String): com.itextpdf.layout.element.Cell {
            return try {
                val cell = com.itextpdf.layout.element.Cell()
                    .add(Paragraph("$label $value"))
                    .setPadding(8f)
                    .setBorder(null)
                    .setTextAlignment(TextAlignment.LEFT)
                
                cell
            } catch (e: Exception) {
                Log.e(TAG, "Error creating payment cell: ${e.message}", e)
                com.itextpdf.layout.element.Cell().add(Paragraph("Error"))
            }
        }
        
        private fun createFooterCell(text: String, isLeft: Boolean): com.itextpdf.layout.element.Cell {
            return try {
                val cell = com.itextpdf.layout.element.Cell()
                    .add(Paragraph(text))
                    .setPadding(10f)
                    .setBorder(null)
                    .setTextAlignment(if (isLeft) TextAlignment.LEFT else TextAlignment.RIGHT)
                    .setFontColor(ColorConstants.WHITE)
                
                cell
            } catch (e: Exception) {
                Log.e(TAG, "Error creating footer cell: ${e.message}", e)
                com.itextpdf.layout.element.Cell().add(Paragraph("Error"))
            }
        }
        
        private fun createFooterCellWithLink(text: String, url: String, isLeft: Boolean): com.itextpdf.layout.element.Cell {
            return try {
                val link = Link(text, PdfAction.createURI(url))
                val paragraph = Paragraph(link)
                    .setFontColor(ColorConstants.WHITE)
                
                val cell = com.itextpdf.layout.element.Cell()
                    .add(paragraph)
                    .setPadding(10f)
                    .setBorder(null)
                    .setTextAlignment(if (isLeft) TextAlignment.LEFT else TextAlignment.RIGHT)
                
                cell
            } catch (e: Exception) {
                Log.e(TAG, "Error creating footer cell with link: ${e.message}", e)
                com.itextpdf.layout.element.Cell().add(Paragraph("Error"))
            }
        }
        
        private fun formatCurrency(amount: Double): String {
            return try {
                NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting currency: ${e.message}", e)
                "₹$amount"
            }
        }
        
        fun generatePayslip(
            context: Context,
            staffName: String,
            staffPhone: String,
            category: String,
            monthYear: String,
            presentCount: Int,
            absentCount: Int,
            halfdayCount: Int,
            pPlusHalf: String,
            pPlusP: String,
            overtimeHours: String,
            dailyRate: Double,
            overtimeAmount: Double,
            bonusAmount: Double,
            totalAdvance: Double,
            netPayable: Double
        ): File? {
            val englishContext = context.createConfigurationContext(
                context.resources.configuration.apply { setLocale(Locale.ENGLISH) }
            )

            var writer: PdfWriter? = null
            var pdf: PdfDocument? = null
            var document: Document? = null
            var file: File? = null

            try {
                val safeStaffName = staffName.replace(Regex("[^a-zA-Z0-9\\s_-]"), "_")
                val safeMonthYear = monthYear.replace(Regex("[^a-zA-Z0-9\\s_-]"), "_")
                val fileName = "Payslip_${safeStaffName.replace(" ", "_")}_$safeMonthYear.pdf"
                file = File(context.cacheDir, fileName)

                try { writer = PdfWriter(FileOutputStream(file)) } catch (e: IOException) { return null }
                try { pdf = PdfDocument(writer) } catch (e: Exception) { return null }
                try { document = Document(pdf) } catch (e: Exception) { return null }

                document.setMargins(40f, 40f, 40f, 40f)

                // Title
                document.add(
                    Paragraph("PAYSLIP")
                        .setFontSize(22f).setBold()
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4f)
                )
                document.add(
                    Paragraph(monthYear)
                        .setFontSize(14f)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20f)
                )

                // Worker info
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                    .setWidth(UnitValue.createPercentValue(100f)).setMarginBottom(16f)
                infoTable.addCell(createInfoCell("Name", true))
                infoTable.addCell(createInfoCell(staffName, false))
                infoTable.addCell(createInfoCell("Phone", true))
                infoTable.addCell(createInfoCell(staffPhone.ifEmpty { "-" }, false))
                if (category.isNotEmpty()) {
                    infoTable.addCell(createInfoCell("Category", true))
                    infoTable.addCell(createInfoCell(category, false))
                }
                document.add(infoTable)

                // Attendance section
                document.add(
                    Paragraph("Attendance")
                        .setFontSize(14f).setBold().setMarginBottom(8f)
                )
                val attTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                    .setWidth(UnitValue.createPercentValue(100f)).setMarginBottom(16f)
                attTable.addCell(createAttendanceCell("Present (P)", presentCount.toString(), true))
                attTable.addCell(createAttendanceCell("Absent (A)", absentCount.toString(), true))
                attTable.addCell(createAttendanceCell("Half Day (H)", halfdayCount.toString(), true))
                attTable.addCell(createAttendanceCell("P+½", pPlusHalf, true))
                attTable.addCell(createAttendanceCell("P+P (Double)", pPlusP, true))
                attTable.addCell(createAttendanceCell("Overtime", overtimeHours, true))
                document.add(attTable)

                // Earnings section
                document.add(
                    Paragraph("Earnings & Deductions")
                        .setFontSize(14f).setBold().setMarginBottom(8f)
                )
                val payTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
                    .setWidth(UnitValue.createPercentValue(100f)).setMarginBottom(20f)

                if (dailyRate > 0) {
                    payTable.addCell(createPaymentCell("Daily Rate", formatCurrency(dailyRate)))
                }
                if (overtimeAmount > 0) {
                    payTable.addCell(createPaymentCell("Overtime Earnings", formatCurrency(overtimeAmount)))
                }
                if (bonusAmount > 0) {
                    payTable.addCell(createPaymentCell("Bonus", formatCurrency(bonusAmount)))
                }
                if (totalAdvance > 0) {
                    payTable.addCell(createPaymentCell("Advance Deducted", "- ${formatCurrency(totalAdvance)}"))
                }
                document.add(payTable)

                // Net payable — highlighted row
                val netTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY).setMarginBottom(20f)
                val netLabelCell = com.itextpdf.layout.element.Cell()
                    .add(Paragraph("Net Payable").setBold())
                    .setPadding(10f).setBorder(null)
                val netValueCell = com.itextpdf.layout.element.Cell()
                    .add(Paragraph(formatCurrency(netPayable)).setBold())
                    .setPadding(10f).setBorder(null)
                    .setTextAlignment(TextAlignment.RIGHT)
                netTable.addCell(netLabelCell)
                netTable.addCell(netValueCell)
                document.add(netTable)

                // Footer
                val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBackgroundColor(ColorConstants.BLUE)
                footerTable.addCell(createFooterCell("Generated by DailyBook", true))
                footerTable.addCell(createFooterCellWithLink("Download on Play Store", "https://play.google.com/store/apps/details?id=com.laborbook", false))
                document.add(footerTable)

                return file
            } catch (e: Exception) {
                Log.e(TAG, "Error generating payslip: ${e.message}", e)
                return null
            } finally {
                try { document?.close() } catch (e: Exception) { }
                try { pdf?.close() } catch (e: Exception) { }
                try { writer?.close() } catch (e: Exception) { }
            }
        }

        fun captureViewAsBitmap(view: View): Bitmap? {
            return try {
                if (view.width <= 0 || view.height <= 0) {
                    Log.w(TAG, "View dimensions are invalid: ${view.width}x${view.height}")
                    return null
                }
                
                val bitmap = Bitmap.createBitmap(
                    view.width,
                    view.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing view as bitmap: ${e.message}", e)
                null
            }
        }
    }
} 