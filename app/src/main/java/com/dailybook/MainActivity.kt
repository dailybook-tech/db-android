package com.dailybook

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.boilerplate.navigator.MultipleStackNavigator
import com.boilerplate.navigator.Navigator
import com.boilerplate.navigator.NavigatorConfiguration
import com.boilerplate.navigator.transaction.NavigatorTransaction
import com.boilerplate.navigator.transitionanimation.TransitionAnimationType
import com.dailybook.auth.screen.login.view.LoginFragment

class MainActivity : AppCompatActivity(), Navigator.NavigatorListener {
    private lateinit var multipleStackNavigator: MultipleStackNavigator
    private val rootFragmentProvider: ArrayList<() -> Fragment> = arrayListOf(
        { LoginFragment.newInstance() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()
        setupNavigator(savedInstanceState)
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root: View = findViewById(R.id.main)
        val bottomNav: View? = findViewById(R.id.bottom_nav)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = sysBars.left,
                top = sysBars.top,
                right = sysBars.right,
                bottom = sysBars.bottom
            )
            bottomNav?.updatePadding(bottom = sysBars.bottom)
            insets
        }
    }

    private fun setupNavigator(savedInstanceState: Bundle?) {
        multipleStackNavigator = MultipleStackNavigator(
            supportFragmentManager,
            R.id.container,
            rootFragmentProvider,
            navigatorListener = this,
            navigatorConfiguration = NavigatorConfiguration(
                0,
                true,
                NavigatorTransaction.SHOW_HIDE
            ),
            context = this,
            transitionAnimationType = TransitionAnimationType.RIGHT_TO_LEFT
        )
        multipleStackNavigator?.initialize(savedInstanceState)
    }

    override fun onTabChanged(tabIndex: Int) {

    }
}