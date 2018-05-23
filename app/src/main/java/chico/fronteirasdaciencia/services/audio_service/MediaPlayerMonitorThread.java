package chico.fronteirasdaciencia.services.audio_service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import chico.fronteirasdaciencia.fragments.AudioPlayFragmentInterface;

/**
 * Created by chico on 16/07/2015. Uhu!
 */
public class MediaPlayerMonitorThread extends HandlerThread implements AudioPlayerServiceInterface, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private final AtomicReference<Handler> mHandler;
    private final AtomicReference<Handler> mHandlerRelease;
    private MediaPlayer mPlayer;
    private final AtomicReference<AudioPlayFragmentInterface> mAudioPlayFragment;
    private final AtomicReference<AudioServiceFeedbackInterface> mAudioService;
    private final Runnable mUpdateServerRunnable;
    private final Context mAppContext;
    private final Uri mData;

    public MediaPlayerMonitorThread(
            final String name,
            final AudioPlayFragmentInterface audio_play_fragment,
            final AudioServiceFeedbackInterface service_feedback_interface,
            final Context app_context,
            final Uri data
    ) {
        super(name);

        mAudioPlayFragment = new AtomicReference<>(audio_play_fragment);
        mAudioService = new AtomicReference<>(service_feedback_interface);
        mAppContext = app_context;
        mData = data;
        mHandler = new AtomicReference<>();
        mHandlerRelease = new AtomicReference<>();

        mUpdateServerRunnable = new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null) {
                    notifyProgress();
                    final Handler tempH = mHandler.get();
                    if(tempH != null) {
                        tempH.postDelayed(mUpdateServerRunnable, 1000);
                    }
                }
            }
        };
    }




    @Override
    protected void onLooperPrepared() {
        mHandlerRelease.set(new Handler());

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setLooping(false);
        mPlayer.setWakeMode(mAppContext, PowerManager.PARTIAL_WAKE_LOCK);

        try {

            mPlayer.setDataSource(mAppContext, mData);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //final AudioServiceFeedbackInterface tempS = mAudioService.get();
        //if(tempS != null) {
            //tempS.restartTimeout(this);
        //}
    }




    @Override
    public boolean play(Uri data, AudioPlayFragmentInterface fragment_interface, long ep_id) {
        return false;
    }

    @Override
    public void pause(final boolean user) {
        final Handler tempH = mHandler.get();
        if(tempH != null) {
            tempH.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer != null) {
                        if (mPlayer.isPlaying()) {
                            mPlayer.pause();
                        }
                        updateFragmentInterfaceMediaButton(user);

                        //final AudioServiceFeedbackInterface tempS = mAudioService.get();
                        //if (tempS != null) {
                            //tempS.restartTimeout(MediaPlayerMonitorThread.this);
                        //}
                    }
                }
            });
        }
    }

    @Override
    public void restart(final boolean user) {
        final Handler tempH = mHandler.get();
        if(tempH != null) {
            tempH.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer != null) {
                        if (!mPlayer.isPlaying() && mPlayer.getCurrentPosition() < mPlayer.getDuration() - 1) {
                            mPlayer.start();
                        }
                        updateFragmentInterfaceMediaButton(user);

                        //final AudioServiceFeedbackInterface tempS = mAudioService.get();
                        //if (tempS != null) {
                            //tempS.restartTimeout(MediaPlayerMonitorThread.this);
                        //}
                    }
                }
            });
        }
    }

    @Override
    public void seek(final int progress) {
        final Handler tempH = mHandler.get();
        if(tempH != null) {
            tempH.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer != null) {
                        if (!mPlayer.isPlaying()) {
                            mPlayer.seekTo(progress);
                        }
                        updateFragmentInterfaceMediaButton(true);

                        //final AudioServiceFeedbackInterface tempS = mAudioService.get();
                        //if (tempS != null) {
                            //tempS.restartTimeout(MediaPlayerMonitorThread.this);
                        //}
                    }
                }
            });
        }
    }

    @Override
    public void getProgress() {
        final Handler tempH = mHandler.get();
        if(tempH != null) {
            tempH.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer != null) {
                        final AudioPlayFragmentInterface tempF = mAudioPlayFragment.get();
                        if (tempF != null) {
                            tempF.setProgress(mPlayer.getCurrentPosition());
                        }

                        //final AudioServiceFeedbackInterface tempS = mAudioService.get();
                        //if (tempS != null) {
                            //tempS.restartTimeout(MediaPlayerMonitorThread.this);
                        //}
                    }
                }
            });
        }
    }

    @Override
    public void goBackground() {}

    @Override
    public void goForeground() {}

    @Override
    public void setVolume(final float volume) {
        final Handler tempH = mHandler.get();
        if(tempH != null) {
            tempH.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer != null) {
                        mPlayer.setVolume(volume, volume);

                        //final AudioServiceFeedbackInterface tempS = mAudioService.get();
                        //if (tempS != null) {
                            //tempS.restartTimeout(MediaPlayerMonitorThread.this);
                        //}
                    }
                }
            });
        }
    }


    private void notifyProgress(){
        if(mPlayer != null) {
            final int max = mPlayer.getDuration();
            final int progress = mPlayer.getCurrentPosition();
            final AudioServiceFeedbackInterface tempS = mAudioService.get();
            if (tempS != null) {
                tempS.progress(progress, max, this);
            }
        }
    }

    public void release(){

        final AudioPlayFragmentInterface tempF = mAudioPlayFragment.get();
        if(tempF != null) {
            tempF.clean();
        }

        mAudioPlayFragment.set(null);
        mAudioService.set(null);

        final Handler tempH = mHandlerRelease.get();
        if(tempH != null) {
            tempH.removeCallbacks(mUpdateServerRunnable);
            tempH.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer != null) {
                        mPlayer.release();
                        mPlayer = null;
                    }
                    if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        quit();
                    }
                }
            });
        }
    }

    private void updateFragmentInterfaceMediaButton(final boolean user){
        if(mPlayer.isPlaying()){
            final AudioPlayFragmentInterface tempF = mAudioPlayFragment.get();
            if(tempF != null) {
                tempF.playing(user);
                tempF.setProgress(mPlayer.getCurrentPosition());
            }
            final AudioServiceFeedbackInterface tempS = mAudioService.get();
            if(tempS != null) {
                tempS.playing(this,user);
            }
        }
        else{
            final AudioPlayFragmentInterface tempF = mAudioPlayFragment.get();
            if(tempF != null) {
                tempF.paused(user);
                tempF.setProgress(mPlayer.getCurrentPosition());
            }
            final AudioServiceFeedbackInterface tempS = mAudioService.get();
            if(tempS != null) {
                tempS.paused(this,user);
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        final AudioPlayFragmentInterface tempF = mAudioPlayFragment.get();
        if(tempF != null) {
            tempF.error(AudioPlayFragmentInterface.ErrorCode.NETWORK_ERROR);
            release();
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //AudioServiceFeedbackInterface tempS = mAudioService.get();
        //if(tempS != null) {
            //tempS.restartTimeout(this);
        //}

        mPlayer.start();

        final AudioPlayFragmentInterface tempF = mAudioPlayFragment.get();
        if(tempF != null){
            tempF.startPlay(mPlayer.getDuration(), mPlayer.getCurrentPosition());
        }

        mHandler.set(new Handler());
        mHandler.get().postDelayed(mUpdateServerRunnable, 1000);

        notifyProgress();

        AudioServiceFeedbackInterface tempS = mAudioService.get();
        if(tempS != null) {
            tempS.playing(this, false);
        }
    }
}
