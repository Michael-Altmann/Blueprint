/*
 * Copyright (c) 2018. Jahir Fiquitiva
 *
 * Licensed under the CreativeCommons Attribution-ShareAlike
 * 4.0 International License. You may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *    http://creativecommons.org/licenses/by-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jahirfiquitiva.libs.blueprint.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import jahirfiquitiva.libs.archhelpers.ui.fragments.ViewModelFragment
import jahirfiquitiva.libs.blueprint.R
import jahirfiquitiva.libs.blueprint.data.models.Icon
import jahirfiquitiva.libs.blueprint.data.models.IconsCategory
import jahirfiquitiva.libs.blueprint.helpers.extensions.bpKonfigs
import jahirfiquitiva.libs.blueprint.helpers.extensions.getUriFromResource
import jahirfiquitiva.libs.blueprint.providers.viewmodels.IconsViewModel
import jahirfiquitiva.libs.blueprint.ui.adapters.IconsAdapter
import jahirfiquitiva.libs.blueprint.ui.fragments.dialogs.IconDialog
import jahirfiquitiva.libs.frames.helpers.utils.ICONS_PICKER
import jahirfiquitiva.libs.frames.helpers.utils.IMAGE_PICKER
import jahirfiquitiva.libs.frames.ui.widgets.EmptyViewRecyclerView
import jahirfiquitiva.libs.kauextensions.extensions.actv
import jahirfiquitiva.libs.kauextensions.extensions.ctxt
import jahirfiquitiva.libs.kauextensions.extensions.getInteger
import jahirfiquitiva.libs.kauextensions.extensions.getUri
import jahirfiquitiva.libs.kauextensions.extensions.hasContent
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class IconsFragment : ViewModelFragment<Icon>() {
    
    override fun autoStartLoad(): Boolean = true
    
    private var pickerKey = 0
    
    companion object {
        fun create(key: Int) = IconsFragment().apply { pickerKey = key }
    }
    
    private var model: IconsViewModel? = null
    private var rv: EmptyViewRecyclerView? = null
    private var fastScroller: RecyclerFastScroller? = null
    
    private var dialog: IconDialog? = null
    
    private val adapter: IconsAdapter by lazy { IconsAdapter(false) { onItemClicked(it, false) } }
    
    fun applyFilters(filters: ArrayList<String>) {
        val list = ArrayList(model?.getData().orEmpty())
        if (filters.isNotEmpty()) {
            setAdapterItems(ArrayList(list.filter { validFilter(it.title, filters) }))
        } else {
            setAdapterItems(ArrayList(list))
        }
    }
    
    fun doSearch(search: String = "") {
        model?.getData()?.let {
            setAdapterItems(ArrayList(it), search)
        }
    }
    
    override fun initViewModel() {
        model = ViewModelProviders.of(this).get(IconsViewModel::class.java)
    }
    
    override fun registerObserver() {
        model?.observe(this) {
            setAdapterItems(ArrayList(it))
        }
    }
    
    private fun validFilter(title: String, filters: ArrayList<String>): Boolean {
        filters.forEach { if (title.equals(it, true)) return true }
        return false
    }
    
    private fun setAdapterItems(categories: ArrayList<IconsCategory>, filteredBy: String = "") {
        val icons = ArrayList<Icon>()
        categories.forEach {
            val category = it
            if (filteredBy.hasContent())
                icons.addAll(
                        it.icons.filter {
                            val deep = context?.bpKonfigs?.deepSearchEnabled ?: false
                            if (deep) {
                                it.name.contains(filteredBy, true) ||
                                        category.title.contains(filteredBy, true)
                            } else {
                                it.name.contains(filteredBy, true)
                            }
                        })
            else icons.addAll(it.icons)
        }
        adapter.setItems(ArrayList(icons.distinct().sorted()))
        rv?.state = EmptyViewRecyclerView.State.NORMAL
    }
    
    override fun unregisterObserver() {
        model?.destroy(this)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        actv { dialog?.dismiss(it, IconDialog.TAG) }
    }
    
    @SuppressLint("MissingSuperCall")
    override fun onDestroy() {
        super.onDestroy()
        actv { dialog?.dismiss(it, IconDialog.TAG) }
    }
    
    override fun loadDataFromViewModel() = actv { model?.loadData(it) }
    
    override fun getContentLayout(): Int = R.layout.section_layout
    
    override fun initUI(content: View) {
        rv = content.findViewById(R.id.list_rv)
        fastScroller = content.findViewById(R.id.fast_scroller)
        rv?.emptyView = content.findViewById(R.id.empty_view)
        rv?.textView = content.findViewById(R.id.empty_text)
        rv?.adapter = adapter
        val columns = ctxt.getInteger(R.integer.icons_columns)
        rv?.layoutManager = GridLayoutManager(context, columns, GridLayoutManager.VERTICAL, false)
        rv?.state = EmptyViewRecyclerView.State.LOADING
        fastScroller?.attachRecyclerView(rv)
    }
    
    override fun onItemClicked(item: Icon, longClick: Boolean) {
        if (!longClick) {
            if (pickerKey != 0) {
                pickIcon(item)
            } else {
                actv {
                    dialog?.dismiss(it, IconDialog.TAG)
                    dialog = IconDialog()
                    dialog?.show(it, item.name, item.icon, it.bpKonfigs.animationsEnabled)
                }
            }
        }
    }
    
    private fun pickIcon(item: Icon) {
        actv { activity ->
            val intent = Intent()
            val bitmap: Bitmap? = try {
                val drawable =
                        ResourcesCompat.getDrawable(resources, item.icon, null) as BitmapDrawable?
                drawable?.bitmap ?: BitmapFactory.decodeResource(resources, item.icon)
            } catch (e: Exception) {
                null
            }
            
            if (bitmap != null) {
                if (pickerKey == ICONS_PICKER) {
                    intent.putExtra("icon", bitmap)
                    val iconRes = Intent.ShortcutIconResource.fromContext(activity, item.icon)
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes)
                } else if (pickerKey == IMAGE_PICKER) {
                    var uri: Uri? = null
                    val icon = File(activity.cacheDir, item.name + ".png")
                    val fos: FileOutputStream
                    try {
                        fos = FileOutputStream(icon)
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush()
                        fos.close()
                        uri = icon.getUri(activity)
                    } catch (ignored: Exception) {
                    }
                    
                    if (uri == null) {
                        try {
                            uri = activity.getUriFromResource(item.icon)
                        } catch (e: Exception) {
                            try {
                                uri = Uri.parse(
                                        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://" +
                                                "${activity.packageName}/${item.icon}")
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                    if (uri != null) {
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.data = uri
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    intent.putExtra("return-data", false)
                }
                activity.setResult(Activity.RESULT_OK, intent)
            } else {
                activity.setResult(Activity.RESULT_CANCELED, intent)
            }
            bitmap?.let {
                if (!it.isRecycled) it.recycle()
            }
            activity.finish()
        }
    }
}