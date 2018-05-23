package chico.fronteirasdaciencia.fragments;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicReference;

import chico.fronteirasdaciencia.R;
import chico.fronteirasdaciencia.aidl.EpisodeData;
import chico.fronteirasdaciencia.services.audio_service.AudioPlayerService;
import chico.fronteirasdaciencia.services.audio_service.AudioPlayerServiceInterface;

/**
 * Created by chico on 15/07/2015. Uhu!
 */
public class EpisodePlayerDialogFragment extends DialogFragment implements AudioPlayFragmentInterface {

    public static final String EPISODE_PLAYER_FRAGMENT_TAG = "episode_player_fragment_tag";
    public static final String EPISODE_DATA_TAG = "episode_data_tag";

    private EpisodeData mEpisodeData;
    private AudioPlayerServiceInterface mAudioServiceInterface;
    private View mWaitBar;
    private View mMainView;
    private SeekBar mPlayerProgress;
    private TextView mCurrentTime;
    private TextView mDuration;
    private TextView mErrorMessageView;
    private ImageButton mMediaButton;
    private ImageButton mRewindButton;
    private boolean mMediaButtonPlaying = true;
    private volatile AtomicReference<Handler> mHandler;
    private Runnable mProgressChecker;
    private long mEpisodeId;





    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mEpisodeData = getArguments().getParcelable(EPISODE_DATA_TAG);
        mEpisodeId = mEpisodeData.getId();

        getDialog().setTitle(getString(R.string.episode_fragment_or_removal_text) + " " + mEpisodeId);

        View v = inflater.inflate(R.layout.episode_player_fragment_layout, container, false);

        initViews(v);
        bindService();

        mHandler = new AtomicReference<>(new Handler());
        mProgressChecker = new Runnable() {
            @Override
            public void run() {
                if(mAudioServiceInterface != null) {
                    mAudioServiceInterface.getProgress();
                }
            }
        };

