package chico.fronteirasdaciencia.services.audio_service;

/**
 * Created by chico on 16/07/2015. Uhu!
 */
interface AudioServiceFeedbackInterface {
    //void restartTimeout(MediaPlayerMonitorThread monitor);
    void paused(MediaPlayerMonitorThread monitor, boolean user);
    void playing(MediaPlayerMonitorThread monitor, boolean user);
    void progress(int progress, int max, MediaPlayerMonitorThread monitor);
}
