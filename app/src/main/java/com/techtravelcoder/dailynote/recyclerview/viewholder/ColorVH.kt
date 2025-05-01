package com.techtravelcoder.dailynote.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.techtravelcoder.dailynote.databinding.RecyclerColorBinding
import com.techtravelcoder.dailynote.miscellaneous.Operations
import com.techtravelcoder.dailynote.recyclerview.ItemListener
import com.techtravelcoder.dailynote.room.Color

class ColorVH(private val binding: RecyclerColorBinding, listener: ItemListener) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.CardView.setOnClickListener {
            listener.onClick(adapterPosition)
        }
    }

    fun bind(color: Color) {
        val value = Operations.extractColor(color, binding.root.context)
        binding.CardView.setCardBackgroundColor(value)
    }
}