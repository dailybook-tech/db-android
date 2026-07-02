package co.dailybook.income.screen.reports.fragment

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.dailybook.base.BaseFragment
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.toReadableDate
import co.dailybook.income.R
import co.dailybook.income.databinding.FragmentTransactionReportsBinding
import co.dailybook.income.databinding.LayoutReportTemplateBinding
import co.dailybook.income.model.Transaction
import co.dailybook.income.screen.reports.adapter.TransactionReportsAdapter
import co.dailybook.income.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionReportsFragment : BaseFragment<FragmentTransactionReportsBinding>() {

    override val screenName: String
        get() = ConstantEventNames.INCOME_TRANSACTION_DETAILS

    private var transactions: ArrayList<Transaction> = ArrayList()
    private var filteredTransactions: ArrayList<Transaction> = ArrayList()
    private var transactionType: String = Constants.CREDIT
    private var adapter: TransactionReportsAdapter? = null
    private var selectedMonth: Int = 1
    private var selectedYear: Int = 2024
    
    private var fromDate: Calendar? = null
    private var toDate: Calendar? = null

    companion object {
        private const val ARG_TRANSACTIONS = "transactions"
        private const val ARG_TRANSACTION_TYPE = "transaction_type"
        private const val ARG_MONTH = "month"
        private const val ARG_YEAR = "year"

        fun newInstance(
            transactions: ArrayList<Transaction>,
            transactionType: String,
            month: Int,
            year: Int
        ): TransactionReportsFragment = TransactionReportsFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_TRANSACTIONS, transactions)
                putString(ARG_TRANSACTION_TYPE, transactionType)
                putInt(ARG_MONTH, month)
                putInt(ARG_YEAR, year)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            transactions = it.getParcelableArrayList<Transaction>(ARG_TRANSACTIONS) as? ArrayList<Transaction> ?: ArrayList()
            transactionType = it.getString(ARG_TRANSACTION_TYPE, Constants.CREDIT)
            selectedMonth = it.getInt(ARG_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1)
            selectedYear = it.getInt(ARG_YEAR, Calendar.getInstance().get(Calendar.YEAR))
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentTransactionReportsBinding? {
        return FragmentTransactionReportsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupRecyclerView()
        setupClickListeners()
        initializeDates()
        filterTransactions()
        setupEdgeToEdge()
    }

    private fun setupViews() {
        binding?.apply {
            tvTitle.text = if (transactionType == Constants.CREDIT) {
                getString(R.string.cash_in_reports)
            } else {
                getString(R.string.cash_out_reports)
            }
        }
    }

    private fun setupEdgeToEdge() {
        binding?.llShareButtons?.let { shareButtons ->
            ViewCompat.setOnApplyWindowInsetsListener(shareButtons) { v, insets ->
                val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.updatePadding(bottom = navBars.bottom)
                insets
            }
        }
    }

    private fun initializeDates() {
        // Set date range based on selected month and year
        fromDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1) // Calendar.MONTH is 0-based
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        toDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1) // Calendar.MONTH is 0-based
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        updateDateDisplay()
    }

    private fun updateDateDisplay() {
        binding?.apply {
            fromDate?.let {
                tvFromDate.text = formatDateForPicker(it)
            }
            toDate?.let {
                tvToDate.text = formatDateForPicker(it)
            }
        }
    }

    private fun formatDateForPicker(calendar: Calendar): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yy", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun setupRecyclerView() {
        adapter = TransactionReportsAdapter()
        binding?.rvTransactions?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rvTransactions?.adapter = adapter
    }

    private fun setupClickListeners() {
        binding?.apply {
            ivBack.setOnClickListener {
                fragmentNavigator.goBack()
            }

//            tvFromDate.setOnClickListener {
//                openFromDatePicker()
//            }
//
//            tvToDate.setOnClickListener {
//                openToDatePicker()
//            }

            btnSharePdf.setOnClickListener {
                recordClickEvent(ConstantEventNames.SHARE_PDF_REPORT)
                generateAndSharePdf()
            }

            btnShareWhatsapp.setOnClickListener {
                recordClickEvent(ConstantEventNames.SHARE_WHATSAPP_REPORT)
                shareOnWhatsApp()
            }
        }
    }

    private fun openFromDatePicker() {
        val calendar = fromDate ?: Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                fromDate = calendar
                updateDateDisplay()
                filterTransactions()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            toDate?.let {
                datePicker.maxDate = it.timeInMillis
            }
        }.show()
    }

    private fun openToDatePicker() {
        val calendar = toDate ?: Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                toDate = calendar
                updateDateDisplay()
                filterTransactions()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            fromDate?.let {
                datePicker.minDate = it.timeInMillis
            }
        }.show()
    }

    private fun filterTransactions() {
        if (fromDate == null || toDate == null) return

        filteredTransactions = transactions.filter { transaction ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                val transactionDate = sdf.parse(transaction.date)
                transactionDate?.let { date ->
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val from = fromDate!!.clone() as Calendar
                    from.set(Calendar.HOUR_OF_DAY, 0)
                    from.set(Calendar.MINUTE, 0)
                    from.set(Calendar.SECOND, 0)
                    from.set(Calendar.MILLISECOND, 0)

                    val to = toDate!!.clone() as Calendar
                    to.set(Calendar.HOUR_OF_DAY, 23)
                    to.set(Calendar.MINUTE, 59)
                    to.set(Calendar.SECOND, 59)
                    to.set(Calendar.MILLISECOND, 999)

                    cal.timeInMillis >= from.timeInMillis && cal.timeInMillis <= to.timeInMillis
                } ?: false
            } catch (e: Exception) {
                false
            }
        } as ArrayList<Transaction>

        adapter?.submitList(filteredTransactions)
    }

    private fun generateAndSharePdf() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                binding?.btnSharePdf?.isEnabled = false
                (binding?.btnSharePdf as? Button)?.text = getString(R.string.generating_pdf)

                val pdfFile: File? = withContext(Dispatchers.IO) {
                    captureFormattedReportScreenshot()?.let { bitmap ->
                        convertBitmapToPdf(bitmap)
                    }
                }

                if (pdfFile != null && pdfFile.exists() && pdfFile.canRead()) {
                    sharePdfFile(pdfFile)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("TransactionReportsFragment", "Error generating PDF: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding?.btnSharePdf?.isEnabled = true
                (binding?.btnSharePdf as? Button)?.text = getString(R.string.share_pdf)
            }
        }
    }

    private fun sharePdfFile(pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Transaction Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (e: Exception) {
            Log.e("TransactionReportsFragment", "Error sharing PDF: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareOnWhatsApp() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val screenshotUri = withContext(Dispatchers.IO) {
                    captureFormattedReportScreenshot()?.let { bitmap ->
                        saveBitmapToMediaStore(bitmap)
                    }
                }

                if (screenshotUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, screenshotUri)
                        putExtra(Intent.EXTRA_TEXT, buildReportText())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }
            startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
                if (e.message?.contains("No Activity found") == true || e.message?.contains("ActivityNotFoundException") == true) {
            Toast.makeText(requireContext(), "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buildReportText(): String {
        // Use consistent refer friend message for all WhatsApp shares
        return getString(co.dailybook.base.R.string.refer_friend_whatsapp_message)
    }

    private suspend fun createFormattedReportView(): View? {
        return try {
            val reportView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_report_template, null)
            val reportBinding = LayoutReportTemplateBinding.bind(reportView)

            // Get user data
            val userName = withContext(Dispatchers.IO) {
                dataStoreManager.read(DataStoreManager.USER_NAME, "").first()
            }
            val userPhone = withContext(Dispatchers.IO) {
                dataStoreManager.read(DataStoreManager.MOBILE_NUMBER, "").first()
            }

            // Set user info
            reportBinding.tvNameLabel.text = "Name: $userName"
            reportBinding.tvPhoneLabel.text = "Phone number: $userPhone"

            // Add transaction items
            val itemsContainer = reportBinding.llTransactionItems
            itemsContainer.removeAllViews()

        var totalAmount = 0.0
        filteredTransactions.forEach { transaction ->
                val itemView = createTransactionItemView(transaction)
                itemsContainer.addView(itemView)
                totalAmount += transaction.amount
            }

            // Set total label based on transaction type
            val totalLabel = if (transactionType == Constants.CREDIT) {
                getString(R.string.total_in) // Total Cash In
            } else {
                getString(R.string.total_out) // Total Cash Out
            }
            reportBinding.tvTotalLabel.text = totalLabel

            // Set total amount
            val totalFormatted = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalAmount)
            reportBinding.tvTotalAmount.text = totalFormatted

            // Ensure marketing footer text is fully white
            val whiteColor = ContextCompat.getColor(requireContext(), co.dailybook.boilerplate.uikit.R.color.white)
            reportBinding.tvReportGeneratedBy.setTextColor(whiteColor)
            reportBinding.tvDownloadApp.setTextColor(whiteColor)

            // Set click listener for marketing footer
            reportBinding.llMarketingFooter.setOnClickListener {
                openPlayStore()
            }

            // Measure and layout the view
            reportView.measure(
                View.MeasureSpec.makeMeasureSpec(
                    resources.displayMetrics.widthPixels,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            reportView.layout(0, 0, reportView.measuredWidth, reportView.measuredHeight)

            reportView
        } catch (e: Exception) {
            Log.e("TransactionReportsFragment", "Error creating report view: ${e.message}", e)
            null
        }
    }

    private fun createTransactionItemView(transaction: Transaction): View {
        val itemView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 0)
        }

        val cellHeight = resources.getDimensionPixelSize(co.dailybook.boilerplate.uikit.R.dimen.margin_48)
        val cellPadding = resources.getDimensionPixelSize(co.dailybook.boilerplate.uikit.R.dimen.margin_8)

        // Date - with right and bottom border
        val dateView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, cellHeight, 0.6f).apply {
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            }
            text = transaction.date.toReadableDate()
            textSize = 14f
            setPadding(cellPadding, 0, cellPadding, 0)
            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            minHeight = cellHeight
            background = ContextCompat.getDrawable(requireContext(), R.drawable.table_cell_border_right_bottom)
        }

        // Notes - with right and bottom border
        val notesView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, cellHeight, 1.5f).apply {
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            }
            text = transaction.reason
            textSize = 14f
            setPadding(cellPadding, 0, cellPadding, 0)
            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            minHeight = cellHeight
            background = ContextCompat.getDrawable(requireContext(), R.drawable.table_cell_border_right_bottom)
        }

        // Amount - with only bottom border
        val amountView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, cellHeight, 1.2f).apply {
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            }
            text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(transaction.amount)
            textSize = 14f
            setPadding(cellPadding, 0, cellPadding, 0)
            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            minHeight = cellHeight
            background = ContextCompat.getDrawable(requireContext(), R.drawable.table_cell_border_bottom)
        }

        itemView.addView(dateView)
        itemView.addView(notesView)
        itemView.addView(amountView)

        return itemView
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=co.dailybook"))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=co.dailybook"))
            startActivity(intent)
        }
    }

    private suspend fun captureFormattedReportScreenshot(): Bitmap? {
        return try {
            val reportView = createFormattedReportView() ?: return null
            
            withContext(Dispatchers.Main) {
                if (reportView.width > 0 && reportView.height > 0) {
                    val bitmap = Bitmap.createBitmap(reportView.width, reportView.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    reportView.draw(canvas)
                    bitmap
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("TransactionReportsFragment", "Error capturing screenshot: ${e.message}", e)
            null
        }
    }

    private fun View.getBitmapWithoutChanges(): Bitmap? {
        return try {
            if (width <= 0 || height <= 0) return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e("TransactionReportsFragment", "Error getting bitmap: ${e.message}", e)
            null
        }
    }

    private fun RecyclerView.getBitmapFromRecyclerView(): Bitmap? {
        return try {
            val adapter = this.adapter ?: return null
            val itemCount = adapter.itemCount
            if (itemCount == 0) return null
            
            val paint = Paint()
            var height = 0
            val width = this.width
            val backgroundColor = (this.background as? ColorDrawable)?.color ?: Color.WHITE

            val bitmaps = mutableListOf<Bitmap>()
            for (i in 0 until itemCount) {
                val holder = adapter.createViewHolder(this, adapter.getItemViewType(i))
                adapter.onBindViewHolder(holder, i)
                holder.itemView.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                holder.itemView.layout(0, 0, holder.itemView.measuredWidth, holder.itemView.measuredHeight)

                val itemBitmap = Bitmap.createBitmap(holder.itemView.width, holder.itemView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(itemBitmap)
                canvas.drawColor(backgroundColor)
                holder.itemView.draw(canvas)
                bitmaps.add(itemBitmap)
                height += holder.itemView.height
            }

            val combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val combinedCanvas = Canvas(combinedBitmap)
            combinedCanvas.drawColor(backgroundColor)
            
            var top = 0
            for (bitmap in bitmaps) {
                combinedCanvas.drawBitmap(bitmap, 0f, top.toFloat(), paint)
                top += bitmap.height
            }
            combinedBitmap
        } catch (e: Exception) {
            Log.e("TransactionReportsFragment", "Error capturing RecyclerView: ${e.message}", e)
            null
        }
    }

    private fun combineBitmapsWithBackground(backgroundColorRes: Int, vararg bitmaps: Bitmap): Bitmap {
        val backgroundColor = ContextCompat.getColor(requireContext(), backgroundColorRes)
        val width = bitmaps[0].width
        val totalHeight = bitmaps.sumOf { it.height }

        val combinedBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)
        canvas.drawColor(backgroundColor)

        var top = 0
        for (bitmap in bitmaps) {
            canvas.drawBitmap(bitmap, 0f, top.toFloat(), null)
            top += bitmap.height
        }
        return combinedBitmap
    }

    private fun convertBitmapToPdf(bitmap: Bitmap): File? {
        return try {
            val fileName = "Transaction_Report_${System.currentTimeMillis()}.pdf"
            val file = File(requireContext().cacheDir, fileName)
            
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
            
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            
            file
        } catch (e: Exception) {
            Log.e("TransactionReportsFragment", "Error converting to PDF: ${e.message}", e)
            null
        }
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "report_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let {
                requireContext().contentResolver.openOutputStream(it).use { outputStream ->
                    outputStream?.let { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    requireContext().contentResolver.update(it, contentValues, null, null)
                }
            }
            uri
        } catch (e: Exception) {
            Log.e("TransactionReportsFragment", "Error saving bitmap: ${e.message}", e)
            null
        }
    }
}

