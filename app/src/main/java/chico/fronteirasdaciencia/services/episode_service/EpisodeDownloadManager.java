package chico.fronteirasdaciencia.services.episode_service;

import android.net.Uri;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chico on 14/06/2015. Uhu!
 */
class EpisodeDownloadManager {

    private final Queue<DownloadThread> mThreadQueue = new ArrayDeque<>(1024);
    private DownloadThread mCurrentThread;
    private final EpisodeService mService;
    private final EpisodeDataManager mEpisodeDataManager;
    private final AtomicInteger mDownloadsAlive = new AtomicInteger(0);
    private final AtomicInteger mDownloadsFailed = new AtomicInteger(0);

    public EpisodeDownloadManager(
            final EpisodeService service,
            final EpisodeDataManager episode_data_manager
    ){
        mService = service;
        mEpisodeDataManager = episode_data_manager;
    }

    public synchronized void scheduleDownload(final long episode_id, final int start_id){
        final DownloadThread download_thread = new DownloadThread(episode_id, start_id);

        for(final DownloadThread t : mThreadQueue) {
            if(t.getEpisodeId() == episode_id  &&  !t.isInterrupted()){
                download_thread.interrupt();
                break;
            }
        }

        if(mThreadQueue.isEmpty()){
            mService.startForeground();
            mDownloadsFailed.set(0);
        }

        mThreadQueue.offer(download_thread);

        if(!download_thread.isInterrupted()) {
            mEpisodeDataManager.episodeDownloading(episode_id);
            updateDownloadCount();

            mService.sendDownloadNotification(
                    mService.makeDownloadNotification(
                            episode_id,
                            mDownloadsAlive.get(),
                            mDownloadsFailed.get(),
                            10,
                            0
                    )
            );
        }

        if(mCurrentThread == null){
            scheduleNextThread();
        }
    }

    public synchronized void AbortDownload(final long episode_id){
        for(final DownloadThread download_thread : mThreadQueue){
            if(download_thread.getEpisodeId() == episode_id){

                if(!download_thread.isInterrupted()) {
                    download_thread.interrupt();
                    mEpisodeDataManager.episodeAbsent(episode_id);
                }
            }
        }
        updateDownloadCount();
    }

    private synchronized void finishCurrentThread(final int start_id){
        if(mCurrentThread.isInterrupted()){
            mEpisodeDataManager.deleteEpisodeFile(mCurrentThread.getEpisodeId());
        }
        else{
            final Uri local_file = mEpisodeDataManager.checkEpisodeFile(mCurrentThread.getEpisodeId());
            if(local_file == null){
                mEpisodeDataManager.deleteEpisodeFile(mCurrentThread.getEpisodeId());
                mEpisodeDataManager.episodeAbsent(mCurrentThread.getEpisodeId());
            }
            else{
                mEpisodeDataManager.episodeDownloaded(mCurrentThread.getEpisodeId(),local_file);
            }
        }

        mCurrentThread = null;
        mThreadQueue.poll();

        updateDownloadCount();

        if(mThreadQueue.isEmpty()){
            mService.stopForeground2(mDownloadsFailed.get());
            mDownloadsFailed.set(0);
        }

        mService.stopSelfAux(start_id);

        scheduleNextThread();
    }

    public synchronized void abortDownloads(final int start_id){
        if(mThreadQueue.isEmpty()){
            mService.stopSelfAux(start_id);
        }
        else {
            final DownloadThread fake_download_thread = new DownloadThread(0, start_id);
            mThreadQueue.offer(fake_download_thread);

            for(final DownloadThread download_thread : mThreadQueue){
                if(!download_thread.isInterrupted()) {
                    download_thread.interrupt();
                    final long episode_id = download_thread.getEpisodeId();
                    if(episode_id > 0) {
                        mEpisodeDataManager.episodeAbsent(episode_id);
                    }
                }
            }
            mService.sendDownloadNotification(
                    mService.makeDownloadNotification(
                            0,
                            mDownloadsAlive.get(),
                            mDownloadsFailed.get(),
                            10,
                            0
                    )
            );
        }
    }

    private void scheduleNextThread(){
        if(!mThreadQueue.isEmpty()){
            mCurrentThread = mThreadQueue.peek();

            if(!mCurrentThread.isInterrupted()) {
                mService.sendDownloadNotification(
                        mService.makeDownloadNotification(
                                mCurrentThread.getEpisodeId(),
                                mDownloadsAlive.get(),
                                mDownloadsFailed.get(),
                                10,
                                0
                        )
                );
            }

            mCurrentThread.start();
        }
    }

    private void updateDownloadCount(){
        int i = 0;
        for(final DownloadThread t :mThreadQueue){
            if(!t.isInterrupted()){
                i++;
            }
        }
        mDownloadsAlive.set(i);
    }

    private class DownloadThread implements Runnable{

        private final long mEpisodeId;
        private final int mStartId;
        private final AtomicBoolean interrupted = new AtomicBoolean(false);
        private volatile Thread mParent;

        public DownloadThread(final long episode_id, final int start_id){
            mEpisodeId = episode_id;
            mStartId = start_id;
        }

        public long getEpisodeId(){
            return mEpisodeId;
        }

        public void start(){
            if(mParent == null) {
                mParent = new Thread(this);
                mParent.start();
            }
        }

        public void interrupt(){
            interrupted.set(true);
            if(mParent != null) {
                mParent.interrupt();
            }
        }

        public boolean isInterrupted(){
            return interrupted.get() || mParent != null && mParent.isInterrupted();
        }

        @Override
        public void run(){
            if(!isInterrupted()){
                final boolean success =
                    mEpisodeDataManager.downloadEpisode(
                            mEpisodeId,
                            new EpisodeDataManager.StreamProgressListener() {
                                @Override
                                public boolean setProgress(int max, int progress) {
                                    mService.sendDownloadNotification(
                                            mService.makeDownloadNotification(
                                                    mEpisodeId,
                                                    mDownloadsAlive.get(),
                                                    mDownloadsFailed.get(),
                                                    max,
                                                    progress
                                            )
                                    );
                                    return isInterrupted();
                                }
                            }
                    );
                if(!success){
                    if(!isInterrupted()) {
                        interrupt();
                        mDownloadsFailed.incrementAndGet();
                        mEpisodeDataManager.episodeAbsent(mEpisodeId);
                    }
                }
            }
            finishCurrentThread(mStartId);
        }
    }
}
