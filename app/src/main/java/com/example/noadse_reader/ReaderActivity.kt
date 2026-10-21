package com.example.noadse_reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        val containerId = View.generateViewId()
        val container = FragmentContainerView(this).apply {
            id = containerId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)

        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
        if (fileName == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            val dao = LibraryDatabase.get(this@ReaderActivity).books()
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

            val savedJson = dao.getLocator(fileName)
            val startLocator = savedJson?.let { Locator.fromJSON(JSONObject(it)) }

            val navigatorFactory = EpubNavigatorFactory(publication)
            supportFragmentManager.fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = startLocator,
                listener = object : EpubNavigatorFragment.Listener {
                    override fun onExternalLinkActivated(url: AbsoluteUrl) {
                        // Web links inside a book are ignored for now.
                    }
                }
            )

            supportFragmentManager.beginTransaction()
                .add(containerId, EpubNavigatorFragment::class.java, Bundle(), "reader")
                .commitNow()

            val navigator = supportFragmentManager.findFragmentByTag("reader") as Navigator
            (navigator as? OverflowableNavigator)?.apply {
                addInputListener(DirectionalNavigationAdapter(this))
            }

            navigator.currentLocator.collect { locator ->
                dao.saveLocator(fileName, locator.toJSON().toString())
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