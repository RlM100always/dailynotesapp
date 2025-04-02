package com.techtravelcoder.dailynote.fragments

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.techtravelcoder.dailynote.MenuDialog
import com.techtravelcoder.dailynote.R
import com.techtravelcoder.dailynote.databinding.DialogInputBinding
import com.techtravelcoder.dailynote.databinding.FragmentNotesBinding
import com.techtravelcoder.dailynote.miscellaneous.Constants
import com.techtravelcoder.dailynote.miscellaneous.add
import com.techtravelcoder.dailynote.recyclerview.ItemListener
import com.techtravelcoder.dailynote.recyclerview.adapter.LabelAdapter
import com.techtravelcoder.dailynote.room.Label
import com.techtravelcoder.dailynote.viewmodels.BaseNoteModel

class Labels : Fragment(), ItemListener {

    private var adapter: LabelAdapter? = null
    private var binding: FragmentNotesBinding? = null

    private val model: BaseNoteModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = LabelAdapter(this)

        binding?.RecyclerView?.setHasFixedSize(true)
        binding?.RecyclerView?.adapter = adapter
        binding?.RecyclerView?.layoutManager = GridLayoutManager(requireContext(),3 )
        // Custom ItemDecoration
        binding?.RecyclerView?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            private val paint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.Decorator)
                strokeWidth = 2f
            }

            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val layoutManager = parent.layoutManager as GridLayoutManager
                val spanCount = layoutManager.spanCount

                for (i in 0 until parent.childCount) {
                    val child = parent.getChildAt(i)
                    val position = parent.getChildAdapterPosition(child)

                    // Draw horizontal divider (full width)
                    c.drawLine(
                        child.left.toFloat(),
                        child.bottom.toFloat(),
                        child.right.toFloat(),
                        child.bottom.toFloat(),
                        paint
                    )

                    // Draw vertical divider (limited height)
                    if ((position + 1) % spanCount != 0) {
                        c.drawLine(
                            child.right.toFloat(),
                            child.top.toFloat(),
                            child.right.toFloat(),
                            child.bottom.toFloat(),
                            paint
                        )
                    }
                }
            }
        })

        binding?.RecyclerView?.setPadding(0, 0, 0, 0)
        binding?.ImageView?.setImageResource(R.drawable.label)

        setupObserver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        binding = FragmentNotesBinding.inflate(inflater)
        return binding?.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.add_label, R.drawable.add) { displayAddLabelDialog() }
    }


    override fun onClick(position: Int) {
        adapter?.currentList?.get(position)?.let { value ->
            val bundle = Bundle()
            bundle.putString(Constants.SelectedLabel, value)
            findNavController().navigate(R.id.LabelsToDisplayLabel, bundle)
        }
    }

    override fun onLongClick(position: Int) {
        adapter?.currentList?.get(position)?.let { value ->

            showCustomMenuDialog(value)
        }
    }

    private fun showCustomMenuDialog(value: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_menu_item, null)

        val editOption = dialogView.findViewById<LinearLayout>(R.id.editOption)
        val deleteOption = dialogView.findViewById<LinearLayout>(R.id.deleteOption)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        editOption.setOnClickListener {
            displayEditLabelDialog(value)
            dialog.dismiss()
        }

        deleteOption.setOnClickListener {
            confirmDeletion(value)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupObserver() {
        model.labels.observe(viewLifecycleOwner) { labels ->
            adapter?.submitList(labels)
            binding?.ImageView?.isVisible = labels.isEmpty()
        }
    }


    private fun displayAddLabelDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogBinding = DialogInputBinding.inflate(inflater)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_label)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val value = dialogBinding.EditText.text.toString().trim()
                if (value.isNotEmpty()) {
                    val label = Label(value)
                    model.insertLabel(label) { success: Boolean ->
                        if (success) {
                            dialog.dismiss()
                        } else Toast.makeText(context, R.string.label_exists, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()

        dialogBinding.EditText.requestFocus()
    }

    private fun confirmDeletion(value: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_label)
            .setMessage(R.string.your_notes_associated)
            .setPositiveButton(R.string.delete) { _, _ -> model.deleteLabel(value) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun displayEditLabelDialog(oldValue: String) {
        val dialogBinding = DialogInputBinding.inflate(layoutInflater)

        dialogBinding.EditText.setText(oldValue)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle(R.string.edit_label)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val value = dialogBinding.EditText.text.toString().trim()
                if (value.isNotEmpty()) {
                    model.updateLabel(oldValue, value) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else Toast.makeText(requireContext(), R.string.label_exists, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()

        dialogBinding.EditText.requestFocus()
    }
}