package com.example.stephen.movietime;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ViewUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.stephen.movietime.data.Contract;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

//TODO (3) extend RV View Holder (implement onCreate, onBind, and getItemCount)
public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    //TODO (4b) Create ItemClickListener
    private ItemClickListener mClickListener;

    //TODO (2) get data from constructor
    private List<String> mData;
    private final LayoutInflater mInflater;
    private final Context mContext;

    // Create MyRecyclerViewAdapter
    MyRecyclerViewAdapter(Context context){
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
    }

    //TODO (5) inflate
    // Inflates the cell layout from recyclerview_item.xml
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new ViewHolder(view);
    }
    // Binds each poster to the cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //get url for poster
        String url_suffix = mData.get(position);
        String url_string = "http://image.tmdb.org/t/p/w342/" + url_suffix;
        //set the image view using Picasso
        Picasso.with(mContext).load(url_string).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // TODO (6) create swap cursor method to reset the data
    void swapCursor(Cursor data) {
        // Move through the cursor and extract the movie poster urls.
        List<String> movie_posters = new ArrayList<>();
        for (int i = 0; i < data.getCount(); i++) {
            data.moveToPosition(i);
            movie_posters.add(data.getString(data.getColumnIndex(
                    Contract.listEntry.COLUMN_MOVIE_POSTER_PATH)));
        }
        mData = movie_posters;
        notifyDataSetChanged();
    }

    //TODO (1) create ViewHolder class - implement OnClickListener
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        final ImageView imageView;

        // Set the click listener to the image view
        // When a user clicks on an image --> something happens based on the image a user clicked.
        ViewHolder(View itemView){
            super(itemView);
            imageView = itemView.findViewById(R.id.movie_poster_image_view);
            itemView.setOnClickListener(this);
        }

        //TODO (4c) 'wire' into ViewHolder
        @Override
        public void onClick(View view){
            mClickListener.onItemClick(getAdapterPosition());
        }
    }

    //TODO (4a) Create interface for the click listener
    public interface ItemClickListener{
        void onItemClick(int position);
    }
    void setClickListener(ItemClickListener itemClickListener){
        this.mClickListener = itemClickListener;
    }
}