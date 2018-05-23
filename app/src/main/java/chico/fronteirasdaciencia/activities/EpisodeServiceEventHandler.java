package chico.fronteirasdaciencia.activities;

import android.net.Uri;
import android.os.Handler;

import chico.fronteirasdaciencia.aidl.EpisodeData;
import chico.fronteirasdaciencia.aidl.ServiceEvents;

/**
 * Created by chico on 09/06/2015. Uhu!
 */

class EpisodeServiceEventHandler extends ServiceEvents.Stub{

    private final EpisodeListAdapter mEpisodeList;
    private final Handler mUIThreadHandler;

    EpisodeServiceEventHandler(final EpisodeListAdapter episode_list, final Handler handler){
        mEpisodeList = episode_list;
        mUIThreadHandler = handler;
    }

    @Override
    public void episodeDownloading(final long episode_id) {
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mEpisodeList.episodeDownloading(episode_id);
            }
        });
    }

    @Override
    public void episodeDownloaded(final long episode_id, final Uri local_file) {
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mEpisodeList.episodeDownloaded(episode_id, local_file);
            }
        });
    }

    @Override
    public void episodeAbsent(final long episode_id) {
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mEpisodeList.episodeAbsent(episode_id);
            }
        });
    }

    @Override
    public void newEpisode(final EpisodeData episode) {
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mEpisodeList.newEpisode(episode);
            }
        });
    }

    @Override
    public void episodeViewed(final long episode_id, final boolean viewed) {
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mEpisodeList.episodeViewed(episode_id,viewed);
            }
        });
    }
}
