package com.laborbook.auth.screen.login.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.boilerplate.network.NetworkHandler
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.laborbook.auth.databinding.FragmentLoginBinding
import com.laborbook.auth.model.request.AuthRequestBody
import com.laborbook.auth.model.request.AuthResponse
import com.laborbook.auth.model.request.TruecallerRequestBody
import com.laborbook.auth.model.request.User
import com.laborbook.auth.screen.login.uistate.UiState
import com.laborbook.auth.screen.login.viewmodel.AuthViewModel
import com.laborbook.base.BaseConstants
import com.laborbook.base.BaseFragment
import com.laborbook.base.Headers
import com.laborbook.base.Logger
import com.laborbook.base.analytics.ConstantEventAttributes
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.navigator.ActivitiesNameEnum.BookKeepActivityEnum
import com.laborbook.base.toggleKeyboard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.truecaller.android.sdk.oAuth.CodeVerifierUtil
import com.truecaller.android.sdk.oAuth.TcOAuthCallback
import com.truecaller.android.sdk.oAuth.TcOAuthData
import com.truecaller.android.sdk.oAuth.TcOAuthError
import com.truecaller.android.sdk.oAuth.TcSdk
import com.truecaller.android.sdk.oAuth.TcSdkOptions
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.math.BigInteger
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

private const val TEST_MOBILE_NUMBER = "9090909090"

class LoginFragment : BaseFragment<FragmentLoginBinding>(), TcOAuthCallback {

    private var codeVerifier: String? = null
    private var mobileNumber: String = ""
    private val viewModel : AuthViewModel by sharedViewModel()

    override val screenName: String
        get() = ConstantEventNames.LOGIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentLoginBinding? {
        return FragmentLoginBinding.inflate(inflater,container,false)
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
        setupTruecaller()
        try {
            if (isTruecallerInstalled()) {
                triggerTrueCallerLogin()
            } else if (isGooglePlayServicesAvailable(requireContext())) {
                showPhoneNumberHint()
            }
        }catch (e: Exception){}
    }

    private fun setupViews() {
        binding?.apply {
            if(isTruecallerInstalled()){
                tvOrWith.show()
                btnTcLogin.show()
                triggerImpressionEvent(ConstantEventNames.TRUECALLER_INSTALLED)
            } else {
                tvOrWith.hide()
                btnTcLogin.hide()
            }
            binding?.btnLogin?.apply {
                isEnabled = false
            }
            etNumber.let {
                viewModel.attachPhoneNumberTextWatcher(it)
            }

            val termsText = "By continuing you agree that you have read and accepted our Terms & Conditions and Privacy Policy."
            val spannableString = SpannableString(termsText)

            val termsStart = termsText.indexOf("Terms & Conditions")
            val termsEnd = termsStart + "Terms & Conditions".length
            val privacyStart = termsText.indexOf("Privacy Policy")
            val privacyEnd = privacyStart + "Privacy Policy".length

            // ClickableSpan for Terms & Conditions
            val termsClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val termsAndConditionsUrl = "https://laborbook.app/terms-of-service"
                    openUrlInCustomTab(requireContext(), termsAndConditionsUrl)
                }
            }

