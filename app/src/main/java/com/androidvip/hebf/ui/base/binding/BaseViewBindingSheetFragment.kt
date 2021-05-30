package com.androidvip.hebf.ui.base.binding

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.Prefs
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.UserPrefs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

abstract class BaseViewBindingSheetFragment<T : ViewBinding>(
    private val inflate: FragmentInflater<T>,
) : BottomSheetDialogFragment() {
    protected val workerContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    protected val prefs: Prefs by inject()
    protected val userPrefs: UserPrefs by inject()

    private var _binding: T? = null
    val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val behavior = (dialog as BottomSheetDialog).behavior

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    changeStatusBarColor(R.color.colorSurfaceAlt)
                } else {
                    changeStatusBarColor(R.color.statusBarColor)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.background = createBackgroundShape()
    }

    override fun onDestroy() {
        changeStatusBarColor(R.color.statusBarColor)
        super.onDestroy()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    open fun createBackgroundShape(): MaterialShapeDrawable {
        val model = ShapeAppearanceModel.builder(
            requireContext(), 0, R.style.BottomSheetShapeAppearanceOverlay
        ).build()

        return MaterialShapeDrawable(model).apply {
            fillColor = ContextCompat.getColorStateList(requireContext(), R.color.colorSurface)
            tintList = ContextCompat.getColorStateList(requireContext(), R.color.colorSurface)
            elevation = 8F
        }
    }

    open fun createExpandedShape(): MaterialShapeDrawable {
        val model = ShapeAppearanceModel.builder(
            requireContext(), 0, R.style.ShapeAppearance_AppTheme_Flat
        ).build()

        return MaterialShapeDrawable(model).apply {
            fillColor = ContextCompat.getColorStateList(requireContext(), R.color.colorSurface)
            tintList = ContextCompat.getColorStateList(requireContext(), R.color.colorSurface)
            elevation = 8F
        }
    }

    protected suspend fun isRooted() = withContext(workerContext) {
        return@withContext Shell.rootAccess()
    }

    fun runCommand(command: String) {
        lifecycleScope.launch(workerContext) {
            RootUtils.execute(command)

        }
    }

    fun runCommand(array: Array<String>) {
        lifecycleScope.launch(workerContext) {
            RootUtils.execute(array)
        }
    }

    private fun changeStatusBarColor(@ColorRes colorRes: Int) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                requireActivity().window.statusBarColor = ContextCompat.getColor(
                    requireContext(), colorRes
                )
            }
        }
    }

    val isActivityAlive: Boolean
        get() = activity != null && !requireActivity().isFinishing && isAdded

    val applicationContext: Context
        get() = requireContext().applicationContext

    val isResumedState: Boolean
        get() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
}