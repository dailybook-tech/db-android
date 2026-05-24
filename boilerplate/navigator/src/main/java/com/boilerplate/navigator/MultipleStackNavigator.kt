package com.boilerplate.navigator

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.boilerplate.navigator.controller.FragmentManagerController
import com.boilerplate.navigator.data.FragmentData
import com.boilerplate.navigator.data.StackItem
import com.boilerplate.navigator.tag.TagCreator
import com.boilerplate.navigator.tag.UniqueTagCreator
import com.boilerplate.navigator.transitionanimation.TransitionAnimationType

open class MultipleStackNavigator(
    val fragmentManager: FragmentManager,
    containerId: Int,
    var rootFragmentProvider: ArrayList<() -> Fragment>,
    private var navigatorListener: Navigator.NavigatorListener? = null,
    private val navigatorConfiguration: NavigatorConfiguration = NavigatorConfiguration(),
    private val transitionAnimationType: TransitionAnimationType? = null,
    val context: Context
) : Navigator {

    private var destinationChangeLiveData = MutableLiveData<Fragment?>()
    private val tagCreator: TagCreator = UniqueTagCreator()

    private val fragmentManagerController = FragmentManagerController(
        fragmentManager,
        containerId,
        navigatorConfiguration.defaultNavigatorTransaction
    )

    private val fragmentStackStateMapper = FragmentStackStateMapper()
    var fragmentStackState = FragmentStackState()

    private var isFromSwitchTab = false
    private var isFromPopBack = false

    override fun start(fragment: Fragment) {
        start(fragment, DEFAULT_GROUP_NAME, TransitionAnimationType.RIGHT_TO_LEFT)
    }

    override fun start(fragment: Fragment, tabIndex: Int) {
        start(fragment, tabIndex, DEFAULT_GROUP_NAME)
    }

    override fun start(fragment: Fragment, tabIndex: Int, fragmentGroupName: String) {
        switchTab(tabIndex)
        start(fragment, fragmentGroupName)
        navigatorListener?.onTabChanged(tabIndex)
    }

    override fun start(fragment: Fragment, fragmentGroupName: String) {
        start(fragment, fragmentGroupName, transitionAnimationType)
    }

    override fun start(fragment: Fragment, transitionAnimation: TransitionAnimationType) {
        start(fragment, DEFAULT_GROUP_NAME, transitionAnimation)
    }

    override fun start(fragment: Fragment, fragmentGroupName: String, transitionAnimation: TransitionAnimationType?) {
        runCatching {
            val createdTag = tagCreator.create(fragment)
            val currentTabIndex = fragmentStackState.getSelectedTabIndex()
            val fragmentData = FragmentData(fragment, createdTag, transitionAnimation)

            if (fragmentStackState.isSelectedTabEmpty()) {
                val rootFragment = getRootFragment(currentTabIndex)
                val rootFragmentTag = tagCreator.create(rootFragment)
                val rootFragmentData =
                    FragmentData(rootFragment, rootFragmentTag, transitionAnimation)
                fragmentManagerController.disableAndStartFragment(
                    getCurrentFragmentTag(),
                    rootFragmentData,
                    fragmentData
                )
            } else {
                fragmentManagerController.disableAndStartFragment(
                    getCurrentFragmentTag(),
                    fragmentData
                )
            }
            fragmentStackState.notifyStackItemAddToCurrentTab(
                StackItem(
                    fragmentTag = createdTag,
                    groupName = fragmentGroupName,
                    tabGroup = fragmentStackState.getSelectedTabIndex()
                )
            )
            notifyFragmentDestinationChange(fragment)
        }
    }

    override fun goBack() {
        runCatching {
            if (canGoBack().not()) {
//            throw IllegalStateException("Can not call goBack() method because stack is empty.")
            }

            if (canFragmentGoBack().not()) return

            if (shouldExit() && shouldGoBackToInitialIndex()) {
                fragmentStackState.insertTabToBottom(navigatorConfiguration.initialTabIndex)
            }


            if (fragmentStackState.hasSelectedTabOnlyRoot()) {
                fragmentManagerController.disableFragment(getCurrentFragmentTag())
                fragmentStackState.popSelectedTab()
                navigatorListener?.onTabChanged(fragmentStackState.getSelectedTabIndex())
            } else {
                val currentFragmentTag = fragmentStackState.popItemFromSelectedTab().fragmentTag
                fragmentManagerController.removeFragment(currentFragmentTag)
            }

            showUpperFragment(isFromPopBack = true)
        }
    }

    override fun canGoBack(): Boolean {
        if (shouldExit() && shouldGoBackToInitialIndex().not()) {
            return false
        }
        return true
    }

    override fun switchTab(tabIndex: Int) {
        runCatching {
            if (fragmentStackState.isSelectedTab(tabIndex)) return

            fragmentManagerController.disableFragment(getCurrentFragmentTag())

            fragmentStackState.switchTab(tabIndex)

            showUpperFragment(true)

            navigatorListener?.onTabChanged(tabIndex)
        }
    }

    override fun reset(tabIndex: Int, resetRootFragment: Boolean) {
        if (fragmentStackState.isSelectedTab(tabIndex)) {
            resetCurrentTab(resetRootFragment)
            return
        }

        clearAllFragments(tabIndex, resetRootFragment)
    }

    override fun resetCurrentTab(resetRootFragment: Boolean) {
        val currentTabIndex = fragmentStackState.getSelectedTabIndex()
        clearAllFragments(currentTabIndex, resetRootFragment)

        if (resetRootFragment) {
            val rootFragment = getRootFragment(currentTabIndex)
            val createdTag = tagCreator.create(rootFragment)
            val rootFragmentData = FragmentData(rootFragment, createdTag)
            fragmentStackState.switchTab(currentTabIndex)
            fragmentStackState.notifyStackItemAdd(currentTabIndex, StackItem(fragmentTag = createdTag))
            fragmentManagerController.addFragment(rootFragmentData)
            notifyFragmentDestinationChange(rootFragment)
        } else {
            val upperFragmentTag: String = getCurrentFragmentTag()
            val upperFragment: Fragment? = fragmentManagerController.getFragment(upperFragmentTag)

            val newDestination: Fragment = upperFragment ?: getRootFragment(currentTabIndex)
            val newDestinationTag: String = tagCreator.create(newDestination)

            notifyFragmentDestinationChange(newDestination)
            fragmentManagerController.enableFragment(newDestinationTag)
        }
    }

    override fun reset() {
        clearAllFragments()
        fragmentStackState.clear()
        initializeStackState()
    }

    override fun resetWithFragmentProvider(rootFragmentProvider: ArrayList<() -> Fragment>) {
        this.rootFragmentProvider = rootFragmentProvider
        reset()
    }

    override fun clearGroup(fragmentGroupName: String) {
        if (fragmentGroupName == DEFAULT_GROUP_NAME) {
            throw IllegalArgumentException("Fragment group name can not be empty.")
        }
        val upperFragmentTag = fragmentStackState.peekItemFromSelectedTab()?.fragmentTag

        val poppedFragmentTags = fragmentStackState
            .popItems(fragmentGroupName)
            .map { it.fragmentTag }

        if (poppedFragmentTags.isNotEmpty()) {
            fragmentManagerController.removeFragments(poppedFragmentTags)
            val poppedUpperFragment = poppedFragmentTags.contains(upperFragmentTag)
            if (poppedUpperFragment) {
                showUpperFragment()
            }
        }
    }

    fun clearGroup(tabIndex : Int){
        val upperFragmentTag = fragmentStackState.peekItemFromSelectedTab()?.fragmentTag

        val poppedFragmentTags = fragmentStackState
            .popItemsFromTabIndex(tabIndex)
            .map { it.fragmentTag }

        if (poppedFragmentTags.isNotEmpty()) {
            fragmentManagerController.removeFragments(poppedFragmentTags)
            val poppedUpperFragment = poppedFragmentTags.contains(upperFragmentTag)
            if (poppedUpperFragment) {
                showUpperFragment()
            }
        }
    }

    override fun hasOnlyRoot(tabIndex: Int): Boolean {
        return fragmentStackState.hasOnlyRoot(tabIndex)
    }

    override fun getCurrentFragment(): Fragment? {
        return runCatching {
            val visibleFragmentTag = getCurrentFragmentTag()
            fragmentManagerController.getFragment(visibleFragmentTag)
        }.getOrNull()
    }

    override fun initialize(savedState: Bundle?) {
        if (savedState == null) {
            initializeStackState()
        }
        else {
            loadStackStateFromSavedState(savedState)
        }
    }

    override fun observeDestinationChanges(
        lifecycleOwner: LifecycleOwner,
        destinationChangedListener: (Fragment) -> Unit
    ) {
        destinationChangeLiveData.observe(lifecycleOwner) { fragment ->
            if (fragment != null) {
                destinationChangedListener(fragment)
            }
        }
    }

    override fun observeDestinationChangesWithTabChange(
        lifecycleOwner: LifecycleOwner,
        destinationChangedListener: (Fragment, Boolean) -> Unit
    ) {
        destinationChangeLiveData.observe(lifecycleOwner) { fragment ->
            if (fragment != null) {
                destinationChangedListener(fragment, isFromSwitchTab)
            }
        }
    }

    override fun observeDestinationChangesWithPopBack(
        lifecycleOwner: LifecycleOwner,
        destinationChangedListener: (Fragment, Boolean) -> Unit
    ) {
        destinationChangeLiveData.observe(lifecycleOwner) { fragment ->
            if (fragment != null) {
                destinationChangedListener(fragment, isFromPopBack)
            }
        }
    }

    override fun getFragmentIndexInStackBySameType(tag: String?): Int {
        if (tag.isNullOrEmpty()) return -1
        fragmentStackState.fragmentTagStack.forEach { stack ->
            stack.forEachIndexed { index, stackItem ->
                if (stackItem.fragmentTag == tag) {
                    return stack.size - index - 1
                }
            }
        }
        return -1
    }

    private fun initializeStackState() {
        val initialTabIndex = navigatorConfiguration.initialTabIndex
        val rootFragment = rootFragmentProvider.get(initialTabIndex).invoke()
        val createdTag = tagCreator.create(rootFragment)
        val stackItem = StackItem(fragmentTag = createdTag)

        fragmentStackState.setStackCount(rootFragmentProvider.size)
        fragmentStackState.notifyStackItemAdd(tabIndex = initialTabIndex, stackItem = stackItem)
        fragmentStackState.switchTab(initialTabIndex)

        val rootFragmentTag = fragmentStackState.peekItem(initialTabIndex).fragmentTag
        val rootFragmentData = FragmentData(rootFragment, rootFragmentTag)
        fragmentManagerController.addFragment(rootFragmentData)
        navigatorListener?.onTabChanged(navigatorConfiguration.initialTabIndex)
        notifyFragmentDestinationChange(rootFragment)
    }

    private fun loadStackStateFromSavedState(savedState: Bundle) {
        val stackState = fragmentStackStateMapper.fromBundle(savedState.getBundle(
            MEDUSA_STACK_STATE_KEY
        ))
        fragmentStackState.setStackState(stackState)
        if (stackState.tabIndexStack.isNotEmpty()) {
            navigatorListener?.onTabChanged(fragmentStackState.getSelectedTabIndex())
        }
    }

    private fun getRootFragment(tabIndex: Int): Fragment {
        return tabIndex
            .takeUnless { fragmentStackState.isTabEmpty(it) }
            ?.let { fragmentManagerController.getFragment(fragmentStackState.peekItem(it).fragmentTag) }
            ?: rootFragmentProvider[tabIndex].invoke()
    }

    private fun showUpperFragment(isFromSwitchTab : Boolean = false, isFromPopBack : Boolean = false) {
        val upperFragmentTag = fragmentStackState.peekItemFromSelectedTab()?.fragmentTag
        val upperFragment = upperFragmentTag?.let { tag -> fragmentManagerController.getFragment(tag) }
        if (upperFragmentTag == null || upperFragment == null) {
            val rootFragment = getRootFragment(fragmentStackState.getSelectedTabIndex())
            val createdTag = tagCreator.create(rootFragment)
            val rootFragmentData = FragmentData(rootFragment, createdTag)
            fragmentStackState.notifyStackItemAdd(
                fragmentStackState.getSelectedTabIndex(),
                StackItem(createdTag)
            )
            fragmentManagerController.addFragment(rootFragmentData)
            notifyFragmentDestinationChange(rootFragment,isFromSwitchTab, isFromPopBack)
        } else {
            fragmentManagerController.enableFragment(upperFragmentTag)
            notifyFragmentDestinationChange(upperFragment, isFromSwitchTab, isFromPopBack)
        }
    }

    private fun getCurrentFragmentTag() = requireNotNull(fragmentStackState.peekItemFromSelectedTab()).fragmentTag

    private fun shouldExit(): Boolean {
        return fragmentStackState.hasTabStack() && fragmentStackState.hasSelectedTabOnlyRoot()
    }

    private fun shouldGoBackToInitialIndex(): Boolean {
        return fragmentStackState.getSelectedTabIndex() != navigatorConfiguration.initialTabIndex && navigatorConfiguration.alwaysExitFromInitial
    }

    private fun clearAllFragments() {
        fragmentStackState.popItemsFromNonEmptyTabs().forEach {
            fragmentManagerController.findFragmentByTagAndRemove(it.fragmentTag)
        }
        fragmentManagerController.commitAllowingStateLoss()
    }

    private fun clearAllFragments(tabIndex: Int, resetRootFragment: Boolean) {
        if (fragmentStackState.isTabEmpty(tabIndex)) {
            return
        }

        while (fragmentStackState.isTabEmpty(tabIndex).not()) {
            if (fragmentStackState.hasOnlyRoot(tabIndex) && resetRootFragment.not()) {
                break
            }
            val fragmentTagToBeRemoved = fragmentStackState.popItem(tabIndex).fragmentTag
            fragmentManagerController.findFragmentByTagAndRemove(fragmentTagToBeRemoved)
        }

        fragmentManagerController.commitAllowingStateLoss()
    }

    private fun canFragmentGoBack(): Boolean {
        if (getCurrentFragment() is Navigator.OnGoBackListener) {
            return (getCurrentFragment() as Navigator.OnGoBackListener).onGoBack()
        }
        return true
    }

    private fun notifyFragmentDestinationChange(fragment: Fragment,isFromSwitchTab : Boolean = false,isFromPopBack : Boolean = false) {
        fragment.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                fragment.viewLifecycleOwner.lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onCreate(owner: LifecycleOwner) {
                            this@MultipleStackNavigator.isFromPopBack = isFromPopBack
                            destinationChangeLiveData.value = fragment
                            this@MultipleStackNavigator.isFromSwitchTab = isFromSwitchTab

                        }

                        override fun onDestroy(owner: LifecycleOwner) {
                            if (destinationChangeLiveData.value == fragment) {
                                destinationChangeLiveData.value = null
                            }
                            owner.lifecycle.removeObserver(this)
                        }
                    }
                )
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle(MEDUSA_STACK_STATE_KEY, fragmentStackStateMapper.toBundle(fragmentStackState))
    }

    override fun start(bottomSheetFragment: BottomSheetDialogFragment) {
        bottomSheetFragment.show(fragmentManager, tagCreator.create(bottomSheetFragment))
    }

    companion object {
        const val DEFAULT_GROUP_NAME = ""

        internal const val MEDUSA_STACK_STATE_KEY = "MEDUSA_STACK_STATE_KEY"
    }
}
