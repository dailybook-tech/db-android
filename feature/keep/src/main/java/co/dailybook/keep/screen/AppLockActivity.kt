package co.dailybook.keep.screen

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.keep.R
import co.dailybook.keep.databinding.ActivityAppLockBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.Executor

class AppLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppLockBinding
    private val dataStoreManager: DataStoreManager by inject()
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBiometricPrompt()
        checkBiometricAvailability()
    }

    private fun setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || 
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        // User canceled, keep showing lock screen
                        binding.tvLockMessage.text = getString(R.string.unlock_app_to_continue)
                    } else {
                        binding.tvLockMessage.text = errString.toString()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Authentication succeeded, mark as authenticated and finish
                    lifecycleScope.launch {
                        dataStoreManager.write(DataStoreManager.LAST_AUTH_TIME, System.currentTimeMillis().toString())
                        setResult(RESULT_OK)
                        finish()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    binding.tvLockMessage.text = getString(R.string.authentication_failed)
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_lock))
            .setSubtitle(getString(R.string.unlock_app_to_continue))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometric is available, show prompt
                binding.tvLockMessage.text = getString(R.string.touch_sensor_to_unlock)
                showBiometricPrompt()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                binding.tvLockMessage.text = getString(R.string.no_biometric_hardware)
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                binding.tvLockMessage.text = getString(R.string.biometric_unavailable)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.tvLockMessage.text = getString(R.string.no_biometric_enrolled)
            }
            else -> {
                binding.tvLockMessage.text = getString(R.string.biometric_unavailable)
            }
        }
    }

    private fun showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onResume() {
        super.onResume()
        // Show biometric prompt again when activity resumes
        if (::biometricPrompt.isInitialized) {
            checkBiometricAvailability()
        }
    }

    override fun onBackPressed() {
        // Prevent back button from closing the lock screen
        // User must authenticate to proceed
    }
}

