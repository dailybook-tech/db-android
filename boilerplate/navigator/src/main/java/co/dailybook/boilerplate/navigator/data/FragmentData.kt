package co.dailybook.boilerplate.navigator.data

import androidx.fragment.app.Fragment
import co.dailybook.boilerplate.navigator.transitionanimation.TransitionAnimationType

data class FragmentData(val fragment: Fragment, val fragmentTag: String, val transitionAnimation: TransitionAnimationType? = null)