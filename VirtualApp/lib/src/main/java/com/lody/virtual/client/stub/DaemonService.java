package com.lody.virtual.client.stub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.lody.virtual.R;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;

import java.io.File;


/**
 * @author Lody
 *
 */
public class DaemonService extends Service {

    private static final int NOTIFY_ID = 1001;
    private static final String CHANNEL_ID = "channel_x";
    private static final String CHANNEL_NAME = "channel_name";

	static boolean showNotification = true;

	public static void startup(Context context) {
		File flagFile = context.getFileStreamPath(Constants.NO_NOTIFICATION_FLAG);
		if (Build.VERSION.SDK_INT >= 25 && flagFile.exists()) {
			showNotification = false;
		}
		Intent intent = new Intent(context, DaemonService.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(intent);
		} else {
			context.startService(intent);
		}
		if (VirtualCore.get().isServerProcess()) {
			// PrivilegeAppOptimizer.notifyBootFinish();
			DaemonJobService.scheduleJob(context);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		startup(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (!showNotification) {
			return;
		}
		Intent intent = new Intent(this, InnerService.class);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(intent);
		} else {
			startService(intent);

		}
		startDaemonForeground(this);
	}

	public static void startDaemonForeground(Service context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,                    NotificationManager.IMPORTANCE_MIN);
			notificationChannel.enableLights(false);//如果使用中的设备支持通知灯，则说明此通知通道是否应显示灯
			notificationChannel.setShowBadge(false);//是否显示角标
			notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
			NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
			manager.createNotificationChannel(notificationChannel);


			Notification notification =
					new Notification.Builder(context, CHANNEL_NAME)
							.setContentTitle("智能棋盘正在运行中,请不要轻易杀死智能棋盘")
							.setContentText("不使用的时候杀掉进程即可")
							.setSmallIcon(R.drawable.icon)
							.setChannelId(CHANNEL_ID)
							.build();
			context.startForeground(NOTIFY_ID, notification);
		} else {
			context.startForeground(NOTIFY_ID, new Notification());

		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	public static final class InnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
			startDaemonForeground(this);
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}
	}


}
