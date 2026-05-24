package com.boilerplate.navigator.data

import androidx.fragment.app.Fragment
import com.boilerplate.navigator.transitionanimation.TransitionAnimationType

data class FragmentData(val fragment: Fragment, val fragmentTag: String, val transitionAnimation: TransitionAnimationType? = null)