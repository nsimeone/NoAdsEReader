package com.example.noadse_reader

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import com.example.noadse_reader.ui.theme.NoAdsEReaderTheme
import java.io.File
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.publication.services.locateProgression

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity() {

    private var controlsVisible by mutableStateOf(false)
    private var progress by mutableFloatStateOf(0f)
    private var fontSize by mutableDoubleStateOf(1.0)
    private var pageTheme by mutableStateOf(PageTheme.LIGHT)
    private var bookmarks by mutableStateOf<List<BookmarkEntity>>(emptyList())
    private var latestLocator: Locator? = null

    private fun currentPreferences() = EpubPreferences(
        fontSize = fontSize,
        theme = when (pageTheme) {
            PageTheme.LIGHT -> Theme.LIGHT
            PageTheme.SEPIA -> Theme.SEPIA
            PageTheme.DARK -> Theme.DARK
        }
    )

    private fun systemTheme(): PageTheme {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (mode == Configuration.UI_MODE_NIGHT_YES) PageTheme.DARK else PageTheme.LIGHT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        val containerId = View.generateViewId()
        val container = FragmentContainerView(this).apply {
            id = containerId
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val overlay = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val root = FrameLayout(this).apply {
            addView(container)
            addView(overlay)
        }
        setContentView(root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
        if (fileName == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            val db = LibraryDatabase.get(this@ReaderActivity)
            val dao = db.books()
            val bookmarkDao = db.bookmarks()
            val settings = ReaderSettings(this@ReaderActivity)
            val file = File(File(filesDir, "books"), fileName)
            val publication = openPublication(this@ReaderActivity, file)
            if (publication == null) {
                Toast.makeText(
                    this@ReaderActivity,
                    "Couldn't open this book.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@launch
            }

            fontSize = settings.fontSize
            pageTheme = settings.theme ?: systemTheme()

            val savedJson = dao.getLocator(fileName)
            val startLocator = savedJson?.let { Locator.fromJSON(JSONObject(it)) }
            progress = (startLocator?.locations?.totalProgression ?: 0.0).toFloat()

            val navigatorFactory = EpubNavigatorFactory(publication)
            supportFragmentManager.fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = startLocator,
                initialPreferences = currentPreferences(),
                listener = object : EpubNavigatorFragment.Listener {
                    override fun onExternalLinkActivated(url: AbsoluteUrl) {
                        // Web links inside a book are ignored for now.
                    }
                }
            )

            supportFragmentManager.beginTransaction()
                .add(containerId, EpubNavigatorFragment::class.java, Bundle(), "reader")
                .commitNow()

            val navigator = supportFragmentManager.findFragmentByTag("reader") as EpubNavigatorFragment

            (navigator as? OverflowableNavigator)?.apply {
                addInputListener(DirectionalNavigationAdapter(this))
            }
            navigator.addInputListener(object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
                    val width = navigator.view?.width ?: return false
                    val x = event.point.x
                    if (x < width * 0.3f || x > width * 0.7f) return false
                    controlsVisible = !controlsVisible
                    return true
                }
            })

            launch {
                bookmarkDao.observeFor(fileName).collect { bookmarks = it }
            }

            overlay.setContent {
                NoAdsEReaderTheme {
                    ReaderControls(
                        visible = controlsVisible,
                        progress = progress,
                        fontSize = fontSize,
                        theme = pageTheme,
                        bookmarks = bookmarks,
                        onFontSize = { newSize ->
                            fontSize = newSize.coerceIn(0.7, 2.5)
                            settings.fontSize = fontSize
                            navigator.submitPreferences(currentPreferences())
                        },
                        onTheme = { newTheme ->
                            pageTheme = newTheme
                            settings.theme = newTheme
                            navigator.submitPreferences(currentPreferences())
                        },
                        onSeek = { value ->
                            lifecycleScope.launch {
                                publication.locateProgression(value.toDouble())
                                    ?.let { navigator.go(it) }
                            }
                        },
                        onGoToStart = {
                            lifecycleScope.launch {
                                publication.locateProgression(0.0)
                                    ?.let { navigator.go(it) }
                            }
                        },
                        onAddBookmark = {
                            val locator = latestLocator
                            if (locator != null) {
                                lifecycleScope.launch {
                                    bookmarkDao.insert(
                                        BookmarkEntity(
                                            bookFileName = fileName,
                                            locator = locator.toJSON().toString(),
                                            label = locator.title
                                                ?.takeIf { it.isNotBlank() }
                                                ?: "Bookmark",
                                            progress = locator.locations.totalProgression,
                                            createdAt = System.currentTimeMillis()
                                        )
                                    )
                                    Toast.makeText(
                                        this@ReaderActivity,
                                        "Bookmark added",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onOpenBookmark = { bookmark ->
                            controlsVisible = false
                            lifecycleScope.launch {
                                Locator.fromJSON(JSONObject(bookmark.locator))
                                    ?.let { navigator.go(it) }
                            }
                        },
                        onDeleteBookmark = { bookmark ->
                            lifecycleScope.launch { bookmarkDao.delete(bookmark.id) }
                        },
                        onClose = { finish() }
                    )
                }
            }

            navigator.currentLocator.collect { locator ->
                latestLocator = locator
                locator.locations.totalProgression?.let { progress = it.toFloat() }
                dao.saveLocator(
                    fileName,
                    locator.toJSON().toString(),
                    locator.locations.totalProgression
                )
            }
        }
    }

    companion object {
        private const val EXTRA_FILE_NAME = "fileName"

        fun start(context: Context, fileName: String) {
            context.startActivity(
                Intent(context, ReaderActivity::class.java).putExtra(EXTRA_FILE_NAME, fileName)
            )
        }
    }
}