package com.example.android.miwok;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class WordAdapter extends ArrayAdapter<Word> {
    private int mColorResourceId;
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate( R.layout.list_item, parent, false);
        }

        // Get the item from the WordAdapter of the given position in listView
        Word currentWord = getItem(position);

        // Set the Wiwok text
        TextView miwok_text_view = listItemView.findViewById(R.id.miwok_text_view);
        miwok_text_view.setText(currentWord.getMiwokTranslation());

        // Set the default text(english here)
        TextView default_text_view = listItemView.findViewById(R.id.default_text_view);
        default_text_view.setText(currentWord.getDefaultTranslation());

        // Assign the icon for the imageView but only when the image is available else don't show the image view.
        ImageView icon_imageView = listItemView.findViewById(R.id.icon_image_view);
        if(currentWord.hasImage()){
            icon_imageView.setImageResource(currentWord.getImageResourceID());
            // If the view gets recycled, in our case it won't matter because all items in Phrases list are without image.
            icon_imageView.setVisibility(View.VISIBLE);
        } else {
            icon_imageView.setVisibility(View.GONE);
        }

        // Set the background color of both the textViews to supplied color.
        LinearLayout linearLayout = listItemView.findViewById(R.id.text_and_play_container);
        // Find the color resource id maps to
        int color = ContextCompat.getColor(getContext(), mColorResourceId);
        linearLayout.setBackgroundColor(color);

        return listItemView;
    }
    public WordAdapter(Activity context, ArrayList<Word> words, int colorResourceId){
        super(context, 0, words);
        mColorResourceId = colorResourceId;
    }
}
