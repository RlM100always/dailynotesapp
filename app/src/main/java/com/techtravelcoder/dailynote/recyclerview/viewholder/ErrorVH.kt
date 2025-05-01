package com.techtravelcoder.dailynote.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.techtravelcoder.dailynote.databinding.ErrorBinding
import com.techtravelcoder.dailynote.image.ImageError

class ErrorVH(private val binding: ErrorBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(error: ImageError) {
        binding.Name.text = error.name
        binding.Description.text = error.description
    }
}