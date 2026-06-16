package com.dailybook.auth.screen.login.view

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.View
import androidx.fragment.app.Fragment
import com.boilerplate.navigator.MultipleStackNavigator
import com.boilerplate.navigator.Navigator
import com.boilerplate.navigator.NavigatorConfiguration
import com.boilerplate.navigator.transaction.NavigatorTransaction
import com.boilerplate.navigator.transitionanimation.TransitionAnimationType
import com.dailybook.auth.R
import com.dailybook.auth.databinding.ActivityLoginBinding
import com.dailybook.base.BaseActivity
import com.dailybook.base.Logger
import com.dailybook.base.navigator.FragmentNavigator
import com.truecaller.android.sdk.oAuth.TcSdk
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import org.koin.android.ext.android.inject

class LoginActivity : BaseActivity(){

    private lateinit var binding: ActivityLoginBinding
    private val fragmentNavigator: FragmentNavigator by inject()

    val loginFragment = LoginFragment.newInstance()
    private val rootFragmentProvider: ArrayList<() -> Fragment> = arrayListOf(
        { loginFragment }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupNavigator(savedInstanceState)
    }

    private fun enableEdgeToEdge() {
        val root: View = binding.root
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = sysBars.left,
                top = sysBars.top,
                right = sysBars.right,
                bottom = sysBars.bottom
            )
            insets
        }
        WindowInsetsControllerCompat(window, root).isAppearanceLightStatusBars = true
    }

    private fun setupNavigator(savedInstanceState: Bundle?) {
        fragmentNavigator.initialize(
            MultipleStackNavigator(
                supportFragmentManager,
                R.id.container,
                rootFragmentProvider,
                navigatorConfiguration = NavigatorConfiguration(
                    0,
                    true,
                    NavigatorTransaction.SHOW_HIDE
                ),
                context = this,
                transitionAnimationType = TransitionAnimationType.RIGHT_TO_LEFT
            ), savedInstanceState
        )
    }

    override fun onBackPressed() {
        if (fragmentNavigator.canGoBack() == true) {
            fragmentNavigator.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12 || requestCode == TcSdk.SHARE_PROFILE_REQUEST_CODE) {
            loginFragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fragmentNavigator?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }
}