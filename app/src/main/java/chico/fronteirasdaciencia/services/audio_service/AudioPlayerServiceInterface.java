package chico.fronteirasdaciencia.services.audio_service;

import android.net.Uri;

import chico.fronteirasdaciencia.fragments.AudioPlayFragmentInterface;

/**
 * Created by chico on 15/07/2015. Uhu!
 */
public interface AudioPlayerServiceInterface {
    boolean play(Uri data, AudioPlayFragmentInterface fragment_interface, long episode_id);
    void stop();
    void pause(boolean user);
    void restart(boolean user);
    void seek(int progress);
    void getProgress();
    void goBackground();
    void goForeground();
    void setVolume(float volume);
}
