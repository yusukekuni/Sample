package jp.teluru.Teluru;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	 
	 /**
     * 登録トークンを受け取った時に実行される。{@inheritDoc}
     */
    @Override
    public void onRegistered(Context context, String registration) {
         Log.d("onRegistered", "registration id:" + registration);
        TeluruActivity.sendNotificationRegistrationId(registration);
    }
    /**
     * デバイスが登録解除した時に実行される。{@inheritDoc}
     */
    @Override
    protected void onUnregistered(Context arg0, String arg1) {
    }
    /**
     * クラウドメッセージが受信した時に実行される。{@inheritDoc}
     */
    @Override
    protected void onMessage(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Log.d("onMessage", "onMessage:" + extras.toString());
        Integer id = Integer.parseInt(extras.getString("id"));
        String message = extras.getString("message");
        String app_kind = extras.getString("app_kind");
        setNotification(context, id, message, app_kind);
    }
    /**
     * 登録エラーの時に実行される。
     */
    @Override
    public void onError(Context context, String errorId) {
        Log.d("onError", "onError:" + errorId);
    }
    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        Log.d("onError", "onError:" + errorId);
        return super.onRecoverableError(context, errorId);
    }
     
    private void setNotification(Context context, Integer id, String message, String app_kind) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);        

        String osVersion = Build.VERSION.RELEASE;
        String[] vers = osVersion.split("\\.");
        Integer major = Integer.parseInt(vers[0]);
        Integer minor = 0;
        if(vers.length >= 2){
        	minor = Integer.parseInt(vers[1]);
        }
        int icon = R.drawable.ic_stat_notify_30;
        if(major == 2 && minor == 3){
        	icon = R.drawable.ic_stat_notify_23;
        }else if((major == 2 && minor <= 2) || (major <= 1)){
        	icon = R.drawable.ic_stat_notify_22;
        }
        Notification notification = new Notification(
        		icon,
        		message,
                System.currentTimeMillis()
        );
        Intent intent = new Intent(context, TeluruActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setData(Uri.parse("teluru://push-" + app_kind));
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(
        		context,
                "テルル",
                message,
                pendingIntent
        );
        notificationManager.notify(id, notification);
    }
}