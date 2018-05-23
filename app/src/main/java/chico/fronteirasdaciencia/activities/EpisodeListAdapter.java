package chico.fronteirasdaciencia.activities;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import chico.fronteirasdaciencia.R;
import chico.fronteirasdaciencia.aidl.EpisodeData;
import chico.fronteirasdaciencia.aidl.EpisodeData.EpisodeState;
import chico.fronteirasdaciencia.aidl.ServiceEvents;
import chico.fronteirasdaciencia.fragments.EpisodeCategorySelectionFragment;
import chico.fronteirasdaciencia.fragments.EpisodeDescriptionDialogFragment;
import chico.fronteirasdaciencia.services.episode_service.EpisodeService;

/**
 * Created by chico on 09/06/2015. Uhu!
 */

class EpisodeListAdapter implements ListAdapter, ServiceEvents, EpisodeCategorySelectionFragment.EpisodeCategoryListener{

    private List<EpisodeData> mEpisodeList = new ArrayList<>();
    private final List<EpisodeData> mDisplayEpisodeList = new ArrayList<>();
    private final List<DataSetObserver> mDataObservers = new ArrayList<>();
    private final LayoutInflater mInflater;
    private final Context mContext;
    private final Drawable mDownloadIcon;
    private final Drawable mTrashIcon;
    private final ActionInterface mActionInterface;
    private boolean mHideViewedEpisodes = false;
    private boolean mRemoveViewedEpisodes = false;
    private String mEpisodeCategory;
    private final String mAllEpisodesString;

