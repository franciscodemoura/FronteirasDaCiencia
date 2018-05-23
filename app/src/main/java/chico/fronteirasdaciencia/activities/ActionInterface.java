package chico.fronteirasdaciencia.activities;

import android.app.DialogFragment;

/**
 * Created by chico on 14/06/2015. Uhu!
 */
public interface ActionInterface {
    void cancelDownload(long episode_id);
    void deleteFile(long episode_id);
    void episodeViewed(long episode_id, boolean viewed);
    void showDialogFragment(DialogFragment dialog_fragment, String tag);
}