        return v;
    }

    @Override
    public void onDestroy(){
        if(mAudioServiceInterface != null) {
            mAudioServiceInterface.goForeground();
            mAudioServiceInterface.stop();
        }
        unbindService();
        mAudioServiceInterface = null;
        mHandler.set(null);
        super.onDestroy();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mAudioServiceInterface != null) {
            mAudioServiceInterface.goForeground();
        }
    }

    @Override
    public void onPause(){
        if(mAudioServiceInterface != null) {
            mAudioServiceInterface.goBackground();
        }
        super.onPause();
    }





    @Override
    public void startPlay(final int max, final int progress) {
        final Handler tempH = mHandler.get();
        tempH.post(new Runnable() {
            @Override
            public void run() {
                viewMainView();
                mPlayerProgress.setMax(max);
                mPlayerProgress.setProgress(progress);
                mDuration.setText(AudioPlayerService.getPlayTimeString(max));
                scheduleProgressCheck();
            }
        });
    }

    @Override
    public void paused(final boolean user) {
        final Handler tempH = mHandler.get();
        tempH.post(new Runnable() {
            @Override
            public void run() {
                mMediaButtonPlaying = false;
                updateMediaButtonImage();
                viewMainView();
            }
        });
    }

    @Override
    public void playing(final boolean user) {
        final Handler tempH = mHandler.get();
        tempH.post(new Runnable() {
            @Override
            public void run() {
                mMediaButtonPlaying = true;
                updateMediaButtonImage();
                viewMainView();
            }
        });
    }

    @Override
    public void error(final ErrorCode error_code) {
        final Handler tempH = mHandler.get();
        tempH.post(new Runnable() {
            @Override
            public void run() {
                viewErrorMessage(error_code);
                clean();
            }
        });
    }

    @Override
    public void setProgress(final int progress) {
        final Handler tempH = mHandler.get();
        tempH.post(new Runnable() {
            @Override
            public void run() {
                mPlayerProgress.setProgress(progress);
                scheduleProgressCheck();
                viewMainView();
            }
        });
    }

    @Override
    public void clean() {
        abortProgressCheck();
    }

    @Override
    public void terminate() {
        super.dismiss();
    }


    private void scheduleProgressCheck(){
        if(getActivity() == null){
            return;
        }
        if(mAudioServiceInterface != null) {
            final Handler tempH = mHandler.get();
            tempH.postDelayed(mProgressChecker, getResources().getInteger(R.integer.media_player_progress_check_time));
        }
    }

    private void abortProgressCheck() {
        final Handler tempH = mHandler.get();
        tempH.removeCallbacks(mProgressChecker);
    }

    private void updateMediaButtonImage(){
        if(mMediaButtonPlaying) {
            if (getActivity() != null) {
                mMediaButton.setImageDrawable(ContextCompat.getDrawable(getActivity(),android.R.drawable.ic_media_pause));
            }
        }
        else{
            if(getActivity() != null) {
                mMediaButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), android.R.drawable.ic_media_play));
            }
        }
    }





    private void bindService(){
        getActivity().bindService(AudioPlayerService.makeIntent(getActivity()), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService(){
        getActivity().unbindService(mServiceConnection);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mAudioServiceInterface = (AudioPlayerServiceInterface) service;
            if(!mAudioServiceInterface.play(
                    mEpisodeData.getState() == EpisodeData.EpisodeState.DOWNLOADED ? mEpisodeData.getLocalFile() : mEpisodeData.getUrl(),
                    EpisodePlayerDialogFragment.this,
                    mEpisodeId))
            {
                viewErrorMessage(ErrorCode.AUDIO_ERROR);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mAudioServiceInterface = null;
        }
    };





    private void initViews(final View v){
        mMainView = v.findViewById(R.id.main_view);
        mWaitBar = v.findViewById(R.id.wait_bar);

        mPlayerProgress = (SeekBar) v.findViewById(R.id.player_progress);
        mPlayerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    mAudioServiceInterface.seek(progress);
                }
                mCurrentTime.setText(
                        AudioPlayerService.getPlayTimeString(progress) +
                                "  /  " + (int) ((float) progress / (float) (seekBar.getMax() == 0 ? 1000000000 : seekBar.getMax()) * 100.0f) + "%" + " ");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mAudioServiceInterface.pause(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mAudioServiceInterface.restart(true);
            }
        });

        mCurrentTime = (TextView) v.findViewById(R.id.current_time);
        mDuration = (TextView) v.findViewById(R.id.duration);
        mErrorMessageView = (TextView) v.findViewById(R.id.play_error_message);

        mMediaButton = (ImageButton) v.findViewById(R.id.media_button);
        mMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaButtonPlaying) {
                    mAudioServiceInterface.pause(true);
                } else {
                    mAudioServiceInterface.restart(true);
                }
            }
        });

        mRewindButton = (ImageButton) v.findViewById(R.id.rewind_button);
        mRewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                int progress = mPlayerProgress.getProgress();
                progress -= 15*1000;
                if (progress < 0){
                    progress = 0;
                }

                mAudioServiceInterface.pause(true);
                mAudioServiceInterface.seek(progress);
                mAudioServiceInterface.restart(true);

                mPlayerProgress.setProgress(progress);
            }
        });
    }

    /*private void viewWaitBar(){
        if(mWaitBar.getVisibility() != View.VISIBLE) {
            mWaitBar.setVisibility(View.VISIBLE);
            mErrorMessageView.setVisibility(View.INVISIBLE);
            mMainView.setVisibility(View.INVISIBLE);
        }
    }*/

    private void viewMainView(){
        if(mMainView.getVisibility() != View.VISIBLE) {
            mWaitBar.setVisibility(View.INVISIBLE);
            mErrorMessageView.setVisibility(View.INVISIBLE);
            mMainView.setVisibility(View.VISIBLE);
        }
    }

    private void viewErrorMessage(final AudioPlayFragmentInterface.ErrorCode code){
        if(mErrorMessageView.getVisibility() != View.VISIBLE) {
            mWaitBar.setVisibility(View.INVISIBLE);
            mErrorMessageView.setVisibility(View.VISIBLE);
            mMainView.setVisibility(View.INVISIBLE);
        }
        switch(code){
            case NETWORK_ERROR:
                mErrorMessageView.setText(getString(R.string.play_error_message_net));
                break;
            case AUDIO_ERROR:
                mErrorMessageView.setText(getString(R.string.play_error_message_audio));
                break;
            default:
                break;
        }
    }
}
