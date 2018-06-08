package com.example.stephen.movietime;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

//TODO (3) extend RV View Holder (implement onCreate, onBind, and getItemCount)
public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    //TODO (4b) Create ItemClickListener
    private ItemClickListener mClickListener;

    //TODO (2) get data from constructor
    private List<String> mData;
    private final LayoutInflater mInflater;
    private final Context mContext;
    MyRecyclerViewAdapter(Context context, List<String> data){
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.mContext = context;
    }

    //TODO (5) inflate
    //inflates the cell layout from recyclerview_item.xml
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new ViewHolder(view);
    }
    //binds each poster to the cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //get url for poster
        String url_suffix = mData.get(position);
        String url_string = "http://image.tmdb.org/t/p/w"+ViewUtils.get_width() + "/" + url_suffix;
        //set the image view using Picasso
        Picasso.with(mContext).load(url_string).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    //TODO (1) create ViewHolder class - implement OnClickListener
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        final ImageView imageView;

        //set the click listener to the image view
        //when a user clicks on an image --> something happens based on which image a user clicked on
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