package com.techtravelcoder.dailynote.activities

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.techtravelcoder.dailynote.R
import com.techtravelcoder.dailynote.databinding.ActivityLabelBinding
import com.techtravelcoder.dailynote.databinding.DialogInputBinding
import com.techtravelcoder.dailynote.miscellaneous.add
import com.techtravelcoder.dailynote.recyclerview.adapter.SelectableLabelAdapter
import com.techtravelcoder.dailynote.room.Label
import com.techtravelcoder.dailynote.viewmodels.LabelModel

class SelectLabels : AppCompatActivity() {

    private val model: LabelModel by viewModels()
    private lateinit var binding: ActivityLabelBinding

    private lateinit var selectedLabels: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val savedList = savedInstanceState?.getStringArrayList(SELECTED_LABELS)
        val passedList = requireNotNull(intent.getStringArrayListExtra(SELECTED_LABELS))
        selectedLabels = savedList ?: passedList

        val result = Intent()
        result.putExtra(SELECTED_LABELS, selectedLabels)
        setResult(RESULT_OK, result)

        setupToolbar()
        setupRecyclerView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(SELECTED_LABELS, selectedLabels)
    }


    private fun setupToolbar() {
        binding.Toolbar.setNavigationOnClickListener { finish() }
        binding.Toolbar.menu.add(R.string.add_label, R.drawable.add) { addLabel() }
    }

    private fun addLabel() {
        val binding = DialogInputBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_label)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val value = binding.EditText.text.toString().trim()
                if (value.isNotEmpty()) {
                    val label = Label(value)
                    model.insertLabel(label) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else Toast.makeText(this, R.string.label_exists, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()

        binding.EditText.requestFocus()
    }

    private fun setupRecyclerView() {
        val adapter = SelectableLabelAdapter(selectedLabels)
        adapter.onChecked = { position, checked ->
            if (position != -1) {
                val label = adapter.currentList[position]
                if (checked) {
                    if (!selectedLabels.contains(label)) {
                        selectedLabels.add(label)
                    }
                } else selectedLabels.remove(label)
            }
        }

        binding.RecyclerView.setHasFixedSize(true)
        binding.RecyclerView.layoutManager = GridLayoutManager(this, 2) // Added GridLayoutManager with span count 2
        binding.RecyclerView.adapter = adapter

        binding?.RecyclerView?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            private val paint = Paint().apply {
                color = ContextCompat.getColor(this@SelectLabels, R.color.Decorator)
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


        model.labels.observe(this) { labels ->
            adapter.submitList(labels)
            if (labels.isEmpty()) {
                binding.EmptyState.visibility = View.VISIBLE
            } else binding.EmptyState.visibility = View.INVISIBLE
        }
    }

    companion object {
        const val SELECTED_LABELS = "SELECTED_LABELS"
    }
}