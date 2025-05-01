package com.techtravelcoder.dailynote.recyclerview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.techtravelcoder.dailynote.databinding.RecyclerAudioBinding
import com.techtravelcoder.dailynote.recyclerview.viewholder.AudioVH
import com.techtravelcoder.dailynote.room.Audio
import java.text.DateFormat

class AudioAdapter(private val onClick: (position: Int) -> Unit) : ListAdapter<Audio, AudioVH>(DiffCallback) {

    private val formatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)

    override fun onBindViewHolder(holder: AudioVH, position: Int) {
        val audio = getItem(position)
        holder.bind(audio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerAudioBinding.inflate(inflater, parent, false)
        return AudioVH(binding, onClick, formatter)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Audio>() {

        override fun areItemsTheSame(oldItem: Audio, newItem: Audio): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Audio, newItem: Audio): Boolean {
            return oldItem == newItem
        }
    }
}