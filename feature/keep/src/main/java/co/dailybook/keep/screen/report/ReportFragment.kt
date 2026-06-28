package co.dailybook.keep.screen.report

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import co.dailybook.base.BaseFragment
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.utils.PdfGenerator
import co.dailybook.keep.R
import co.dailybook.keep.databinding.FragmentReportBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.util.Locale

class ReportFragment : BaseFragment<FragmentReportBinding>() {

    override val screenName: String
        get() = "Report"
        
    private var staffName: String = ""
    private var staffPhone: String = ""
    private var monthYear: String = ""
    private var presentCount: Int = 0
    private var absentCount: Int = 0
    private var overtimeCount: Double = 0.0
    private var halfdayCount: Int = 0
    private var pPlusHalf: String = "-"
    private var pPlusP: String = "-"
    private var advanceAmount: Double = 0.0
    private var totalEarnings: Double = 0.0

    companion object {
        private const val ARG_STAFF_NAME = "staff_name"
        private const val ARG_STAFF_PHONE = "staff_phone"
        private const val ARG_MONTH_YEAR = "month_year"
        private const val ARG_PRESENT_COUNT = "present_count"
        private const val ARG_ABSENT_COUNT = "absent_count"
        private const val ARG_OVERTIME_COUNT = "overtime_count"
        private const val ARG_HALFDAY_COUNT = "halfday_count"
        private const val ARG_P_PLUS_HALF = "p_plus_half"
        private const val ARG_P_PLUS_P = "p_plus_p"
        private const val ARG_ADVANCE_AMOUNT = "advance_amount"
        private const val ARG_TOTAL_EARNINGS = "total_earnings"

        fun newInstance(
            staffName: String,
            staffPhone: String,
            monthYear: String,
            presentCount: Int,
            absentCount: Int,
            overtimeCount: Double,
            halfdayCount: Int,
            pPlusHalf: String = "-",
            pPlusP: String = "-",
            advanceAmount: Double = 0.0,
            totalEarnings: Double = 0.0
        ): ReportFragment = ReportFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_STAFF_NAME, staffName)
                putString(ARG_STAFF_PHONE, staffPhone)
                putString(ARG_MONTH_YEAR, monthYear)
                putInt(ARG_PRESENT_COUNT, presentCount)
                putInt(ARG_ABSENT_COUNT, absentCount)
                putDouble(ARG_OVERTIME_COUNT, overtimeCount)
                putInt(ARG_HALFDAY_COUNT, halfdayCount)
                putString(ARG_P_PLUS_HALF, pPlusHalf)
                putString(ARG_P_PLUS_P, pPlusP)
                putDouble(ARG_ADVANCE_AMOUNT, advanceAmount)
                putDouble(ARG_TOTAL_EARNINGS, totalEarnings)
            }
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentReportBinding? {
        return FragmentReportBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        registerOnClickListeners()
        populateData()
    }

    private fun setupView() {
        // View setup is handled in populateData()
    }

    private fun populateData() {
        arguments?.let { args ->
            // Store data in class variables
            staffName = args.getString(ARG_STAFF_NAME, "N/A")
            staffPhone = args.getString(ARG_STAFF_PHONE, "N/A")
            monthYear = args.getString(ARG_MONTH_YEAR, "Monthly Report")
            presentCount = args.getInt(ARG_PRESENT_COUNT, 0)
            absentCount = args.getInt(ARG_ABSENT_COUNT, 0)
            overtimeCount = args.getDouble(ARG_OVERTIME_COUNT, 0.0)
            halfdayCount = args.getInt(ARG_HALFDAY_COUNT, 0)
            pPlusHalf = args.getString(ARG_P_PLUS_HALF, "-")
            pPlusP = args.getString(ARG_P_PLUS_P, "-")
            advanceAmount = args.getDouble(ARG_ADVANCE_AMOUNT, 0.0)
            totalEarnings = args.getDouble(ARG_TOTAL_EARNINGS, 0.0)
            
            binding?.apply {
                // Staff Information
                tvReportNameValue.text = staffName
                tvReportPhone.text = staffPhone
                
                // Report Header
                tvReportMonth.text = monthYear
                
                // Attendance Data
                tvPresentCount.text = presentCount.toString()
                tvAbsentCount.text = absentCount.toString()
                tvOvertimeCount.text = formatOvertime(overtimeCount)
                tvHalfdayCount.text = halfdayCount.toString()
                tvPPlusHalf.text = pPlusHalf
                tvPPlusP.text = pPlusP
                
                // Payment Data
                tvAdvanceAmount.text = formatCurrency(advanceAmount)
                tvTotalEarnings.text = formatCurrency(totalEarnings)
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }

    private fun formatOvertime(overtime: Double): String {
        return if (overtime > 0) {
            // If the value is already in hours (e.g., 9.0 for 9 hours)
            if (overtime >= 1) {
                val hours = overtime.toInt()
                val remainingMinutes = ((overtime - hours) * 60).toInt()
                if (remainingMinutes > 0) {
                    "${hours}h${remainingMinutes}m"
                } else {
                    "${hours}h"
                }
            } else {
                // If the value is less than 1, it might be in hours (e.g., 0.5 for 30 minutes)
                val totalMinutes = (overtime * 60).toInt()
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
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            ivBack.setOnClickListener {
                fragmentNavigator.goBack()
            }

            btnSharePdf.setOnClickListener {
                recordClickEvent(ConstantEventNames.SHARE_PDF_REPORT)
                generateAndSharePdf()
            }

            btnWhatsapp.setOnClickListener {
                recordClickEvent(ConstantEventNames.SHARE_PDF_REPORT, hashMapOf(Pair("channel", "whatsapp")))
                generateAndShareViaWhatsApp()
            }

            btnExportCsv.setOnClickListener {
                recordClickEvent(ConstantEventNames.SHARE_PDF_REPORT, hashMapOf(Pair("channel", "csv")))
                exportCsv()
            }
        }
    }
    
    private fun generateAndSharePdf() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show loading
                binding?.btnSharePdf?.isEnabled = false
                binding?.btnSharePdf?.text = getString(R.string.generating_pdf)
                
                // Generate PDF in background
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfGenerator.generateStaffReport(
                        context = requireContext(),
                        staffName = staffName,
                        staffPhone = staffPhone,
                        monthYear = monthYear,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        overtimeCount = overtimeCount,
                        halfdayCount = halfdayCount,
                        pPlusHalf = pPlusHalf,
                        pPlusP = pPlusP,
                        advanceAmount = advanceAmount,
                        totalEarnings = totalEarnings
                    )
                }
                
                // Check if PDF generation was successful
                if (pdfFile != null && pdfFile.exists() && pdfFile.canRead()) {
                    // Share the PDF
                    sharePdfFile(pdfFile)
                } else {
                    // PDF generation failed
                    Toast.makeText(requireContext(), getString(R.string.failed_to_generate_pdf, "PDF generation failed"), Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e("ReportFragment", "Error generating PDF: ${e.message}", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_generate_pdf, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                // Reset button state
                binding?.btnSharePdf?.isEnabled = true
                binding?.btnSharePdf?.text = getString(R.string.share_pdf)
            }
        }
    }
    
    private fun generateAndShareViaWhatsApp() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                binding?.btnWhatsapp?.isEnabled = false
                binding?.btnWhatsapp?.text = getString(R.string.generating_pdf)

                val pdfFile = withContext(Dispatchers.IO) {
                    PdfGenerator.generateStaffReport(
                        context = requireContext(),
                        staffName = staffName,
                        staffPhone = staffPhone,
                        monthYear = monthYear,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        overtimeCount = overtimeCount,
                        halfdayCount = halfdayCount,
                        pPlusHalf = pPlusHalf,
                        pPlusP = pPlusP,
                        advanceAmount = advanceAmount,
                        totalEarnings = totalEarnings
                    )
                }

                if (pdfFile != null && pdfFile.exists()) {
                    val uri = FileProvider.getUriForFile(requireContext(), "co.dailybook.provider", pdfFile)
                    val summary = getString(R.string.whatsapp_report_summary, staffName, monthYear, presentCount, formatCurrency(totalEarnings))
                    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, summary)
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(whatsappIntent)
                    } catch (e: ActivityNotFoundException) {
                        // WhatsApp not installed — fall back to generic share
                        val fallback = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, summary)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(fallback, getString(R.string.share_pdf_report_title)))
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.failed_to_generate_pdf, "PDF generation failed"), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ReportFragment", "WhatsApp share error: ${e.message}", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_generate_pdf, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding?.btnWhatsapp?.isEnabled = true
                binding?.btnWhatsapp?.text = getString(R.string.share_on_whatsapp)
            }
        }
    }

    private fun exportCsv() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                binding?.btnExportCsv?.isEnabled = false
                binding?.btnExportCsv?.text = getString(R.string.exporting_csv)

                val csvFile = withContext(Dispatchers.IO) {
                    val fileName = "report_${staffName.replace(" ", "_")}_${monthYear.replace(" ", "_")}.csv"
                    val file = File(requireContext().cacheDir, fileName)
                    FileWriter(file).use { writer ->
                        writer.append("Name,Phone,Month,Present,Absent,Half Day,Overtime,P+½,P+P,Advance,Total Earnings\n")
                        writer.append("\"$staffName\",\"$staffPhone\",\"$monthYear\",$presentCount,$absentCount,$halfdayCount,\"${formatOvertime(overtimeCount)}\",\"$pPlusHalf\",\"$pPlusP\",${advanceAmount.toLong()},${totalEarnings.toLong()}\n")
                    }
                    file
                }

                val uri = FileProvider.getUriForFile(requireContext(), "co.dailybook.provider", csvFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.staff_report_subject, staffName, monthYear))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.export_csv)))

            } catch (e: Exception) {
                Log.e("ReportFragment", "CSV export error: ${e.message}", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_export_csv, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding?.btnExportCsv?.isEnabled = true
                binding?.btnExportCsv?.text = getString(R.string.export_csv)
            }
        }
    }

    private fun sharePdfFile(pdfFile: File) {
        try {
            // Validate file before sharing
            if (!pdfFile.exists()) {
                Toast.makeText(requireContext(), getString(R.string.failed_to_share_pdf, "PDF file not found"), Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!pdfFile.canRead()) {
                Toast.makeText(requireContext(), getString(R.string.failed_to_share_pdf, "Cannot read PDF file"), Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "co.dailybook.provider",
                pdfFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.staff_report_subject, staffName, monthYear))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.staff_report_message, staffName, monthYear))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_pdf_report_title)))
            
        } catch (e: Exception) {
            Log.e("ReportFragment", "Error sharing PDF: ${e.message}", e)
            Toast.makeText(requireContext(), getString(R.string.failed_to_share_pdf, e.message), Toast.LENGTH_SHORT).show()
        }
    }
} 