            // ClickableSpan for Privacy Policy
            val privacyClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val privacyPolicyUrl = "https://laborbook.app/privacy-policy"
                    openUrlInCustomTab(requireContext(), privacyPolicyUrl)
                }
            }

            // ColorSpan for custom color
            val color = Color.parseColor("#3270D2")

            spannableString.setSpan(termsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(ForegroundColorSpan(color), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(privacyClickableSpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(ForegroundColorSpan(color), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            tvTermsAndConditions.text = spannableString
            tvTermsAndConditions.movementMethod = LinkMovementMethod.getInstance()
            tvTermsAndConditions.highlightColor = Color.TRANSPARENT // Remove highlight color on click
        }
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            btnLogin.setOnClickListener {
                binding?.etNumber?.toggleKeyboard(requireActivity())
                mobileNumber = etNumber.text.toString()
                if (mobileNumber == TEST_MOBILE_NUMBER) {
                    viewModel.setOtpProvider(useFirebase = false)
                    viewModel.generateOtp(AuthRequestBody(countryCode = BaseConstants.COUNTRY_CODE, mobileNumber = mobileNumber))
                } else {
                    startFirebasePhoneVerification(mobileNumber)
                }
                recordClickEvent(ConstantEventNames.REQUEST_OTP, hashMapOf(Pair("mobile_number", mobileNumber)))
            }

            btnTcLogin.setOnClickListener {
                triggerTrueCallerLogin()
            }
        }
    }

    private fun triggerTrueCallerLogin() {
        try {
            TcSdk.getInstance().getAuthorizationCode(requireActivity())
            recordClickEvent(ConstantEventNames.TRUECALLER_LOGIN)
        } catch (e: Exception) {
        }
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner){
            if (!isVisible) {
                return@observe
            }
            when(it){
                is UiState.Loading -> {
                    binding?.pb?.show()
                    binding?.btnLogin?.isEnabled = false
                }
                is UiState.OtpSent -> {
                    binding?.pb?.hide()
                    // Re-enable the button when navigating to OTP screen
                    binding?.btnLogin?.isEnabled = true
                    fragmentNavigator.start(OtpFragment.newInstance(mobileNumber))
                }
                is UiState.Error -> {
                    binding?.pb?.hide()
                    binding?.btnLogin?.isEnabled = true
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                is UiState.TrueCallerLoginSuccess -> {
                    lifecycleScope.launch {
                        triggerSystemEvent(ConstantEventNames.TRUECALLER_LOGIN_SUCCESS)
                        triggerSystemEvent(ConstantEventNames.MOBILE_OTP_TRUECALLER)
                        storeUserDetails(it.authResponse)
                        moduleNavigator.startActivity(requireContext(), BookKeepActivityEnum)
                        requireActivity().finish()
                    }
                }
                is UiState.NumberEntered -> { 
                    // Only enable button if not currently loading
                    if (viewModel.uiState().value !is UiState.Loading) {
                        changeButtonState(it.isValidNumber)
                    }
                }
                else -> {}
            }
        }
    }

    private fun changeButtonState(enable : Boolean){
        binding?.btnLogin?.apply {
            isEnabled = enable
        }
    }

    private fun startFirebasePhoneVerification(phoneNumber: String) {
        viewModel.setOtpProvider(useFirebase = true)
        FirebaseAuth.getInstance().signOut()

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+${BaseConstants.COUNTRY_CODE}$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val smsCode = credential.smsCode
                    if (smsCode != null && !viewModel.getFirebaseVerificationId().isNullOrBlank()) {
                        viewModel.setFirebaseOtpSession(
                            verificationId = viewModel.getFirebaseVerificationId().orEmpty(),
                            resendToken = viewModel.getFirebaseResendToken(),
                            otpCode = smsCode
                        )
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    viewModel.showError(e.localizedMessage ?: "Failed to send OTP")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    viewModel.setFirebaseOtpSession(verificationId, token)
                    viewModel.showOtpSent("Otp Sent")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showPhoneNumberHint() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()

        val credentialsClient = requireActivity().let { Credentials.getClient(it) }
        val intent = credentialsClient.getHintPickerIntent(hintRequest)

        try {
            intent?.let {
                startIntentSenderForResult(it.intentSender, viewModel.PHONE_NUMBER_REQUEST, null, 0,
                    0, 0, Bundle())
            }
        } catch (e: IntentSender.SendIntentException) {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!isAdded || binding == null) return

        if (requestCode == viewModel.PHONE_NUMBER_REQUEST && resultCode == Activity.RESULT_OK) {
            val credential = data?.getParcelableExtra(Credential.EXTRA_KEY) as? Credential
            val phoneNumber = credential?.id?.substring(3)
            binding?.etNumber?.setText(phoneNumber)
            binding?.etNumber?.text?.length?.let { binding?.etNumber?.setSelection(it) }
        }

        if (requestCode == TcSdk.SHARE_PROFILE_REQUEST_CODE) {
            TcSdk.getInstance().onActivityResultObtained(requireActivity(), requestCode, resultCode, data)
        }
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        TcSdk.clear()
    }

    override fun onFailure(tcOAuthError: TcOAuthError) {
    }

    override fun onSuccess(tcOAuthData: TcOAuthData) {
        codeVerifier?.let { verifier ->
            lifecycleScope.launch {
                val installSource = dataStoreManager.read(DataStoreManager.INSTALL_SOURCE, "").first()
                val installReferrerPayload = dataStoreManager.read(DataStoreManager.INSTALL_REFERRER_RAW, "").first()
                viewModel.truecallerLogin(
                    TruecallerRequestBody(
                        authorizationCode = tcOAuthData.authorizationCode,
                        codeVerifier = verifier,
                        installSource = installSource.ifBlank { null },
                        installReferrerPayload = installReferrerPayload.ifBlank { null }
                    )
                )
            }
        }
    }

    override fun onVerificationRequired(tcOAuthError: TcOAuthError?) {
    }

    private fun setupTruecaller() {
        try {
            val tcSdkOptions = TcSdkOptions.Builder(requireContext(), this).build()
            TcSdk.init(tcSdkOptions)
            val isUsable = TcSdk.getInstance().isOAuthFlowUsable
            val stateRequested = BigInteger(130, SecureRandom()).toString(32)
            TcSdk.getInstance().setOAuthState(stateRequested)

            TcSdk.getInstance().setOAuthScopes(arrayOf("profile", "phone"))
            codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier()
            codeVerifier?.let {
                val codeChallenge = CodeVerifierUtil.getCodeChallenge(it)
                codeChallenge?.let {
                    TcSdk.getInstance().setCodeChallenge(it)
                } ?: Logger.e("Code challenge is Null. Can’t proceed further")
            }
        }catch (e: Exception){}
    }

    private fun isTruecallerInstalled(): Boolean {
        try {
            requireContext().packageManager.getPackageInfo("com.truecaller", PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
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

    companion object {
        @JvmStatic
        fun newInstance() = LoginFragment()
    }
}
