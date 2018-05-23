package chico.fronteirasdaciencia.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.List;

import chico.fronteirasdaciencia.R;
import chico.fronteirasdaciencia.aidl.EpisodeData;
import chico.fronteirasdaciencia.aidl.EpisodeServiceInterface;
import chico.fronteirasdaciencia.fragments.EpisodeCategorySelectionFragment;
import chico.fronteirasdaciencia.services.episode_service.EpisodeService;

/**
 * Created by chico on 08/06/2015. Uhu!
 */

public class MainActivity extends Activity implements ActionInterface, EpisodeCategorySelectionFragment.EpisodeCategoryListener {

    private EpisodeServiceInterface mEpisodeService;
    private EpisodeListAdapter mEpisodeList;
    private EpisodeServiceEventHandler mServiceEventHandler;
    private boolean mFirstHandshake = true;
    private SharedPreferences mPreferences;
    private boolean mHideViewed;
    private boolean mRemoveViewed;
    private FragmentManager mFragmentManager;
    private EpisodeCategorySelectionFragment mCategoryFragment;
    private View mFadeView;
    private String mCurrentCategory;

    @Override
    protected void onCreate(final Bundle saved_instance) {
        super.onCreate(saved_instance);

        setContentView(R.layout.main_layout);
        final ListView episode_list_view = (ListView) findViewById(R.id.episode_list);
        mFadeView = findViewById(R.id.fade_view);

        mEpisodeList = new EpisodeListAdapter(this, this);
        episode_list_view.setAdapter(mEpisodeList);

        playSplashScreen();

        mFragmentManager = getFragmentManager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService();
        openPreferences();
    }

    @Override
    protected void onPause() {
        closePreferences();
        unbindService();
        super.onPause();
    }

    private void closePreferences(){
        mPreferences.edit()
                .putBoolean(getString(R.string.hide_viewed_preference_tag),mHideViewed)
                .putBoolean(getString(R.string.remove_viewed_preference_tag),mRemoveViewed)
                .putString(getString(R.string.episode_category_string_tag),mCurrentCategory)
            .apply();
    }

    private void openPreferences(){
        mPreferences = getSharedPreferences(getString(R.string.preferences_name), Context.MODE_PRIVATE);

        mHideViewed = mPreferences.getBoolean(getString(R.string.hide_viewed_preference_tag), false);
        mRemoveViewed = mPreferences.getBoolean(getString(R.string.remove_viewed_preference_tag), false);
        mCurrentCategory = mPreferences.getString(getString(R.string.episode_category_string_tag), getString(R.string.all_episodes_category_string));

        mEpisodeList.setHideViewedEpisodes(mHideViewed);
        mEpisodeList.setRemoveViewedEpisodes(mRemoveViewed);
    }

