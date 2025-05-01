package com.techtravelcoder.dailynote.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.techtravelcoder.dailynote.miscellaneous.Constants

class WidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val id = intent.getLongExtra(Constants.SelectedBaseNote, 0)
        return WidgetFactory(application, id)
    }
}