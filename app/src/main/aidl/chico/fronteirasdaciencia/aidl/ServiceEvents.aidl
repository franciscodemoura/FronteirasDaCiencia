package chico.fronteirasdaciencia.aidl;

import chico.fronteirasdaciencia.aidl.EpisodeData;

interface ServiceEvents {
    oneway void episodeDownloading(in long episode_id);
    oneway void episodeDownloaded(in long episode_id, in Uri local_file);
    oneway void episodeAbsent(in long episode_id);
    oneway void newEpisode(in EpisodeData episode);
    oneway void episodeViewed(in long episode_id, in boolean viewed);
}
