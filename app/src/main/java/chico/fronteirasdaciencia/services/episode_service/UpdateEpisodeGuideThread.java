package chico.fronteirasdaciencia.services.episode_service;

import android.net.Uri;

/**
 * Created by chico on 14/06/2015. Uhu!
 */
class UpdateEpisodeGuideThread extends Thread{

    public enum UpdateMode {CHECK_NEW,CHECK_SAVED}

    private final EpisodeDataManager mEpisodeDataManager;
    private final UpdateMode mMode;

    public UpdateEpisodeGuideThread(
            final EpisodeDataManager episode_data_manager,
            final UpdateMode mode
    ){
        mEpisodeDataManager = episode_data_manager;
        mMode = mode;
    }

    @Override
    public void run(){
        if(mMode == UpdateMode.CHECK_SAVED) {
            checkDownloadedEpisodes();
        }
        else {
            final int new_episodes = mEpisodeDataManager.downloadAndUpdateEpisodeGuide();
            checkNewEpisodes(new_episodes);
        }
    }

    private void checkNewEpisodes(final int new_episodes){

        for (int i = 0; i < new_episodes; i++) {
            final long episode_id = mEpisodeDataManager.getEpisode(mEpisodeDataManager.size() - new_episodes + i).getId();
            mEpisodeDataManager.episodeAbsent(episode_id);
        }
    }

    private void checkDownloadedEpisodes(){
        for (int i = 0; i < mEpisodeDataManager.size(); i++) {

            final long episode_id = mEpisodeDataManager.getEpisode(i).getId();
            final Uri local_file = mEpisodeDataManager.checkEpisodeFile(episode_id);

            if (local_file != null) {
                mEpisodeDataManager.episodeDownloaded(episode_id, local_file);
            } else {
                mEpisodeDataManager.episodeAbsent(episode_id);
            }
        }
    }
}
