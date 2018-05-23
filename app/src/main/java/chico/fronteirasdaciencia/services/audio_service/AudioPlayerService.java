package chico.fronteirasdaciencia.services.audio_service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import chico.fronteirasdaciencia.R;
import chico.fronteirasdaciencia.activities.MainActivity;
import chico.fronteirasdaciencia.fragments.AudioPlayFragmentInterface;

/**
 * Created by chico on 15/07/2015. Uhu!
 */
public class AudioPlayerService extends Service implements AudioServiceFeedbackInterface, AudioManager.OnAudioFocusChangeListener {

    private static final int AUDIO_PLAY_NOTIFICATION_ID = 3;
    private static final String AUDIO_PLAY_PLAYING_STATE_STRING = "playing";
    private static final String MEDIA_COMMAND_ACTION = "chico.fronteirasdaciencia_play_pause_command";

    public static Intent makeIntent(final Context context) {
        return new Intent(context, AudioPlayerService.class);
    }

    private static Intent makeAudioPlayIntent(final boolean playing) {
        final Intent intent = new Intent(MEDIA_COMMAND_ACTION);
        intent.putExtra(AUDIO_PLAY_PLAYING_STATE_STRING, playing);
        return intent;
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mServiceBinder.restart(false);
                mServiceBinder.setVolume(1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                /*if (mFragmentInterface != null) {
                    mFragmentInterface.clean();
                    mFragmentInterface.error(1);
                }*/
                //mServiceBinder.stop();
                mServiceBinder.pause(false);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mServiceBinder.pause(false);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mServiceBinder.setVolume(0.1f);
                break;
        }
    }


    private class AudioServiceBinder extends Binder implements AudioPlayerServiceInterface {

        @Override
        public boolean play(final Uri data, final AudioPlayFragmentInterface fragment_interface, final long episode_id) {
            mEpisodeId = episode_id;
            mBackground = false;
            mFragmentInterface = fragment_interface;
            mReady = false;

            mPlayerMonitor = new MediaPlayerMonitorThread(
                    "Media_player_monitor",
                    mFragmentInterface,
                    AudioPlayerService.this,
                    getApplicationContext(),
                    data
            );

            registerReceiver(mMediaCommandReceiver, new IntentFilter(MEDIA_COMMAND_ACTION));
            mReceiverRegistered = true;

            if (!data.getScheme().equals("file")) {
                mWifiLock.acquire();
            }

            if (requestAudioFocus()) {
                mPlayerMonitor.start();
                return true;
            } else {
                mPlayerMonitor = null;
                return false;
            }
        }

        @Override
        public void stop() {
            AudioPlayerService.this.onUnbind(null);
        }

        @Override
        public void pause(final boolean user) {
            if(user){
                abandonAudioFocus();
            }
            if (mPlayerMonitor != null) {
                mPlayerMonitor.pause(user);
            }
        }

        @Override
        public void restart(final boolean user) {
            if(requestAudioFocus()) {
                if (mPlayerMonitor != null) {
                    mPlayerMonitor.restart(user);
                }
            }
            else{
                if(mFragmentInterface != null){
                    mPlayerMonitor.release();
                    mFragmentInterface.error(AudioPlayFragmentInterface.ErrorCode.AUDIO_ERROR);
                }
            }
        }

        @Override
        public void seek(final int progress) {
            if (mPlayerMonitor != null) {
                mPlayerMonitor.seek(progress);
            }
        }

        @Override
        public void getProgress() {
            if (mPlayerMonitor != null) {
                mPlayerMonitor.getProgress();
            }
        }

        @Override
        public void goBackground() {
            mBackground = true;
            updateAudioPlayerNotification(true);
        }

        @Override
        public void goForeground() {
            mBackground = false;
            updateAudioPlayerNotification(false);
        }

        @Override
        public void setVolume(final float volume) {
            if (mPlayerMonitor != null) {
                mPlayerMonitor.setVolume(volume);
            }
        }
    }


