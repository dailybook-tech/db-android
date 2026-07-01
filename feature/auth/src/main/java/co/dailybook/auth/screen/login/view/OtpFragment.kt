package co.dailybook.auth.screen.login.view

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import co.dailybook.boilerplate.network.NetworkHandler
import co.dailybook.boilerplate.uikit.views.hide
import co.dailybook.boilerplate.uikit.views.show
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import co.dailybook.auth.R
import co.dailybook.auth.common.sms.AuthOTPBroadcastReceiver
import co.dailybook.auth.common.sms.SMSListener
import co.dailybook.auth.databinding.FragmentOtpBinding
import co.dailybook.auth.model.request.AuthRequestBody
import co.dailybook.auth.model.request.AuthResponse
import co.dailybook.auth.model.request.User
import co.dailybook.auth.screen.login.uistate.UiState
import co.dailybook.auth.screen.login.viewmodel.AuthViewModel
import co.dailybook.base.BaseConstants
import co.dailybook.base.BaseFragment
import co.dailybook.base.Headers
import co.dailybook.base.analytics.ConstantEventAttributes
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.navigator.ActivitiesNameEnum.BookKeepActivityEnum
import co.dailybook.base.navigator.FragmentNavigator
import co.dailybook.base.navigator.ModuleNavigator
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.concurrent.TimeUnit

private const val MOBILE_NUMBER = "MOBILE_NUMBER"

class OtpFragment : BaseFragment<FragmentOtpBinding>() {

