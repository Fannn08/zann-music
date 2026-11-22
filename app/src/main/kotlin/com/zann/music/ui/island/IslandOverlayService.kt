package com.zann.music.ui.island

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zann.music.data.island.IslandSettings
import com.zann.music.playback.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Menggambar Dynamic Island di atas aplikasi lain. Memakai layanan aksesibilitas
 * (TYPE_ACCESSIBILITY_OVERLAY) sehingga tidak perlu izin "tampil di atas aplikasi lain".
 * Layanan ini TIDAK membaca isi layar (canRetrieveWindowContent = false); event jendela
 * hanya dipakai untuk tahu apakah ZANN MUSIC sedang dibuka.
 */
class IslandOverlayService : AccessibilityService() {

    companion object {
        private val _appForeground = MutableStateFlow(true)
        /** true bila ZANN MUSIC sedang di layar -> island disembunyikan. */
        val appForeground: StateFlow<Boolean> = _appForeground.asStateFlow()

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        /** Apakah layanan ini sudah dinyalakan pengguna di Pengaturan > Aksesibilitas. */
        fun isEnabledInSystem(context: Context): Boolean {
            val expected = ComponentName(context, IslandOverlayService::class.java).flattenToString()
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return list.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        fun openAccessibilitySettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private var windowManager: WindowManager? = null
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var owner: OverlayOwner? = null
    private var scope: CoroutineScope? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        IslandSettings.init(applicationContext)
        PlayerManager.initialize(applicationContext)
        _connected.value = true
        addOverlay()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { s ->
            s.launch { IslandSettings.config.collect { applyPosition() } }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        when {
            pkg == packageName -> _appForeground.value = true
            // Bayangan notifikasi dan keyboard tidak dianggap berpindah aplikasi.
            pkg == "com.android.systemui" || pkg.contains("inputmethod", true) || pkg.contains("keyboard", true) -> Unit
            else -> _appForeground.value = false
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        removeOverlay()
        _connected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        removeOverlay()
        _connected.value = false
        super.onDestroy()
    }

    private fun addOverlay() {
        if (view != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val o = OverlayOwner().also { it.create() }
        owner = o
        val cv = ComposeView(this).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeViewModelStoreOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { IslandOverlay(onOpenApp = ::openApp) }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
        params = lp
        view = cv
        applyPosition()
        runCatching { wm.addView(cv, lp) }
        o.resume()
    }

    private fun applyPosition() {
        val lp = params ?: return
        val v = view ?: return
        val cfg = IslandSettings.config.value
        val d = resources.displayMetrics.density
        lp.x = (cfg.offsetXDp * d).toInt()
        lp.y = statusBarHeightPx() + (cfg.topGapDp * d).toInt()
        if (v.isAttachedToWindow) runCatching { windowManager?.updateViewLayout(v, lp) }
    }

    private fun statusBarHeightPx(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            ?.let { startActivity(it) }
    }

    private fun removeOverlay() {
        scope?.cancel(); scope = null
        owner?.destroy(); owner = null
        view?.let { v -> runCatching { windowManager?.removeViewImmediate(v) } }
        view = null
    }

    /** Pemilik lifecycle minimal supaya Compose bisa hidup di dalam window layanan. */
    private class OverlayOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val viewModelStore = ViewModelStore()
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        fun create() {
            controller.performAttach()
            controller.performRestore(null)
            registry.currentState = Lifecycle.State.CREATED
        }

        fun resume() { registry.currentState = Lifecycle.State.RESUMED }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }
}
