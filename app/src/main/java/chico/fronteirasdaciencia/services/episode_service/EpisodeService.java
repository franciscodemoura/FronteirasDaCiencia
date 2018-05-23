package chico.fronteirasdaciencia.services.episode_service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import chico.fronteirasdaciencia.R;
import chico.fronteirasdaciencia.activities.MainActivity;
import chico.fronteirasdaciencia.aidl.EpisodeData;
import chico.fronteirasdaciencia.aidl.EpisodeServiceInterface;
import chico.fronteirasdaciencia.aidl.ServiceEvents;

/**
 * Created by chico on 07/06/2015. Uhu!
 */

public class EpisodeService extends Service implements EpisodeEventsInterface {

    private final static String EPISODE_ID_TAG = "episode_id";

    private static final int DOWNLOAD_NOTIFICATION_ID = 1;
    private static final int DOWNLOADS_FAILED_NOTIFICATION_ID = 2;

    public static boolean LOCAL_SERVICE = false;
    {
        LOCAL_SERVICE = true;
    }

    private ServiceImpl mServiceImpl;
    private volatile AtomicReference<ServiceEvents> mServiceEvents;
    private EpisodeDataManager mEpisodeDataManager;
    private EpisodeDownloadManager mEpisodeDownloadManager;
    private NotificationManager mNotificationManager;
    private volatile Handler mHandler;
    private boolean mDownloadCanceled = false;

    public static Intent makeIntent(final Context context){
        return new Intent(context,EpisodeService.class);
    }

    private static Intent makeIntentWithEpisodeId(final Context context, final long episode_id){
        final Intent intent = new Intent(context,EpisodeService.class);
        intent.putExtra(EPISODE_ID_TAG, episode_id);
        return intent;
    }

    public static void startEpisodeDownload(final Context context, final long episode_id){
        context.startService(makeIntentWithEpisodeId(context, episode_id));
    }