    private class MediaCommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mBackground) {
                if (intent.getBooleanExtra(AUDIO_PLAY_PLAYING_STATE_STRING, false)) {
                    mServiceBinder.pause(true);
                } else {
                    mServiceBinder.restart(true);
                }
            }
        }
    }


    private AudioServiceBinder mServiceBinder;
    private AudioPlayFragmentInterface mFragmentInterface;
    //private Runnable mTimeOutRunnable;
    private volatile Handler mHandler;
    private volatile MediaPlayerMonitorThread mPlayerMonitor;
    private WifiManager.WifiLock mWifiLock;
    private NotificationManager mNotificationManager;
    private boolean mBackground;
    private boolean mPlaying;
    private int mProgress;
    private int mMax;
    private long mEpisodeId;
    private MediaCommandReceiver mMediaCommandReceiver;
    private boolean mReceiverRegistered;
    private boolean mReady;
    private AudioManager mAudioManager;

    @Override
    public void onCreate() {
        mWifiLock =
                ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mServiceBinder = new AudioServiceBinder();
        /*mTimeOutRunnable = new Runnable() {
            @Override
            public void run() {
                if(mFragmentInterface != null) {
                    mFragmentInterface.error();
                }
                mReady = false;
                updateAudioPlayerNotification(false);
            }
        };*/
        mHandler = new Handler();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBackground = false;
        mMediaCommandReceiver = new MediaCommandReceiver();
        mReceiverRegistered = false;
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onDestroy() {
        close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBackground = false;
        return mServiceBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        close();
        return false;
    }

    private void close() {
        mPlaying = false;
        mBackground = false;
        updateAudioPlayerNotification(false);

        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }

        if (mPlayerMonitor != null) {
            mPlayerMonitor.release();

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitHandlerThread(mPlayerMonitor);
            } //else {
                //mPlayerMonitor.quit();
            //}
        }

        mPlayerMonitor = null;
        mFragmentInterface = null;

        //mHandler.removeCallbacks(mTimeOutRunnable);

        abandonAudioFocus();

        unregisterMediaCommandReceiver();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void quitHandlerThread(HandlerThread t) {
        t.quitSafely();
    }

    private void unregisterMediaCommandReceiver() {
        if (mReceiverRegistered) {
            unregisterReceiver(mMediaCommandReceiver);
            mReceiverRegistered = false;
        }
    }

    private boolean requestAudioFocus() {
        final int result = mAudioManager.requestAudioFocus(
                AudioPlayerService.this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

    }

    private void abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(this);
    }


    /*@Override
    public void restartTimeout(final MediaPlayerMonitorThread monitor) {
        if (monitor == mPlayerMonitor) {
            mHandler.removeCallbacks(mTimeOutRunnable);
            if (mFragmentInterface != null) {
                mHandler.postDelayed(mTimeOutRunnable, getResources().getInteger(R.integer.media_player_timeout));
            }
        }
    }*/

    @Override
    public void paused(final MediaPlayerMonitorThread monitor, final boolean user){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (monitor == mPlayerMonitor) {
                        mPlaying = false;
                        updateAudioPlayerNotification(false);
                    }
                }
            });
    }

    @Override
    public void playing(final MediaPlayerMonitorThread monitor, final boolean user) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(monitor == mPlayerMonitor) {
                    mPlaying = true;
                    updateAudioPlayerNotification(false);
                }
            }
        });
    }

    @Override
    public void progress(final int progress, final int max, final MediaPlayerMonitorThread monitor) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(monitor == mPlayerMonitor){
                    mProgress = progress;
                    mMax = max;
                    mReady = true;
                    updateAudioPlayerNotification(false);
                }
            }
        });
    }





    private void updateAudioPlayerNotification(final boolean start_foreground) {
        if (mBackground && mFragmentInterface != null) {
            sendAudioPlayerNotification(makeAudioPlayerNotification(), start_foreground);
        }
        else{
            cancelAudioPlayerNotification();
        }
    }

    private void sendAudioPlayerNotification(final Notification notification, final boolean start_foreground){
        if(start_foreground) {
            startForeground(AUDIO_PLAY_NOTIFICATION_ID, notification);
        }
        else{
            mNotificationManager.notify(AUDIO_PLAY_NOTIFICATION_ID, notification);
        }
    }

    private void cancelAudioPlayerNotification(){
        stopForeground(true);
    }

    private Notification makeAudioPlayerNotification(){
        if(mProgress > mMax){
            mProgress = mMax;
        }

        Notification.Style style =  null;
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            if (mReady) {
                style = new Notification.MediaStyle().setShowActionsInCompactView(0);
            } else {
                style = new Notification.MediaStyle();
            }
        }


        final Notification.Builder builder = new Notification.Builder(this)
                .setTicker(getString(R.string.player_notification_text) + mEpisodeId)
                .setOngoing(true)
                .setContentInfo(getString(R.string.total_time_string) + ": " + getPlayTimeString(mMax))
                .setContentTitle(getString(R.string.player_notification_text) + mEpisodeId)
                .setContentText(getPlayTimeString(mProgress) + "  (" + 100*mProgress/(mMax == 0 ? 1000000000 : mMax) + "%)")
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.logo))
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(this, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                );
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(style);
        }

        if(mReady){
            builder.addAction(mPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, "", PendingIntent.getBroadcast(
                    this,
                    0,
                    makeAudioPlayIntent(mPlaying),
                    PendingIntent.FLAG_UPDATE_CURRENT
            ));
        }

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder.setProgress(mMax, mProgress, false);
        }

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            setNotificationPriorityHigh(builder);
        }

        return builder.build();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setNotificationPriorityHigh(final Notification.Builder notification_builder){
        notification_builder.setPriority(Notification.PRIORITY_HIGH);
    }




    public static String getPlayTimeString(final int play_time){
        final int minutes = play_time / (60*1000);
        final int seconds = play_time % (60*1000) / 1000;
        return (minutes < 10 ? "0" : "") + String.valueOf(minutes) + ":" + (seconds < 10 ? "0" : "") + String.valueOf(seconds);
    }
}
