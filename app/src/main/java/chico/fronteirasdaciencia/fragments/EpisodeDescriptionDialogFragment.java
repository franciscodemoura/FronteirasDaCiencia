package chico.fronteirasdaciencia.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import chico.fronteirasdaciencia.R;
import chico.fronteirasdaciencia.activities.ActionInterface;
import chico.fronteirasdaciencia.aidl.EpisodeData;

/**
 * Created by chico on 07/07/2015. Uhu!
 */
public class EpisodeDescriptionDialogFragment extends DialogFragment {

    public static final String EPISODE_DESCRIPTION_FRAGMENT_TAG = "episode_description_fragment_tag";
    public static final String EPISODE_DATA_TAG = "episode_description_tag";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final EpisodeData episode = getArguments().getParcelable(EPISODE_DATA_TAG);

        getDialog().setTitle(getString(R.string.episode_fragment_or_removal_text) + " " + episode.getId());

        View v = inflater.inflate(R.layout.episode_description_fragment_layout,container,false);

        ((TextView) v.findViewById(R.id.episode_description)).setText(episode.getDescription().replaceAll("[\n\r\t]", " ").replaceAll("[ ]{2,}"," "));
        ((TextView) v.findViewById(R.id.episode_title)).setText(episode.getTitle());

        v.findViewById(R.id.play_button_description).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EpisodeData.playEpisode(episode,(ActionInterface)getActivity());
            }
        });

        return v;
    }
}
