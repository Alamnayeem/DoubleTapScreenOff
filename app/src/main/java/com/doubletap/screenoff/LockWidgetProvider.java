package com.doubletap.screenoff;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class LockWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent("com.doubletap.screenoff.ACTION_LOCK");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
            );

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lock);
            views.setOnClickPendingIntent(R.id.btnWidgetLock, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