    public Notification makeDownloadNotification(
            long current_episode_id,
            int total_episodes,
            int failed_episodes,
            int progress_max,
            int progress_current
    ){
        final String main_text =
                getString(R.string.notification_downloading_text) + " " +
                        total_episodes + " " +
                        getResources().getQuantityString(R.plurals.episode_plural_string, total_episodes) +
                        (
                                failed_episodes >= 1 ?
                                        ", " + failed_episodes + " " + getResources().getQuantityString(R.plurals.downloads_failed_notification_text, failed_episodes) :
                                        " "
                        );

        final Notification.Builder builder = new Notification.Builder(this)
                .setTicker(main_text)
                .setOngoing(true)
                .setContentInfo((int) ((float) progress_current / (float) (progress_max == 0 ? 1000000000 : progress_max) * 100.0f) + "%")
                .setContentTitle(main_text)
                .setContentText(
                        current_episode_id == 0 ? null : (
                                getString(R.string.notification_downloading_text) + " " +
                                        getResources().getQuantityString(R.plurals.episode_plural_string, 1) + " " +
                                        current_episode_id
                        )
                )
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.logo))
                .setProgress(progress_max, progress_current, false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .addAction(
                        mDownloadCanceled ? android.R.drawable.ic_dialog_alert : R.drawable.abc_ic_clear_mtrl_alpha,
                        mDownloadCanceled ? getString(R.string.canceling_string) : getString(R.string.notification_cancel_download_text),
                        PendingIntent.getService(
                                this,
                                0,
                                makeIntentWithEpisodeId(this, 0),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(this, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                );

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder.setProgress(progress_max, progress_current, false);
        }

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            setNotificationPriorityDefault(builder);
        }

        return builder.build();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setNotificationPriorityDefault(final Notification.Builder notification_builder){
        notification_builder.setPriority(Notification.PRIORITY_DEFAULT);
    }

    private Notification makeDownloadsFailedNotification(
            int failed_episodes
    ){
        final String main_text =
                failed_episodes + " " +
                        getResources().getQuantityString(R.plurals.downloads_plural_string, failed_episodes) + " " +
                        getResources().getQuantityString(R.plurals.downloads_failed_notification_text, failed_episodes);

        return new Notification.Builder(this)
                .setTicker(main_text)
                .setContentTitle(main_text)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.logo))
                .setSmallIcon(R.drawable.abc_ic_clear_mtrl_alpha)
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(this, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )
                .setAutoCancel(true)
                .build();
    }

    public void sendDownloadNotification(final Notification notification){
        mNotificationManager.notify(DOWNLOAD_NOTIFICATION_ID,notification);
    }

    private void sendDownloadsFailedNotification(final Notification notification){
        mNotificationManager.notify(DOWNLOADS_FAILED_NOTIFICATION_ID,notification);
    }

    private void cancelDownloadNotification(){
        mNotificationManager.cancel(DOWNLOAD_NOTIFICATION_ID);
    }

    @Override
    public void onCreate(){
        mServiceImpl = new ServiceImpl();
        mEpisodeDataManager = new EpisodeDataManager(this,this);
        mServiceEvents = new AtomicReference<>();
        mEpisodeDownloadManager = new EpisodeDownloadManager(this,mEpisodeDataManager);
        new UpdateEpisodeGuideThread(mEpisodeDataManager, UpdateEpisodeGuideThread.UpdateMode.CHECK_SAVED).start();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mHandler = new Handler();
    }

    @Override
    public void onDestroy(){
        cancelDownloadNotification();
    }

    public void stopSelfAux(final int start_id){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                stopSelf(start_id);
            }
        });
    }

    public void stopForeground2(final int downloads_failed){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                stopForeground(true);
                if(downloads_failed >= 1) {
                    sendDownloadsFailedNotification(
                            makeDownloadsFailedNotification(downloads_failed)
                    );
                }
            }
        });
    }

    public void startForeground() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final Notification notification = makeDownloadNotification(
                        0,
                        1,
                        0,
                        10,
                        0
                );
                startForeground(DOWNLOAD_NOTIFICATION_ID, notification);
            }
        });
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int start_id){
        final long episode_id = intent.getLongExtra(EPISODE_ID_TAG, 0);
        if(episode_id == 0){
            mDownloadCanceled = true;
            mEpisodeDownloadManager.abortDownloads(start_id);
        }
        else {
            mDownloadCanceled = false;
            mEpisodeDownloadManager.scheduleDownload(episode_id, start_id);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mServiceImpl;
    }

    @Override
    public boolean onUnbind(final Intent intent){
        mServiceEvents.set(null);
        return false;
    }

    @Override
    public void episodeDownloading(long episode_id) {
        final ServiceEvents service_events = mServiceEvents.get();
        if (service_events != null) {
            try {
                service_events.episodeDownloading(episode_id);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void episodeDownloaded(long episode_id, Uri local_file) {
        final ServiceEvents temp = mServiceEvents.get();
        if(temp != null){
            try {
                temp.episodeDownloaded(episode_id, local_file);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void episodeAbsent(long episode_id) {
        final ServiceEvents temp = mServiceEvents.get();
        if(temp != null){
            try {
                temp.episodeAbsent(episode_id);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void newEpisode(EpisodeData episode) {
        final ServiceEvents temp = mServiceEvents.get();
        if(temp != null){
            try {
                temp.newEpisode(episode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void episodeViewed(long episode_id, boolean viewed) {
        final ServiceEvents temp = mServiceEvents.get();
        if(temp != null){
            try {
                temp.episodeViewed(episode_id,viewed);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServiceImpl extends EpisodeServiceInterface.Stub {
        @Override
        public List<EpisodeData> doServiceHandshake(final ServiceEvents service_events_callback, final boolean search_for_new_episodes) {
            mServiceEvents.set(service_events_callback);
            if(search_for_new_episodes) {
                new UpdateEpisodeGuideThread(mEpisodeDataManager, UpdateEpisodeGuideThread.UpdateMode.CHECK_NEW).start();
            }
            return mEpisodeDataManager.getEpisodeList();
        }

        @Override
        public List<String> getSeasonList() {
            ArrayList<String> seasons = new ArrayList<>();
            seasons.add("Primeira");
            seasons.add("Segunda");
            seasons.add("Terceira");
            seasons.add("Quarta");
            seasons.add("Quinta");
            seasons.add("Sexta");
            seasons.add("Sétima");
            seasons.add("Oitava");
            seasons.add("Nona");

            return seasons;
        }

        @Override
        public List<String> getCategoryList() {
            return new ArrayList<>();
        }

        @Override
        public void releaseService(){
            mServiceEvents.set(null);
        }

        @Override
        public void cancelDownload(final long episode_id) {
            mEpisodeDownloadManager.AbortDownload(episode_id);
        }

        @Override
        public void deleteFile(final long episode_id) {
            mEpisodeDataManager.deleteEpisodeFile(episode_id);
            mEpisodeDataManager.episodeAbsent(episode_id);
        }

        @Override
        public void episodeViewed(final long episode_id, final boolean viewed){
            mEpisodeDataManager.episodeViewed(episode_id, viewed);
        }

        @Override
        public String[] getPodcastData() {
            return new String [] {
                    mEpisodeDataManager.getPodcastTitle(),
                    mEpisodeDataManager.getPodcastLink(),
                    mEpisodeDataManager.getPodcastDescription(),
            };
        }
    }
}
