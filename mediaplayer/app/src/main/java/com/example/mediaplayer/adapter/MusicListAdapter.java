package com.example.mediaplayer.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayer.MainActivity;
import com.example.mediaplayer.R;
import com.example.mediaplayer.Utils.MusicUtils;
import com.example.mediaplayer.model.Song;

import java.util.List;


public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.RecyclerHolder> {
    private List<Song> dataList = null;
    private ItemClickListener mItemClickListener;



    public interface ItemClickListener{
        public void onItemClick(int position);
    }
    public void setOnItemClickListener(ItemClickListener itemClickListener){
        this.mItemClickListener = itemClickListener;
    }
    public void setData(List<Song> dataList){
        this.dataList = dataList;
        notifyDataSetChanged();

    }
    @NonNull
    @Override
    public MusicListAdapter.RecyclerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_rv_list,parent,false);
        return new RecyclerHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicListAdapter.RecyclerHolder holder, @SuppressLint("RecyclerView") int position) {
        Song song = dataList.get(position);
        int duration = song.duration;
        String time = MusicUtils.formatTime(duration);
        holder.tvDurationTime.setText(time);
        holder.tvSongName.setText(song.getSong().trim());
        holder.tvSinger.setText(song.getSinger()+" - "+song.getAlbum());
        holder.tvPosition.setText(holder.getAdapterPosition()+1+"");

        //点击事件一般写在绑定数据这里
        if (mItemClickListener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mItemClickListener.onItemClick(position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (dataList == null){
            return 0;
        }
       return dataList.size();
    }

    class RecyclerHolder extends RecyclerView.ViewHolder{
        TextView tvSongName;
        TextView tvSinger;
        TextView tvDurationTime;
        TextView tvPosition;
        LinearLayout itemMusic;
        public RecyclerHolder(@NonNull View itemView) {
            super(itemView);
            itemMusic = itemView.findViewById(R.id.item_music);
            tvSongName = itemView.findViewById(R.id.tv_song_name);
            tvSinger = itemView.findViewById(R.id.tv_singer);
            tvDurationTime = itemView.findViewById(R.id.tv_duration_time);
            tvPosition = itemView.findViewById(R.id.tv_position);
        }
    }
}
