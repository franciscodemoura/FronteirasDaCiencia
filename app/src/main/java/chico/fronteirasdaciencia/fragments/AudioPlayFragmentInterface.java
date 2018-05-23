package chico.fronteirasdaciencia.fragments;

/**
 * Created by chico on 15/07/2015. Uhu!
 */
public interface AudioPlayFragmentInterface {
    enum ErrorCode{NETWORK_ERROR, AUDIO_ERROR}
    void startPlay(int max, int progress);
    void paused(boolean user);
    void playing(boolean user);
    void error(ErrorCode error_code);
    void setProgress(int progress);
    void clean();
    void terminate();
}