    private final View.OnClickListener mPlayButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            EpisodeData.playEpisode(getEpisodeData((Long) v.getTag()),mActionInterface);
        }
    };

    private final View.OnClickListener mActionButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final EpisodeData episode = getEpisodeData((Long) v.getTag());
            switch(episode.getState()){
                case ABSENT:
                    EpisodeService.startEpisodeDownload(mContext,episode.getId());
                    break;
                case DOWNLOADED:
                    launchEpisodeRemovalConfirmationDialog(episode.getId());
                    break;
                case DOWNLOADING:
                    mActionInterface.cancelDownload(episode.getId());
                    break;
                case SEEKING:
                    Toast.makeText(mContext,"Wait a little, please",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final View.OnClickListener mViewedCheckListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final EpisodeData episode = getEpisodeData((Long) v.getTag());
            final boolean viewed = !episode.getViewed();
            mActionInterface.episodeViewed(episode.getId(), viewed);
            if(viewed && mRemoveViewedEpisodes && episode.getState() == EpisodeState.DOWNLOADED){
                mActionInterface.deleteFile(episode.getId());
            }
        }
    };

    private void launchEpisodeRemovalConfirmationDialog(final long episode_id) {
        new AlertDialog.Builder(mContext)
                .setIcon(R.drawable.trash)
                .setMessage(R.string.delete_downloaded_episode_confirmation_message)
                .setTitle(mContext.getString(R.string.episode_fragment_or_removal_text) + " " + episode_id)
                .setNegativeButton(R.string.abort_episode_removal_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.episode_removal_confirmation_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActionInterface.deleteFile(episode_id);
                    }
                })
                .show();
    }

    private final View.OnClickListener mItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final DialogFragment episode_description_fragment = new EpisodeDescriptionDialogFragment();

            final Bundle bundle = new Bundle();
            final EpisodeData episode = getEpisodeData((Long) ((ViewHolder) v.getTag()).mPlayButton.getTag());
            bundle.putParcelable(EpisodeDescriptionDialogFragment.EPISODE_DATA_TAG,episode);
            episode_description_fragment.setArguments(bundle);

            mActionInterface.showDialogFragment(episode_description_fragment,EpisodeDescriptionDialogFragment.EPISODE_DESCRIPTION_FRAGMENT_TAG);
        }
    };

    public void setCategory(final String category) {
        mEpisodeCategory = category;
        rebuildDisplayEpisodeList();
        notifyChanges();
    }

    @Override
    public void killCategoryFragment() {

    }

    private class ViewHolder{
        public TextView mEpisodeNumber;
        public TextView mEpisodeTitle;
        public View mPlayButton;
        public View mProgressBar;
        public ImageButton mActionButton;
        public ImageView mViewedCheck;
    }

    EpisodeListAdapter(
            final Context context,
            final ActionInterface action_interface
    ){
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mDownloadIcon = ContextCompat.getDrawable(mContext, R.drawable.download);
        mTrashIcon = ContextCompat.getDrawable(mContext,R.drawable.trash);
        //Drawable mCheckIcon = ContextCompat.getDrawable(mContext,R.drawable.checked);
        mActionInterface = action_interface;
        mAllEpisodesString = context.getString(R.string.all_episodes_category_string);
    }

    public void setEpisodeList(final List<EpisodeData> episode_list){
        mEpisodeList = episode_list;
        rebuildDisplayEpisodeList();
        notifyChanges();
    }

    public void setHideViewedEpisodes(boolean hide_viewed_episodes) {
        this.mHideViewedEpisodes = hide_viewed_episodes;
        rebuildDisplayEpisodeList();
        notifyChanges();
    }

    public void setRemoveViewedEpisodes(boolean remove_viewed_episodes) {
        this.mRemoveViewedEpisodes = remove_viewed_episodes;
        if(remove_viewed_episodes){
            for (final EpisodeData episode : mEpisodeList) {
                if (episode.getViewed() && episode.getState() == EpisodeState.DOWNLOADED) {
                    mActionInterface.deleteFile(episode.getId());
                }
            }
        }
        //notifyChanges();
    }

    private void rebuildDisplayEpisodeList(){
        mDisplayEpisodeList.clear();
        for (final EpisodeData episode : mEpisodeList) {
            if (
                    (!episode.getViewed() || !mHideViewedEpisodes) &&
                            (mEpisodeCategory == null || mEpisodeCategory.equals(mAllEpisodesString) || episode.hasCategory(mEpisodeCategory))
                    ) {
                mDisplayEpisodeList.add(episode);
            }
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer) {
        mDataObservers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer) {
        mDataObservers.remove(observer);
    }

    private void notifyChanges(){
        for(DataSetObserver observer : mDataObservers){
            observer.onChanged();
        }
    }

    @Override
    public int getCount() {
        return mDisplayEpisodeList.size();
    }

    /*private EpisodeData getEpisodeData(final int position){
        return mEpisodeList.get(mEpisodeList.size() - position - 1);
    }*/

    private EpisodeData getDisplayEpisodeData(final int position){
        return mDisplayEpisodeList.get(mDisplayEpisodeList.size() - position - 1);
    }

    @Override
    public Object getItem(final int position) {
        return getDisplayEpisodeData(position);
    }

    @Override
    public long getItemId(final int position) {
        return getDisplayEpisodeData(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final EpisodeData episode_data = getDisplayEpisodeData(position);

        View v;
        ViewHolder vh;

        if(convertView == null){
            v = mInflater.inflate(R.layout.list_item_layout,parent,false);
            v.setOnClickListener(mItemClickListener);

            vh = new ViewHolder();
            v.setTag(vh);

            vh.mEpisodeNumber = (TextView) v.findViewById(R.id.episode_number);
            vh.mEpisodeTitle = (TextView) v.findViewById(R.id.episode_title);
            vh.mPlayButton = v.findViewById(R.id.play_button);
            vh.mProgressBar = v.findViewById(R.id.progress_bar);
            vh.mActionButton = (ImageButton) v.findViewById(R.id.action_button);
            vh.mViewedCheck = (ImageView) v.findViewById(R.id.viewed_check);

            vh.mPlayButton.setOnClickListener(mPlayButtonListener);
            vh.mActionButton.setOnClickListener(mActionButtonListener);
            vh.mViewedCheck.setOnClickListener(mViewedCheckListener);
        }
        else{
            v = convertView;
            vh = (ViewHolder) convertView.getTag();
        }

        vh.mEpisodeNumber.setText(String.valueOf(episode_data.getId()));
        vh.mEpisodeTitle.setText(episode_data.getTitle());

        vh.mPlayButton.setTag(episode_data.getId());
        vh.mActionButton.setTag(episode_data.getId());
        vh.mViewedCheck.setTag(episode_data.getId());

        switch(episode_data.getState()){
            case ABSENT:
                vh.mProgressBar.setVisibility(View.INVISIBLE);
                vh.mActionButton.setImageDrawable(mDownloadIcon);
                vh.mEpisodeTitle.setTypeface(null, Typeface.NORMAL);
                break;
            case DOWNLOADED:
                vh.mProgressBar.setVisibility(View.INVISIBLE);
                vh.mActionButton.setImageDrawable(mTrashIcon);
                vh.mEpisodeTitle.setTypeface(null, Typeface.BOLD);
                break;
            case DOWNLOADING:
                vh.mProgressBar.setVisibility(View.VISIBLE);
                vh.mActionButton.setImageDrawable(mDownloadIcon);
                vh.mEpisodeTitle.setTypeface(null, Typeface.ITALIC);
                break;
            case SEEKING:
                vh.mProgressBar.setVisibility(View.VISIBLE);
                vh.mActionButton.setImageDrawable(null);
                vh.mEpisodeTitle.setTypeface(null, Typeface.NORMAL);
                break;
        }
        vh.mViewedCheck.setAlpha(episode_data.getViewed() ? 0.4f : 0.0f);
        v.setBackgroundColor(mContext.getResources().getIntArray(R.array.item_colors)[position%2]);

        return v;
    }

    @Override
    public int getItemViewType(final int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mDisplayEpisodeList.isEmpty();
    }

    @Override
    public void episodeDownloading(final long episode_id) {
        changeEpisodeState(episode_id,EpisodeState.DOWNLOADING, null);
    }

    @Override
    public void episodeDownloaded(final long episode_id, final Uri local_file) {
        changeEpisodeState(episode_id,EpisodeState.DOWNLOADED, local_file);
    }

    @Override
    public void episodeAbsent(final long episode_id) {
        changeEpisodeState(episode_id,EpisodeState.ABSENT, null);
    }

    @Override
    public void newEpisode(final EpisodeData episode) {
        if(EpisodeService.LOCAL_SERVICE){
            rebuildDisplayEpisodeList();
            notifyChanges();
        }
        else {
            add(episode);
        }
    }

    @Override
    public void episodeViewed(final long episode_id, final boolean viewed) {
        if (!EpisodeService.LOCAL_SERVICE) {
            final EpisodeData episode = getEpisodeData(episode_id);
            if (episode == null) throw new AssertionError();
            episode.setViewed(viewed);
        }

        rebuildDisplayEpisodeList();
        notifyChanges();
    }

    @Override
    public IBinder asBinder() {
        return null;
    }

    private void add(final EpisodeData episode){
        mEpisodeList.add(episode);
        rebuildDisplayEpisodeList();
        notifyChanges();
    }

    private EpisodeData getEpisodeData(final long episode_id){
        if (episode_id > (long) mEpisodeList.size()) throw new AssertionError();
        if (episode_id < 1L) throw new AssertionError();

        final EpisodeData episode = mEpisodeList.get((int)episode_id - 1);

        if (episode.getId() != episode_id) throw new AssertionError();

        return episode;
    }

    private boolean changeEpisodeState(final long episode_id, final EpisodeState new_state, final Uri local_file){
        if(EpisodeService.LOCAL_SERVICE){
            notifyChanges();
            return true;
        }

        final EpisodeData episode = getEpisodeData(episode_id);

        if (episode == null) throw new AssertionError();

        EpisodeState old_state = episode.getState();
        if (EpisodeData.isStateTransitionValid(old_state,new_state)){
            episode.setState(new_state);
            episode.setLocalFile(local_file);
            notifyChanges();
            return true;
        }
        else{
            return false;
        }
    }
}
