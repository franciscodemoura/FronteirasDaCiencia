package chico.fronteirasdaciencia.aidl;

import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import chico.fronteirasdaciencia.fragments.EpisodePlayerDialogFragment;
import chico.fronteirasdaciencia.activities.ActionInterface;

/**
 * Created by chico on 07/06/2015. Uhu!
 */

public class EpisodeData implements Parcelable {

    public enum EpisodeState{
        SEEKING,
        ABSENT,
        DOWNLOADING,
        DOWNLOADED
    }

    private final long mId;
    private Uri mLocalFile;
    private final Uri mUrl;
    private final String mTitle;
    private final String mDescription;
    private final String mDate;
    private final int mFileSize;
    private EpisodeState mState = EpisodeState.SEEKING;
    private boolean mViewed;
    private final List<String> mCategories;

    public EpisodeData(
            final long id,
            final String title,
            final String description,
            final Uri url,
            final String date,
            final int file_size,
            final boolean viewed,
            final List<String> categories
    ){
        mId = id;
        mTitle = title;
        mDescription = description;
        mUrl = url;
        mDate = date;
        mFileSize = file_size;
        mViewed = viewed;
        mCategories = categories;
    }

    public EpisodeData(final EpisodeData episode_data){
        mId = episode_data.mId;
        mTitle = episode_data.mTitle;
        mDescription = episode_data.mDescription;
        mUrl = episode_data.mUrl;
        mDate = episode_data.mDate;
        mFileSize = episode_data.mFileSize;
        mState = episode_data.mState;
        mViewed = episode_data.mViewed;
        mCategories = episode_data.mCategories;
    }

    public long getId() {
        return mId;
    }

    public Uri getLocalFile() {
        return mLocalFile;
    }

    public Uri getUrl() {
        return mUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    /*public String getDate() {
        return mDate;
    }*/

    public int getFileSize() {
        return mFileSize;
    }

    public EpisodeState getState() {
        return mState;
    }

    public boolean getViewed() {
        return mViewed;
    }

    public void setViewed(final boolean viewed) {
        mViewed = viewed;
    }

    public void setState(final EpisodeState state) {
        mState = state;
    }

    public void setLocalFile(final Uri local_file) {
        mLocalFile = local_file;
    }

    public boolean hasCategory(final String category){
        for(final String c : mCategories){
            if(c.equals(category)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeParcelable(mLocalFile, flags);
        dest.writeParcelable(mUrl, flags);
        dest.writeString(mTitle);
        dest.writeString(mDescription);
        dest.writeString(mDate);
        dest.writeInt(mFileSize);
        dest.writeInt(mState.ordinal());
        dest.writeInt(mViewed?1:0);
        dest.writeList(mCategories);
    }

    private EpisodeData(Parcel in) {
        mId = in.readLong();
        mLocalFile = in.readParcelable(null);
        mUrl = in.readParcelable(null);
        mTitle = in.readString();
        mDescription = in.readString();
        mDate = in.readString();
        mFileSize = in.readInt();
        mState = EpisodeState.values()[in.readInt()];
        mViewed = in.readInt() == 1;
        mCategories = new ArrayList<>();
        in.readList(mCategories,null);
    }

    public static final Parcelable.Creator<EpisodeData> CREATOR =
            new Parcelable.Creator<EpisodeData>() {

                public EpisodeData createFromParcel(Parcel in) {
                    return new EpisodeData(in);
                }

                public EpisodeData[] newArray(int size) {
                    return new EpisodeData[size];
                }
            };

    public static boolean isStateTransitionValid(final EpisodeState old_state, final EpisodeState new_state){
        boolean [][] table =
                {
                        { //old_state = SEEKING
                                false, //new_state = SEEKING
                                true, //new_state = ABSENT
                                true, //new_state = DOWNLOADING
                                true //new_state = DOWNLOADED
                        },
                        { //old_state = ABSENT
                                false, //new_state = SEEKING
                                false, //new_state = ABSENT
                                true, //new_state = DOWNLOADING
                                false //new_state = DOWNLOADED
                        },
                        { //old_state = DOWNLOADING
                                false, //new_state = SEEKING
                                true, //new_state = ABSENT
                                false, //new_state = DOWNLOADING
                                true //new_state = DOWNLOADED
                        },
                        { //old_state = DOWNLOADED
                                false, //new_state = SEEKING
                                true, //new_state = ABSENT
                                false, //new_state = DOWNLOADING
                                false //new_state = DOWNLOADED
                        },
                };

        return table[old_state.ordinal()][new_state.ordinal()];
    }

    /*public static void playEpisode(final EpisodeData episode, final Context context){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(episode.getState() == EpisodeState.DOWNLOADED ? episode.getLocalFile() : episode.getUrl(), "audio/*");
        if(intent.resolveActivity(context.getPackageManager()) == null){
            Toast.makeText(context, R.string.no_application_to_listen_to_episode, Toast.LENGTH_LONG).show();
        }
        else {
            context.startActivity(intent);
        }
    }*/

    public static void playEpisode(final EpisodeData episode, final ActionInterface action_interface){
        final DialogFragment episode_play_fragment = new EpisodePlayerDialogFragment();

        final Bundle bundle = new Bundle();
        bundle.putParcelable(EpisodePlayerDialogFragment.EPISODE_DATA_TAG,episode);
        episode_play_fragment.setArguments(bundle);

        action_interface.showDialogFragment(episode_play_fragment, EpisodePlayerDialogFragment.EPISODE_PLAYER_FRAGMENT_TAG);
    }
}