    private val viewModel: AuthViewModel by sharedViewModel()
    private var mobileNumber: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mobileNumber = it.getString(MOBILE_NUMBER)
        }
    }

    override val screenName: String
        get() = ConstantEventNames.OTP

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentOtpBinding? {
        return FragmentOtpBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModelObserver()
        setupViews()
        registerOnClickListeners()
        if (!viewModel.usesFirebaseOtp()) {
            initSMSRetriever()
        }
    }

    private fun setupViews() {

        val otpSentTo = getString(R.string.otp_has_been_sent_to)
        val fullText = "$otpSentTo +91 $mobileNumber"
        val spannable = SpannableString(fullText)

        val phoneNumberColor = getColor(requireContext(), R.color.otp_phone_number)
        spannable.setSpan(ForegroundColorSpan(phoneNumberColor), otpSentTo.length, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding?.tvOtpSentToNumber?.text = spannable

        binding?.btnVerifyOtp?.apply {
            isEnabled = false
        }

        binding?.otpView?.let {
            viewModel.attachOtpTextWatcher(it)
            viewModel.consumePrefilledOtpCode()?.let { otpCode ->
                it.setText(otpCode)
            }
        }
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            btnVerifyOtp.setOnClickListener {
                lifecycleScope.launch {
                    val installSource = dataStoreManager.read(DataStoreManager.INSTALL_SOURCE, "").first()
                    val installReferrerPayload = dataStoreManager.read(DataStoreManager.INSTALL_REFERRER_RAW, "").first()
                    if (viewModel.usesFirebaseOtp()) {
                        verifyFirebaseOtp(
                            otp = otpView.text.toString(),
                            installSource = installSource.ifBlank { null },
                            installReferrerPayload = installReferrerPayload.ifBlank { null }
                        )
                    } else {
                        viewModel.verifyOtp(
                            AuthRequestBody(
                                countryCode = BaseConstants.COUNTRY_CODE,
                                mobileNumber = mobileNumber,
                                otp = otpView.text.toString(),
                                installSource = installSource.ifBlank { null },
                                installReferrerPayload = installReferrerPayload.ifBlank { null }
                            )
                        )
                    }
                    recordClickEvent(ConstantEventNames.VERIFY_OTP, hashMapOf(Pair("mobile_number", mobileNumber?:"")))
                }
            }

            toolbar.ivBack.setOnClickListener {
                fragmentNavigator.goBack()
            }

            ivResendOtp.setOnClickListener {
                if (viewModel.usesFirebaseOtp()) {
                    resendFirebaseOtp()
                } else {
                    viewModel.resendOtp(
                        AuthRequestBody(
                            countryCode = BaseConstants.COUNTRY_CODE,
                            mobileNumber = mobileNumber
                        )
                    )
                }
                recordClickEvent(ConstantEventNames.RESEND_OTP, hashMapOf(Pair("mobile_number", mobileNumber?:"")))
            }
        }
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner) { it ->
            when (it) {
                is UiState.Loading -> {
                    binding?.pb?.show()
                }

                is UiState.OtpVerified -> {
                    binding?.pb?.hide()
                    lifecycleScope.launch {
                        triggerSystemEvent(ConstantEventNames.LOGIN_SUCCESS)
                        triggerSystemEvent(ConstantEventNames.MOBILE_OTP_TRUECALLER)
                        storeUserDetails(it.authResponse)
                        moduleNavigator.startActivity(requireContext(), BookKeepActivityEnum)
                        requireActivity().finish()
                    }
                }

                is UiState.Error -> {
                    binding?.pb?.hide()
                    binding?.tvOtpError?.text = it.message
                    binding?.tvOtpError?.show()
                }

                is UiState.OtpEntered -> {
                    changeButtonState(it.isValidOtp)
                }

                is UiState.OtpSent -> {
                    binding?.pb?.hide()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }

    private suspend fun storeUserDetails(authResponse: AuthResponse?) {
        authResponse?.let {
            it.user?.let { user: User ->
                dataStoreManager.write(DataStoreManager.ACCESS_TOKEN, it.authToken ?: "")
                dataStoreManager.write(DataStoreManager.USER_ID, user.userId ?: "")
                dataStoreManager.write(DataStoreManager.USER_NAME, user.userName ?: "")
                dataStoreManager.write(DataStoreManager.USER_TYPE, user.userType ?: "")
                dataStoreManager.write(DataStoreManager.MOBILE_NUMBER, user.mobileNumber ?: "")
                dataStoreManager.write(DataStoreManager.COMPANY_ID, user.companyId ?: "")
                dataStoreManager.write(DataStoreManager.IS_LOGGED_IN, true)
                NetworkHandler.getInstance().setAdditionalHeaders(
                    hashMapOf(
                        Pair(
                            Headers.COMPANY_ID,
                            dataStoreManager.read(DataStoreManager.COMPANY_ID, "").first()
                        ),
                        Pair(
                            Headers.AUTHORIZATION,
                            Headers.BEARER.plus(" ")
                                .plus(
                                    dataStoreManager.read(DataStoreManager.ACCESS_TOKEN, "").first()
                                )
                        ),
                        Pair(
                            Headers.USER_ID,
                            dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                        ),
                        Pair(
                            Headers.GENERIC_USER_ID,
                            dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                        )
                    )
                )
                val mixpanel = MixpanelAPI.getInstance(requireContext(), BaseConstants.MIXPANEL_TOKEN, true)
                mixpanel.identify(dataStoreManager.read(DataStoreManager.USER_ID, "").first())
                mixpanel?.people?.set(
                    ConstantEventAttributes.USER_ID, dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                )
                mixpanel?.people?.set(
                    ConstantEventAttributes.USER_MOBILE_NUMBER, dataStoreManager.read(DataStoreManager.MOBILE_NUMBER, "").first()
                )
                mixpanel?.people?.set(
                    ConstantEventAttributes.USER_NAME, dataStoreManager.read(DataStoreManager.USER_NAME, "").first()
                )
                mixpanel?.people?.set(
                    ConstantEventAttributes.USER_TYPE, dataStoreManager.read(DataStoreManager.USER_TYPE, "").first()
                )
            }
        }
    }

    private fun changeButtonState(enable: Boolean) {
        binding?.apply {
            if (tvOtpError.isVisible) {
                tvOtpError.hide()
            }
            btnVerifyOtp.text = getString(R.string.verify_otp)
            if (enable) {
                btnVerifyOtp.isEnabled = true
                btnVerifyOtp.performClick()
            } else {
                btnVerifyOtp.isEnabled = false
            }
        }
    }

    private fun initSMSRetriever() {
        // Start the SMS retriever
        val smsRetrieverClient = SmsRetriever.getClient(requireContext())
        val task = smsRetrieverClient.startSmsRetriever()

        task.addOnSuccessListener {
            // SMS retrieval has started successfully
            AuthOTPBroadcastReceiver.initSMSListener(object : SMSListener {
                override fun onSuccess(message: String?) {
                    try {
                        val otp = message?.take(4)
                        binding?.otpView?.setText("")
                        binding?.otpView?.setText(otp)
                    } catch (e: Exception) {
                    }
                }

                override fun onError(message: String?) {

                }
            })
        }

        task.addOnFailureListener { e -> }
    }

    private fun resendFirebaseOtp() {
        val resendToken = viewModel.getFirebaseResendToken() ?: run {
            viewModel.showError("Unable to resend OTP right now")
            return
        }

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+${BaseConstants.COUNTRY_CODE}${mobileNumber.orEmpty()}")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setForceResendingToken(resendToken)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    credential.smsCode?.let { binding?.otpView?.setText(it) }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    viewModel.showError(e.localizedMessage ?: "Failed to resend OTP")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    viewModel.setFirebaseOtpSession(verificationId, token)
                    viewModel.showOtpSent("Otp Re-Sent")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyFirebaseOtp(
        otp: String,
        installSource: String?,
        installReferrerPayload: String?,
    ) {
        val verificationId = viewModel.getFirebaseVerificationId()
        if (verificationId.isNullOrBlank()) {
            viewModel.showError("OTP session expired. Please try again.")
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                authResult.user?.getIdToken(true)
                    ?.addOnSuccessListener { tokenResult ->
                        viewModel.verifyFirebaseOtp(
                            AuthRequestBody(
                                countryCode = BaseConstants.COUNTRY_CODE,
                                mobileNumber = mobileNumber,
                                firebaseIdToken = tokenResult.token,
                                installSource = installSource,
                                installReferrerPayload = installReferrerPayload
                            )
                        )
                    }
                    ?.addOnFailureListener { exception ->
                        viewModel.showError(exception.localizedMessage ?: "Failed to verify Firebase OTP")
                    }
            }
            .addOnFailureListener { exception ->
                viewModel.showError(exception.localizedMessage ?: "Invalid OTP")
            }
    }

    companion object {
        @JvmStatic
        fun newInstance(mobileNumber: String) = OtpFragment().apply {
            arguments = Bundle().apply {
                putString(MOBILE_NUMBER, mobileNumber)
            }
        }
    }
}
