package com.techtravelcoder.dailynote.fragments

import com.techtravelcoder.dailynote.R

class Archived : NotallyFragment() {

    override fun getBackground() = R.drawable.archive

    override fun getObservable() = model.archivedNotes
}