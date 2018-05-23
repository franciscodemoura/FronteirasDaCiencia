package chico.fronteirasdaciencia.services.episode_service;

import android.net.Uri;

import chico.fronteirasdaciencia.aidl.EpisodeData;

/**
 * Created by chico on 15/06/2015. Uhu!
 */
interface EpisodeEventsInterface {
    void episodeDownloading(long episode_id);
    void episodeDownloaded(long episode_id, Uri local_file);
    void episodeAbsent(long episode_id);
    void newEpisode(EpisodeData episode);
    void episodeViewed(long episode_id, boolean viewed);
}