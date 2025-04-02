package com.techtravelcoder.dailynote.fragments

import android.view.Menu
import android.view.MenuInflater
import androidx.navigation.fragment.findNavController
import com.techtravelcoder.dailynote.R
import com.techtravelcoder.dailynote.miscellaneous.add

class Notes : NotallyFragment() {

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.search, R.drawable.search) { findNavController().navigate(R.id.NotesToSearch) }
    }


    override fun getObservable() = model.baseNotes

    override fun getBackground() = R.drawable.notebook
}