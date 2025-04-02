package com.techtravelcoder.dailynote.fragments

import androidx.lifecycle.LiveData
import com.techtravelcoder.dailynote.R
import com.techtravelcoder.dailynote.miscellaneous.Constants
import com.techtravelcoder.dailynote.room.Item

class DisplayLabel : NotallyFragment() {

    override fun getBackground() = R.drawable.label

    override fun getObservable(): LiveData<List<Item>> {
        val label = requireNotNull(requireArguments().getString(Constants.SelectedLabel))
        return model.getNotesByLabel(label)
    }
}