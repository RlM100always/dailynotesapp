package com.techtravelcoder.dailynote.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.techtravelcoder.dailynote.databinding.RecyclerSelectableLabelBinding

class SelectableLabelVH(
    private val binding: RecyclerSelectableLabelBinding,
    private val onChecked: (position: Int, checked: Boolean) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.cb.setOnCheckedChangeListener { _, isChecked ->
            onChecked(adapterPosition, isChecked)
        }
    }

    fun bind(value: String, checked: Boolean) {
        binding.cb.text = value
        binding.cb.isChecked = checked
    }
}