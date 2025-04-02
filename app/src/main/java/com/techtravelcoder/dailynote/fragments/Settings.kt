package com.techtravelcoder.dailynote.fragments

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.techtravelcoder.dailynote.MenuDialog
import com.techtravelcoder.dailynote.Progress
import com.techtravelcoder.dailynote.R
import com.techtravelcoder.dailynote.databinding.DialogProgressBinding
import com.techtravelcoder.dailynote.databinding.FragmentSettingsBinding
import com.techtravelcoder.dailynote.databinding.PreferenceBinding
import com.techtravelcoder.dailynote.databinding.PreferenceSeekbarBinding
import com.techtravelcoder.dailynote.preferences.*
import com.techtravelcoder.dailynote.viewmodels.BaseNoteModel

class Settings : Fragment() {

    private val model: BaseNoteModel by activityViewModels()

    private fun setupBinding(binding: FragmentSettingsBinding) {
        model.preferences.view.observe(viewLifecycleOwner) { value ->
            binding.View.setup(View, value)
        }

        model.preferences.theme.observe(viewLifecycleOwner) { value ->
            binding.Theme.setup(Theme, value)
        }

        model.preferences.dateFormat.observe(viewLifecycleOwner) { value ->
            binding.DateFormat.setup(DateFormat, value)
        }

        model.preferences.textSize.observe(viewLifecycleOwner) { value ->
            binding.TextSize.setup(TextSize, value)
        }


        binding.MaxItems.setup(MaxItems, model.preferences.maxItems)

        binding.MaxLines.setup(MaxLines, model.preferences.maxLines)

        binding.MaxTitle.setup(MaxTitle, model.preferences.maxTitle)


        model.preferences.autoBackup.observe(viewLifecycleOwner) { value ->
            binding.AutoBackup.setup(AutoBackup, value)
        }

        binding.ImportBackup.setOnClickListener {
            importBackup()
        }

        binding.ExportBackup.setOnClickListener {
            exportBackup()
        }
        binding.PrivacyPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bscengineeringbook.blogspot.com/p/privacy-policy.html"))
            startActivity(intent)
        }


        setupProgressDialog(R.string.exporting_backup, model.exportingBackup)
        setupProgressDialog(R.string.importing_backup, model.importingBackup)





    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSettingsBinding.inflate(inflater)
        setupBinding(binding)
        return binding.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            intent?.data?.let { uri ->
                when (requestCode) {
                    REQUEST_IMPORT_BACKUP -> model.importBackup(uri)
                    REQUEST_EXPORT_BACKUP -> model.exportBackup(uri)
                    REQUEST_CHOOSE_FOLDER -> model.setAutoBackupPath(uri)
                }
            }
        }
    }


    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, "Notally Backup")
        startActivityForResult(intent, REQUEST_EXPORT_BACKUP)
    }

    private fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "text/xml"))
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, REQUEST_IMPORT_BACKUP)
    }

    private fun setupProgressDialog(titleId: Int, liveData: MutableLiveData<Progress>) {
        val dialogBinding = DialogProgressBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleId)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        liveData.observe(viewLifecycleOwner) { progress ->
            if (progress.inProgress) {
                if (progress.indeterminate) {
                    dialogBinding.ProgressBar.isIndeterminate = true
                    dialogBinding.Count.setText(R.string.calculating)
                } else {
                    dialogBinding.ProgressBar.max = progress.total
                    dialogBinding.ProgressBar.setProgressCompat(progress.current, true)
                    dialogBinding.Count.text = getString(R.string.count, progress.current, progress.total)
                }
                dialog.show()
            } else dialog.dismiss()
        }
    }




    private fun displayChooseFolderDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.notes_will_be)
            .setPositiveButton(R.string.choose_folder) { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, REQUEST_CHOOSE_FOLDER)
            }
            .show()
    }


    private fun PreferenceBinding.setup(info: ListInfo, value: String) {
        Title.setText(info.title)

        val entries = info.getEntries(requireContext())
        val entryValues = info.getEntryValues()

        val checked = entryValues.indexOf(value)
        if(checked!=-1){
            val displayValue = entries[checked]
            Value.text = displayValue
        }



        root.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(info.title)
                .setSingleChoiceItems(entries, checked) { dialog, which ->
                    dialog.cancel()
                    val newValue = entryValues[which]
                    model.savePreference(info, newValue)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun PreferenceBinding.setup(info: AutoBackup, value: String) {
        Title.setText(info.title)

        if (value == info.emptyPath) {
            Value.setText(R.string.tap_to_set_up)

            root.setOnClickListener { displayChooseFolderDialog() }
        } else {
            val uri = Uri.parse(value)
            val folder = requireNotNull(DocumentFile.fromTreeUri(requireContext(), uri))
            if (folder.exists()) {
                Value.text = folder.name
            } else Value.setText(R.string.cant_find_folder)

            root.setOnClickListener {
                MenuDialog(requireContext())
                    .add(R.string.disable_auto_backup, R.drawable.delete) { model.disableAutoBackup() }
                    .add(R.string.choose_another_folder, R.drawable.delete) { displayChooseFolderDialog() }
                    .show()
            }
        }
    }

    private fun PreferenceSeekbarBinding.setup(info: SeekbarInfo, initialValue: Int) {
        Title.setText(info.title)

        Slider.valueTo = info.max.toFloat()
        Slider.valueFrom = info.min.toFloat()

        Slider.value = initialValue.toFloat()

        Slider.addOnChangeListener { _, value, _ ->
            model.savePreference(info, value.toInt())
        }
    }


    private fun openLink(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.install_a_browser, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQUEST_IMPORT_BACKUP = 20
        private const val REQUEST_EXPORT_BACKUP = 21
        private const val REQUEST_CHOOSE_FOLDER = 22
    }
}