    private void playSplashScreen(){
        final ImageView splash = (ImageView) findViewById(R.id.splash_image);
        splash.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        splash.animate()
                .setStartDelay(3000L)
                .setDuration(2000L)
                .alpha(0.0f)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        splash.setVisibility(View.GONE);
                    }
                });
    }

    private void bindService(){
        bindService(EpisodeService.makeIntent(this), mServiceConnection, BIND_AUTO_CREATE);
        mServiceEventHandler = new EpisodeServiceEventHandler(mEpisodeList, new Handler());
    }

    private void unbindService(){
        if(mEpisodeService != null) {
            try {
                mEpisodeService.releaseService();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mServiceConnection);
        mEpisodeService = null;
        mServiceEventHandler = null;
    }

    private void serviceHandshake(){
        try {
            final List<EpisodeData> episode_list = mEpisodeService.doServiceHandshake(mServiceEventHandler, mFirstHandshake);
            mFirstHandshake = false;
            mEpisodeList.setEpisodeList(episode_list);

            if(mCategoryFragment == null){
                mCategoryFragment = new EpisodeCategorySelectionFragment();
                mCategoryFragment.parseCategoriesLists(mEpisodeService.getSeasonList(), mEpisodeService.getCategoryList(), this);
                mCategoryFragment.setCategoryListener(this);
                mCategoryFragment.setFadeView(mFadeView);
                mCategoryFragment.setCurrentCategory(mCurrentCategory);
            }
        }
        catch(RemoteException ignored){
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mEpisodeService = EpisodeServiceInterface.Stub.asInterface(service);
            serviceHandshake();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mEpisodeService = null;
        }
    };

    @Override
    public void cancelDownload(final long episode_id) {
        if(mEpisodeService != null){
            try {
                mEpisodeService.cancelDownload(episode_id);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deleteFile(final long episode_id) {
        if(mEpisodeService != null){
            try {
                mEpisodeService.deleteFile(episode_id);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void episodeViewed(final long episode_id, final boolean viewed) {
        if(mEpisodeService != null){
            try {
                mEpisodeService.episodeViewed(episode_id, viewed);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void showDialogFragment(final DialogFragment dialog_fragment, final String tag) {
        dialog_fragment.show(getFragmentManager(), tag);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.hide_viewed).setChecked(mHideViewed);
        menu.findItem(R.id.remove_viewed).setChecked(mRemoveViewed);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.about) {
            final Intent intent = new Intent(this,AboutActivity.class);
            try {
                intent.putExtra(AboutActivity.PODCAST_DATA_TAG, mEpisodeService.getPodcastData());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            startActivity(intent);
            return true;
        }
        else if (id == R.id.help) {
            startActivity(new Intent(this,HelpActivity.class));
            return true;
        }
        else if (id == R.id.hide_viewed){
            mHideViewed = !mHideViewed;
            item.setChecked(mHideViewed);
            mEpisodeList.setHideViewedEpisodes(mHideViewed);
            return true;
        }
        else if (id == R.id.remove_viewed){
            if(!mRemoveViewed){
                launchViewedEpisodeRemovalConfirmationDialog(item);
            }
            else{
                processRemoveViewedMenuOption(item);
            }
            return true;
        }
        else if (id == android.R.id.home){
            toggleCategoryFragment();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleCategoryFragment() {
        if (mCategoryFragment != null) {
            if (mFragmentManager.findFragmentByTag(EpisodeCategorySelectionFragment.TRANSACTION_TAG) == null) {
                mCategoryFragment.setCurrentCategory(mCurrentCategory);
                mFragmentManager.beginTransaction()
                        .setCustomAnimations(
                                R.animator.category_fragment_enter_animation,
                                R.animator.category_fragment_leave_animation,
                                R.animator.category_fragment_enter_animation,
                                R.animator.category_fragment_leave_animation
                        )
                        .addToBackStack(null)
                        .add(R.id.main_view, mCategoryFragment, EpisodeCategorySelectionFragment.TRANSACTION_TAG)
                        .commit();
            } else {
                if (mCategoryFragment.isAdded()) {
                    onBackPressed();
                }
            }
        }
    }

    private void launchViewedEpisodeRemovalConfirmationDialog(final MenuItem item) {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.trash)
                .setMessage(getString(R.string.remove_viewed_episodes_dialog_message))
                .setTitle(getString(R.string.remove_viewed_episodes_dialog_title))
                .setPositiveButton(getString(R.string.remove_viewed_episodes_dialog_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        processRemoveViewedMenuOption(item);
                    }
                })
                .setNegativeButton(getString(R.string.remove_viewed_episodes_dialog_negative_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void processRemoveViewedMenuOption(final MenuItem item){
        mRemoveViewed = !mRemoveViewed;
        item.setChecked(mRemoveViewed);
        mEpisodeList.setRemoveViewedEpisodes(mRemoveViewed);
    }

    @Override
    public void setCategory(final String category) {
        mCurrentCategory = category;
        mEpisodeList.setCategory(category);
    }

    @Override
    public void killCategoryFragment() {
        toggleCategoryFragment();
    }
}
