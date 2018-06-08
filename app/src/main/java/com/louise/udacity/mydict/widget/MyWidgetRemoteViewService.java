package com.louise.udacity.mydict.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class MyWidgetRemoteViewService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MyWidgetRemoteViewFactory(this.getApplicationContext(), intent);

    }
}
