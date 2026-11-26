package cn.edu.pku.openrunner.feature.hike.ui

import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.edu.pku.openrunner.R
import com.google.android.material.button.MaterialButton
import cn.edu.pku.openrunner.ui.openRunnerDialog
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import java.text.DateFormat
import java.util.Date

internal class HikeFilesPanel(
    private val root: View,
    private val model: HikeViewModel,
    private val close: () -> Unit,
    importRoute: () -> Unit,
    cacheMap: () -> Unit
) {
    private val context = root.context
    private val tabs = root.findViewById<TabLayout>(R.id.hike_files_tabs)
    private val list = root.findViewById<RecyclerView>(R.id.hike_files_list)
    private val action = root.findViewById<MaterialButton>(R.id.hike_files_action)
    private val progress = root.findViewById<LinearProgressIndicator>(R.id.hike_files_progress)
    private val progressLabel = root.findViewById<TextView>(R.id.hike_files_progress_label)
    private val empty = root.findViewById<TextView>(R.id.hike_files_empty_label)
    private val adapter = FileAdapter(::open, ::showOptions)
    private val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private var page = model.page.value
    private var visibleMapKey: String? = null
    private var popup: PopupMenu? = null
    private var deleteDialog: AlertDialog? = null

    val showingMaps: Boolean get() = tabs.selectedTabPosition == Section.MAPS.ordinal

    init {
        list.adapter = adapter
        root.findViewById<View>(R.id.hike_files_back).setOnClickListener { close() }
        ViewCompat.setAccessibilityHeading(root.findViewById(R.id.hike_files_title), true)
        Section.entries.forEach { tabs.addTab(tabs.newTab().setText(it.label)) }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                list.scrollToPosition(0)
                update()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        action.setOnClickListener {
            if (showingMaps) cacheMap() else importRoute()
        }
    }

    fun show(maps: Boolean) {
        tabs.getTabAt(if (maps) Section.MAPS.ordinal else Section.ROUTES.ordinal)?.select()
        root.isVisible = true
        root.findViewById<View>(R.id.hike_files_back).requestFocus()
    }

    fun hide() {
        popup?.dismiss()
        deleteDialog?.dismiss()
        root.isVisible = false
    }

    fun render(page: HikePage, visibleMapKey: String?) {
        this.page = page
        this.visibleMapKey = visibleMapKey
        update()
    }

    private fun update() {
        val maps = showingMaps
        val download = page.progress.takeIf { maps }
        val importing = page.importing && !maps
        action.isEnabled = !importing
        action.setText(when {
            download != null -> R.string.hike_cancel_download
            maps -> R.string.hike_cache
            importing -> R.string.hike_importing
            else -> R.string.hike_import
        })
        progress.isVisible = download != null
        progressLabel.isVisible = download != null
        if (download != null) {
            val (complete, total) = download
            progress.max = total
            progress.setProgressCompat(complete, false)
            progressLabel.text = context.getString(R.string.hike_cache_progress, complete, total)
        }

        val files = if (maps) page.maps.map { item ->
            FileEntry(Section.MAPS, item.key, item.label,
                context.getString(R.string.hike_map_tile_count, item.terrain.size), item.key == visibleMapKey)
        } else page.routes.entries.map { route ->
            FileEntry(Section.ROUTES, route.id, route.title,
                context.getString(R.string.hike_file_metadata, route.fileName, format.format(Date(route.importedAt))),
                route.id == page.routes.activeId && page.route != null)
        }
        empty.setText(if (maps) R.string.hike_files_map_empty else R.string.hike_routes_empty)
        empty.isVisible = files.isEmpty() && download == null && !importing
        list.isVisible = files.isNotEmpty()
        adapter.submitList(files)
    }

    private fun open(entry: FileEntry) {
        when (entry.section) {
            Section.ROUTES -> {
                close()
                model.selectRoute(entry.key)
            }
            Section.MAPS -> page.maps.firstOrNull { it.key == entry.key }?.let {
                close()
                model.openOfflineMap(it)
            }
        }
    }

    private fun showOptions(anchor: View, entry: FileEntry) {
        popup?.dismiss()
        popup = PopupMenu(context, anchor, Gravity.END, 0, R.style.Widget_OpenRunner_PopupMenu).apply {
            if (entry.section == Section.ROUTES && entry.active) {
                menu.add(R.string.hike_file_hide).setOnMenuItemClickListener {
                    if (model.page.value.routes.activeId == entry.key) model.selectRoute(null)
                    true
                }
            }
            menu.add(R.string.hike_file_delete).setOnMenuItemClickListener {
                confirmDelete(entry)
                true
            }
            setOnDismissListener { popup = null }
            show()
        }
    }

    private fun confirmDelete(entry: FileEntry) {
        deleteDialog?.dismiss()
        deleteDialog = context.openRunnerDialog()
            .setTitle(context.getString(R.string.hike_file_delete_confirm, entry.title))
            .setNegativeButton(R.string.hike_cancel, null)
            .setPositiveButton(R.string.hike_file_delete) { _, _ ->
                when (entry.section) {
                    Section.ROUTES -> model.deleteRoute(entry.key)
                    Section.MAPS -> page.maps.firstOrNull { it.key == entry.key }?.let(model::deleteMap)
                }
            }
            .setOnDismissListener { deleteDialog = null }
            .show()
    }

    private enum class Section(val label: Int) {
        ROUTES(R.string.hike_kml_reference), MAPS(R.string.hike_offline_maps)
    }

    private data class FileEntry(
        val section: Section,
        val key: String,
        val title: String,
        val details: String,
        val active: Boolean
    )

    private class FileAdapter(
        private val open: (FileEntry) -> Unit,
        private val options: (View, FileEntry) -> Unit
    ) : ListAdapter<FileEntry, FileHolder>(object : DiffUtil.ItemCallback<FileEntry>() {
        override fun areItemsTheSame(old: FileEntry, new: FileEntry): Boolean =
            old.key == new.key && old.section == new.section
        override fun areContentsTheSame(old: FileEntry, new: FileEntry): Boolean = old == new
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
            val row = LayoutInflater.from(parent.context).inflate(R.layout.item_hike_file, parent, false)
            return FileHolder(row)
        }

        override fun onBindViewHolder(holder: FileHolder, position: Int) {
            val entry = getItem(position)
            holder.title.text = entry.title
            holder.details.text = entry.details
            holder.status.isVisible = entry.active
            holder.open.setOnClickListener { open(entry) }
            holder.more.contentDescription = holder.more.context.getString(R.string.hike_file_more, entry.title)
            holder.more.setOnClickListener { options(it, entry) }
        }
    }

    private class FileHolder(row: View) : RecyclerView.ViewHolder(row) {
        val title: TextView = row.findViewById(R.id.hike_file_title)
        val details: TextView = row.findViewById(R.id.hike_file_details)
        val status: TextView = row.findViewById(R.id.hike_file_status)
        val open: View = row.findViewById(R.id.hike_file_open)
        val more: View = row.findViewById(R.id.hike_file_more)

        init {
            ViewCompat.setScreenReaderFocusable(open, true)
        }
    }
}
