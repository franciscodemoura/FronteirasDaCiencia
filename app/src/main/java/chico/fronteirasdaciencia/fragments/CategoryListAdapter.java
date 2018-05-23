package chico.fronteirasdaciencia.fragments;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import chico.fronteirasdaciencia.R;

/**
 * Created by chico on 23/07/2015. Uhu!
 */
class CategoryListAdapter implements ListAdapter{

    private class ListElement{
        public final int mParent;
        public final String mCategory;
        public boolean mExpanded;

        public ListElement(final int parent, final String category, final boolean expanded) {
            mParent = parent;
            mCategory = category;
            mExpanded = expanded;
        }

        public int getLevel(){
            return mParent == -1 ? 0 : 1 + mCategories.get(mParent).getLevel();
        }

        public int getPosition(){
            return mCategories.indexOf(this);
        }

        public boolean isVisible(){
            return mParent == -1 || (mCategories.get(mParent).mExpanded && mCategories.get(mParent).isVisible());
        }

        public boolean isLeaf(){
            final int p = getPosition();
            for(final ListElement e : mCategories){
                if(e.mParent == p){
                    return false;
                }
            }
            return true;
        }
    }

    private class ViewHolder{
        public TextView mCategoryText;
        public ImageView mArrowImage;
        public View mBackground;
    }

    private final List<ListElement> mCategories = new ArrayList<>();
    private String mCurrentCategory;
    private final List<DataSetObserver> mDataObservers = new ArrayList<>();
    private final LayoutInflater mInflater;
    private final Drawable mUpArrow;
    private final Drawable mDownArrow;
    private final String mAllEpisodesString;


    public CategoryListAdapter(final List<String> seasons, final List<String> categories, final Context context){

        mAllEpisodesString = context.getString(R.string.all_episodes_category_string);

        int p = 0;
        mCategories.add(new ListElement(-1,mAllEpisodesString,false));
        p++;

        if(!seasons.isEmpty()) {
            final int season_label_position = p;
            mCategories.add(new ListElement(-1, "Por temporada", false));
            p++;

            for (final String season : seasons) {
                mCategories.add(new ListElement(season_label_position, season, false));
                p++;
            }
        }

        if(!categories.isEmpty()) {
            final int category_label_position = p;
            mCategories.add(new ListElement(-1, "Por assunto", false));
            p++;

            for (final String category : categories) {
                mCategories.add(new ListElement(category_label_position, category, false));
                p++;
            }
        }

        mInflater = LayoutInflater.from(context);

        mUpArrow = ContextCompat.getDrawable(context, android.R.drawable.arrow_up_float);
        mDownArrow = ContextCompat.getDrawable(context, android.R.drawable.arrow_down_float);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(final int position) {
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

    @Override
    public int getCount() {
        int c = 0;
        for(final ListElement e : mCategories){
            if(e.isVisible()){
                c++;
            }
        }
        return c;
    }

    @Override
    public Object getItem(int position) {
        int c = 0;

        for(final ListElement e : mCategories){
            if(e.isVisible()){
                if(position == c){
                    return e;
                }
                c++;
            }
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return ((ListElement) getItem(position)).getPosition();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(mInflater == null){
            return null;
        }

        final ListElement e = (ListElement) getItem(position);

        View v;
        ViewHolder vh;

        if(convertView == null){
            v = mInflater.inflate(R.layout.category_item_layout,parent,false);

            vh = new ViewHolder();
            v.setTag(vh);

            vh.mCategoryText = (TextView) v.findViewById(R.id.category_text);
            vh.mArrowImage = (ImageView) v.findViewById(R.id.category_arrow);
            vh.mBackground = v.findViewById(R.id.category_background);
        }
        else{
            v = convertView;
            vh = (ViewHolder) convertView.getTag();
        }

        vh.mCategoryText.setText(e.mCategory);
        vh.mCategoryText.setTranslationX(e.getLevel() * 20);
        vh.mCategoryText.setTypeface(null, e.isLeaf() ? Typeface.NORMAL : Typeface.BOLD);
        vh.mArrowImage.setImageDrawable(e.isLeaf() ? null : e.mExpanded ? mUpArrow : mDownArrow);
        vh.mBackground.setBackgroundResource(e.mCategory.equals(mCurrentCategory) ? R.drawable.category_selection_rectangle : 0);

        return v;
    }

    @Override
    public int getItemViewType(int position) {
        return ((ListElement)getItem(position)).getLevel();
    }

    @Override
    public int getViewTypeCount() {
        int c = 0;
        for(final ListElement e : mCategories){
            c = Math.max(e.getLevel() + 1, c);
        }
        return c;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    public void setCurrentCategory(final String current_category){
        updateVisibility(current_category);
    }

    public String itemClicked(final long id){
        if(id == 0){
            mCurrentCategory = mAllEpisodesString;
            notifyChanges();
            return mAllEpisodesString;
        }
        final ListElement e = mCategories.get((int) id);
        if(e.isLeaf()){
            mCurrentCategory = e.mCategory;
            notifyChanges();
            return e.mCategory;
        }
        else{
            e.mExpanded = !e.mExpanded;
            notifyChanges();
            return null;
        }
    }

    private void notifyChanges(){
        for(DataSetObserver observer : mDataObservers){
            observer.onChanged();
        }
    }

    private void updateVisibility(final String current_category){
        mCurrentCategory = current_category;

        ListElement e = findCategoryByString(mCurrentCategory);
        if(e == null){
            return;
        }
        while(e.mParent != -1){
            e = mCategories.get(e.mParent);
            e.mExpanded = true;
        }

        notifyChanges();
    }

    private ListElement findCategoryByString(final String category){
        for(final ListElement e : mCategories){
            if(e.mCategory.equals(category)){
                return e;
            }
        }
        return null;
    }
}
