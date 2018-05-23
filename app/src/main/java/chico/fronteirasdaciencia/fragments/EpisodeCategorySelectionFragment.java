package chico.fronteirasdaciencia.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import chico.fronteirasdaciencia.R;

/**
 * Created by chico on 21/07/2015. Uhu!
 */
public class EpisodeCategorySelectionFragment extends Fragment {

    public static final String TRANSACTION_TAG = "category_fragment";

    public interface EpisodeCategoryListener{
        void setCategory(String category);
        void killCategoryFragment();
    }

    private EpisodeCategoryListener mCategoryListener;
    private ListView mCategoryListView;
    private CategoryListAdapter mCategoryList;
    private String mCurrentCategory;
    private View mFadeView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.episode_category_fragment_layout,container,false);

        mCategoryListView = (ListView) v.findViewById(R.id.category_list);
        mCategoryListView.setAdapter(mCategoryList);
        mCategoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                final String category = mCategoryList.itemClicked(id);
                if(category != null) {
                    if (mCategoryListener != null) {
                        mCategoryListener.setCategory(category);
                    }
                }
            }
        });

        v.findViewById(R.id.category_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCategoryListener != null) {
                    mCategoryListener.killCategoryFragment();
                }
            }
        });

        return v;
    }

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        animateFadeView(false);
    }

    @Override
    public void onDetach(){
        animateFadeView(true);
        super.onDetach();
    }

    public void parseCategoriesLists(final List<String> season_list, final List<String> category_list, Context context){
        mCategoryList = new CategoryListAdapter(season_list, category_list, context);
        if(mCategoryListView != null) {
            mCategoryListView.setAdapter(mCategoryList);
        }

        mCategoryList.setCurrentCategory(mCurrentCategory);
    }

    public void setCategoryListener(final EpisodeCategoryListener category_listener){
        mCategoryListener = category_listener;
    }

    public void setCurrentCategory(final String current_category){
        mCurrentCategory = current_category;
        mCategoryList.setCurrentCategory(current_category);
        if (mCategoryListener != null) {
            mCategoryListener.setCategory(mCurrentCategory);
        }
    }

    public void setFadeView(final View fade_view){
        mFadeView = fade_view;
    }

    private void animateFadeView(final boolean reverse){
        if(mFadeView != null) {
            mFadeView.animate()
                    .setDuration(getResources().getInteger(R.integer.category_fragment_animation_time))
                    .alpha(reverse ? 0.0f : 0.5f)
                    .start();
        }
    }
}
