package com.techtravelcoder.dailynote.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PostPDFGenerator
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFade
import com.techtravelcoder.dailynote.MenuDialog
import com.techtravelcoder.dailynote.R
import com.techtravelcoder.dailynote.databinding.ActivityMainBinding
import com.techtravelcoder.dailynote.databinding.DialogColorBinding
import com.techtravelcoder.dailynote.miscellaneous.Operations
import com.techtravelcoder.dailynote.miscellaneous.applySpans
import com.techtravelcoder.dailynote.recyclerview.ItemListener
import com.techtravelcoder.dailynote.recyclerview.adapter.ColorAdapter
import com.techtravelcoder.dailynote.room.BaseNote
import com.techtravelcoder.dailynote.room.Color
import com.techtravelcoder.dailynote.room.Folder
import com.techtravelcoder.dailynote.room.Type
import com.techtravelcoder.dailynote.viewmodels.BaseNoteModel
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var configuration: AppBarConfiguration

    private val model: BaseNoteModel by viewModels()

    override fun onBackPressed() {
        if (model.actionMode.enabled.value) {
            model.actionMode.close(true)
        } else super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // Apply custom font to title
        setSupportActionBar(binding.Toolbar)


        setupFAB()
        setupMenu()
        setupActionMode()
        setupNavigation()
        setupSearch()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EXPORT_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                model.writeCurrentFileToUri(uri)
            }
        }
    }


    private fun setupFAB() {
        binding.TakeNote.setOnClickListener {
            val intent = Intent(this, TakeNote::class.java)
            startActivity(intent)
        }
        binding.MakeList.setOnClickListener {
            val intent = Intent(this, MakeList::class.java)
            startActivity(intent)
        }
    }

    private fun setupMenu()  {
        val menu = binding.NavigationView.menu
        menu.add(0, R.id.Notes, 0, R.string.notes).setCheckable(true).setIcon(R.drawable.home)
        menu.add(1, R.id.Labels, 0, R.string.labels).setCheckable(true).setIcon(R.drawable.label)
        menu.add(2, R.id.Settings, 0, R.string.settings).setCheckable(true).setIcon(R.drawable.settings)

        menu.add(3, R.id.Deleted, 0, R.string.deleted).setCheckable(true).setIcon(R.drawable.delete)
        menu.add(4, R.id.Archived, 0, R.string.archived).setCheckable(true).setIcon(R.drawable.archive)

    }


    private fun setupActionMode() {
        // Close action mode on navigation click
        binding.ActionMode.setNavigationOnClickListener { model.actionMode.close(true) }

        // Set up MaterialFade transition
        val transition = MaterialFade().apply {
            secondaryAnimatorProvider = null
            excludeTarget(binding.NavHostFragment, true)
            excludeChildren(binding.NavHostFragment, true)
            excludeTarget(binding.TakeNote, true)
            excludeTarget(binding.MakeList, true)
            excludeTarget(binding.NavigationView, true)
        }

        // Observe action mode state
        model.actionMode.enabled.observe(this) { enabled ->
            TransitionManager.beginDelayedTransition(binding.RelativeLayout, transition)
            if (enabled) {
                binding.Toolbar.visibility = View.GONE
                binding.ActionMode.visibility = View.VISIBLE
                binding.DrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                binding.Toolbar.visibility = View.VISIBLE
                binding.ActionMode.visibility = View.GONE
                binding.DrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        val menu = binding.ActionMode.menu

        // Helper function to add menu items with actions
        fun addMenuItem(titleRes: Int, iconRes: Int, action: () -> Unit): MenuItem {
            return menu.add(0, titleRes, Menu.NONE, titleRes).apply {
                setIcon(iconRes)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                setOnMenuItemClickListener {
                    action()
                    true
                }
            }
        }

        val export = createExportMenu(menu)

        // Menu items
        val pinned = addMenuItem(R.string.pin, R.drawable.pin) { model.pinBaseNote(true) }
        val share = addMenuItem(R.string.share, R.drawable.share) { share() }
        val labels = addMenuItem(R.string.labels, R.drawable.label) { label() }
        val changeColor = addMenuItem(R.string.change_color, R.drawable.change_color) { changeColor() }
        val copy = addMenuItem(R.string.make_a_copy, R.drawable.copy) {

            MaterialAlertDialogBuilder(this)
                .setTitle("Copy Notes")
                .setMessage("Are you sure to copy this selected notes ?")
                .setPositiveButton("Yes") { _, _ -> model.copyBaseNote() }
                .setNegativeButton("No", null)
                .show()

        }
        val delete = addMenuItem(R.string.delete, R.drawable.delete) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Notes")
                .setMessage("Are you sure to delete this selected notes ?")
                .setPositiveButton("Yes") { _, _ ->  model.moveBaseNotes(Folder.DELETED)  }
                .setNegativeButton("No", null)
                .show()
           }
        val archive = addMenuItem(R.string.archive, R.drawable.archive) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Archive Notes")
                .setMessage("Are you sure to archive this selected notes ?")
                .setPositiveButton("Yes") { _, _ ->  model.moveBaseNotes(Folder.ARCHIVED)  }
                .setNegativeButton("No", null)
                .show()

        }
        val restore = addMenuItem(R.string.restore, R.drawable.restore) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Restore Notes")
                .setMessage("Are you sure to restore this selected notes ?")
                .setPositiveButton("Yes") { _, _ ->   model.moveBaseNotes(Folder.NOTES) }
                .setNegativeButton("No", null)
                .show()
        }
        val unarchive = addMenuItem(R.string.unarchive, R.drawable.unarchive) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Unarchive Notes")
                .setMessage("Are you sure to unarchive this selected notes ?")
                .setPositiveButton("Yes") { _, _ ->   model.moveBaseNotes(Folder.NOTES) }
                .setNegativeButton("No", null)
                .show()

        }
        val deleteForever = addMenuItem(R.string.delete_forever, R.drawable.delete) { deleteForever() }


        // Create export menu item

        // Observe selected item count
        model.actionMode.count.observe(this) { count ->
            if (count == 0) {
                menu.forEach { it.isVisible = false }
            } else {
                binding.ActionMode.title = count.toString()

                val baseNote = model.actionMode.getFirstNote()

                // Handle pin/unpin visibility and action for a single item
                if (count == 1) {
                    if (baseNote.pinned) {
                        pinned.setTitle(R.string.unpin)
                        pinned.setIcon(R.drawable.unpin)
                    } else {
                        pinned.setTitle(R.string.pin)
                        pinned.setIcon(R.drawable.pin)
                    }
                    pinned.setOnMenuItemClickListener {
                        model.pinBaseNote(!baseNote.pinned)
                        true
                    }
                }

                // Set visibility of menu items
                pinned.isVisible = count == 1
                share.isVisible = count == 1
                labels.isVisible = count == 1
                export.isVisible = count == 1

                changeColor.isVisible = true
                copy.isVisible = true

                when (baseNote.folder) {
                    Folder.NOTES -> {
                        delete.isVisible = true
                        archive.isVisible = true
                        restore.isVisible = false
                        unarchive.isVisible = false
                        deleteForever.isVisible = false
                    }
                    Folder.ARCHIVED -> {
                        delete.isVisible = true
                        archive.isVisible = false
                        restore.isVisible = false
                        unarchive.isVisible = true
                        deleteForever.isVisible = false
                    }
                    Folder.DELETED -> {
                        delete.isVisible = false
                        archive.isVisible = false
                        restore.isVisible = true
                        unarchive.isVisible = false
                        deleteForever.isVisible = true
                    }
                }
            }
        }
    }
    private fun createExportMenu(menu: Menu): MenuItem {
        val export = menu.addSubMenu(R.string.export)
        export.setIcon(R.drawable.export)
        export.item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        export.add("PDF").onClick { exportToPDF() }
        export.add("TXT").onClick { exportToTXT() }
        export.add("JSON").onClick { exportToJSON() }
        export.add("HTML").onClick { exportToHTML() }

        return export.item
    }

    fun MenuItem.onClick(function: () -> Unit) {
        setOnMenuItemClickListener {
            function()
            return@setOnMenuItemClickListener false
        }
    }


    private fun share() {
        val baseNote = model.actionMode.getFirstNote()
        val body = when (baseNote.type) {
            Type.NOTE -> baseNote.body.applySpans(baseNote.spans)
            Type.LIST -> Operations.getBody(baseNote.items)
        }
        Operations.shareNote(this, baseNote.title, body)
    }

    private fun changeColor() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.change_color)
            .create()

        val colorAdapter = ColorAdapter(object : ItemListener {
            override fun onClick(position: Int) {
                dialog.dismiss()
                val color = Color.entries[position]
                model.colorBaseNote(color)
            }
            override fun onLongClick(position: Int) {}
        })

        val dialogBinding = DialogColorBinding.inflate(layoutInflater)
        dialogBinding.RecyclerView.adapter = colorAdapter

        dialog.setView(dialogBinding.root)
        dialog.show()
    }

    private fun deleteForever() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Notes")
            .setMessage("Are you sure to delete this selected notes forever ?")
            .setPositiveButton("Yes") { _, _ -> model.deleteBaseNotes() }
            .setNegativeButton("No", null)
            .show()
    }


    private fun label() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val labels = model.getAllLabels()
            if (labels.isNotEmpty()) {
                displaySelectLabelsDialog(labels, baseNote)
            } else {
                model.actionMode.close(true)
                navigateWithAnimation(R.id.Labels)
            }
        }
    }

    private fun displaySelectLabelsDialog(labels: Array<String>, baseNote: BaseNote) {
        val checkedPositions = BooleanArray(labels.size) { index -> baseNote.labels.contains(labels[index]) }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.labels)
            .setNegativeButton(R.string.cancel, null)
            .setMultiChoiceItems(labels, checkedPositions) { _, which, isChecked -> checkedPositions[which] = isChecked }
            .setPositiveButton(R.string.save) { _, _ ->
                val new = ArrayList<String>()
                checkedPositions.forEachIndexed { index, checked ->
                    if (checked) {
                        val label = labels[index]
                        new.add(label)
                    }
                }
                model.updateBaseNoteLabels(new, baseNote.id)
            }
            .show()
    }


    private fun exportToPDF() {
        val baseNote = model.actionMode.getFirstNote()
        model.getPDFFile(baseNote, object : PostPDFGenerator.OnResult {

            override fun onSuccess(file: File) {
                showFileOptionsDialog(file, "application/pdf")
            }

            override fun onFailure(message: CharSequence?) {
                Toast.makeText(this@MainActivity, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun exportToTXT() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val file = model.getTXTFile(baseNote)
            showFileOptionsDialog(file, "text/plain")
        }
    }

    private fun exportToJSON() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val file = model.getJSONFile(baseNote)
            showFileOptionsDialog(file, "application/json")
        }
    }

    private fun exportToHTML() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val file = model.getHTMLFile(baseNote)
            showFileOptionsDialog(file, "text/html")
        }
    }


    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

        MenuDialog(this)
            .add(R.string.share, R.drawable.delete) { shareFile(uri, mimeType) }
            .add(R.string.view_file, R.drawable.delete) { viewFile(uri, mimeType) }
            .add(R.string.save_to_device, R.drawable.delete) { saveFileToDevice(file, mimeType) }
            .show()
    }

    private fun viewFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        val chooser = Intent.createChooser(intent, getString(R.string.view_note))
        startActivity(chooser)
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_STREAM, uri)

        val chooser = Intent.createChooser(intent, null)
        startActivity(chooser)
    }

    private fun saveFileToDevice(file: File, mimeType: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = mimeType
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)

        model.currentFile = file
        startActivityForResult(intent, REQUEST_EXPORT_FILE)
    }


    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.NavHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        configuration = AppBarConfiguration(binding.NavigationView.menu, binding.DrawerLayout)
        setupActionBarWithNavController(navController, configuration)

        var fragmentIdToLoad: Int? = null
        binding.NavigationView.setNavigationItemSelectedListener { item ->
            fragmentIdToLoad = item.itemId
            binding.DrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        binding.DrawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerClosed(drawerView: View) {
                if (fragmentIdToLoad != null && navController.currentDestination?.id != fragmentIdToLoad) {
                    navigateWithAnimation(requireNotNull(fragmentIdToLoad))
                }
            }
        })

        navController.addOnDestinationChangedListener { _, destination, _ ->
            fragmentIdToLoad = destination.id
            binding.NavigationView.setCheckedItem(destination.id)
            handleDestinationChange(destination)
        }
    }

    private fun handleDestinationChange(destination: NavDestination) {
        if (destination.id == R.id.Notes) {
            binding.TakeNote.show()
            binding.MakeList.show()
        } else {
            binding.TakeNote.hide()
            binding.MakeList.hide()
        }

        val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (destination.id == R.id.Search) {
            binding.EnterSearchKeyword.visibility = View.VISIBLE
            binding.EnterSearchKeyword.requestFocus()
            inputManager.showSoftInput(binding.EnterSearchKeyword, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.EnterSearchKeyword.visibility = View.GONE
            inputManager.hideSoftInputFromWindow(binding.EnterSearchKeyword.windowToken, 0)
        }
    }

    private fun navigateWithAnimation(id: Int) {
        val options = navOptions {
            launchSingleTop = true
            anim {
                exit = androidx.navigation.ui.R.anim.nav_default_exit_anim
                enter = androidx.navigation.ui.R.anim.nav_default_enter_anim
                popExit = androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
                popEnter = androidx.navigation.ui.R.anim.nav_default_pop_enter_anim
            }
            popUpTo(navController.graph.startDestination) { inclusive = false }
        }
        navController.navigate(id, null, options)
    }


    private fun setupSearch() {
        binding.EnterSearchKeyword.setText(model.keyword)
        binding.EnterSearchKeyword.doAfterTextChanged { text ->
            model.keyword = requireNotNull(text).trim().toString()
        }
    }

    companion object {
        private const val REQUEST_EXPORT_FILE = 10
    }
}