package chico.fronteirasdaciencia.aidl;

import java.util.List;
import chico.fronteirasdaciencia.aidl.EpisodeData;
import chico.fronteirasdaciencia.aidl.ServiceEvents;

interface EpisodeServiceInterface {
    List<EpisodeData> doServiceHandshake(in ServiceEvents service_events_callback, in boolean search_for_new_episodes);
    List<String> getSeasonList();
    List<String> getCategoryList();
    void releaseService();
    oneway void cancelDownload(in long episode_id);
    oneway void deleteFile(in long episode_id);
    oneway void episodeViewed(in long episode_id, in boolean viewed);
    String [] getPodcastData();
}
