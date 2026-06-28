package co.dailybook.auth.common.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status


/**
 * BroadcastReceiver to wait for SMS messages. This can be registered either
 * in the AndroidManifest or at runtime.  Should filter Intents on
 * SmsRetriever.SMS_RETRIEVED_ACTION.
 */
class AuthOTPBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras
            var status: Status? = null
            if (extras != null) {
                status = extras[SmsRetriever.EXTRA_STATUS] as Status?
            }
            if (status != null) {
                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        // Get SMS message contents
                        val message = extras!![SmsRetriever.EXTRA_SMS_MESSAGE] as String?
                        // Extract one-time code from the message and complete verification
                        // by sending the code back to your server.
                        smsListener?.onSuccess(message)
                    }

                    CommonStatusCodes.TIMEOUT ->                         // Waiting for SMS timed out (5 minutes)
                        // Handle the error ...
                        smsListener?.onError("Failed to extract from Broadcast Receiver")
                }
            }
        }
    }

    companion object {
        private var smsListener: SMSListener? = null
        fun initSMSListener(listener: SMSListener?) {
            smsListener = listener
        }
    }
}