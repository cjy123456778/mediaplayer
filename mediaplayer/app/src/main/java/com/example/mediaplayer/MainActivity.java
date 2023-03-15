package com.example.mediaplayer;

import static com.example.mediaplayer.Utils.DateUtil.parseTime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.example.mediaplayer.Utils.Constant;
import com.example.mediaplayer.Utils.MusicUtils;
import com.example.mediaplayer.Utils.ObjectUtils;
import com.example.mediaplayer.Utils.SPUtils;
import com.example.mediaplayer.adapter.MusicListAdapter;
import com.example.mediaplayer.model.Song;
import com.tbruyelle.rxpermissions3.RxPermissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    private RecyclerView rvMusic;
    private Button btnScan;
    private LinearLayout scanLay;
    private TextView tvClearList;
    private TextView tvTitle;
    private Toolbar toolbar;
    private TextView tvPlayTime;
    private SeekBar timeSeekBar;
    private TextView tvTotalTime;
    private ImageView btnPreVious;
    private ImageView btnPlayOrPause;
    private ImageView btnNext;
    private TextView tvPlaySongInfo;
    private ImageView playStateImg;
    private LinearLayout playStateLay;
    private MusicListAdapter mAdapter;//歌曲适配器
    private List<Song> mList;//歌曲列表
    private RxPermissions rxPermissions;//权限请求
    private int mCurrentPosition;
    private String musicData = null;
    private MediaPlayer mediaPlayer;//音频播放器
    private  static final int INTERNAL_TIME = 1000;//音乐进度条间隔时间
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            //展示给进度条和当前时间
            int progress = mediaPlayer.getCurrentPosition();
            timeSeekBar.setProgress(progress);
            tvPlayTime.setText(parseTime(progress));
            //继续定时发送数据
            updateProgress();
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_AUDIO
        },100);
        rvMusic = findViewById(R.id.rv_music);
        btnScan = findViewById(R.id.btn_scan);
        scanLay = findViewById(R.id.scan_lay);
        tvClearList = findViewById(R.id.tv_clear_list);
        tvTitle = findViewById(R.id.tv_title);
        toolbar = findViewById(R.id.toolbar);
        tvPlayTime = findViewById(R.id.tv_play_time);
        timeSeekBar = findViewById(R.id.time_seekBar);
        tvTotalTime = findViewById(R.id.tv_total_time);
        btnPreVious = findViewById(R.id.btn_previous);
        btnPlayOrPause = findViewById(R.id.btn_play_or_pause);
        btnNext = findViewById(R.id.btn_next);
        tvPlaySongInfo = findViewById(R.id.tv_play_song_info);
        playStateImg = findViewById(R.id.play_state_img);
        playStateLay = findViewById(R.id.play_state_lay);
        rxPermissions = new RxPermissions(this);//使用前先实例化

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permissionRequest();
            }
        });

        musicData = SPUtils.getString(Constant.MUSIC_DATA_FIRST,"yes",this);

        if (musicData.equals("no")){
            //说明是第一次打开app
            scanLay.setVisibility(View.GONE);
            initMusic();
            Log.d("cjy", "onCreate: 第一次打开app");
        }else {
            scanLay.setVisibility(View.VISIBLE);
        }
    }
    private void permissionRequest() {//使用这个框架需要制定JDK版本，建议用1.8
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            initMusic();
        }
    }

    private void initMusic() {
        mList = new ArrayList<>();//实例化

        //数据赋值
        mList = MusicUtils.getMusicData(this);//将扫描到的音乐赋值给音乐列表

        if (!ObjectUtils.isEmpty(mList) && mList != null){
            scanLay.setVisibility(View.GONE);
            SPUtils.putString(Constant.MUSIC_DATA_FIRST,"no",this);
            Log.d("cjy", "initMusic: ");
        }
        Log.d("cjy", "initMusic: out");
        mAdapter = new MusicListAdapter();//指定适配器的布局和数据源
        mAdapter.setData(mList);
        //线性布局管理器，可以设置横向还是纵向，RecyclerView默认是纵向的，所以不用处理,如果不需要设置方向，代码还可以更加的精简如下
        rvMusic.setLayoutManager(new LinearLayoutManager(this));
        //如果需要设置方向显示，则将下面代码注释去掉即可
//        LinearLayoutManager manager = new LinearLayoutManager(this);
//        manager.setOrientation(RecyclerView.HORIZONTAL);
//        rvMusic.setLayoutManager(manager);

        //设置适配器
        rvMusic.setAdapter(mAdapter);

        //item的点击事件
        mAdapter.setOnItemClickListener(new MusicListAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mCurrentPosition = position;
                changeMusic(mCurrentPosition);
            }
        });
    }

    private void changeMusic(int position) {
        Log.d("cjy", "changeMusic: position" + position);
        if (position < 0){
            mCurrentPosition = position = mList.size() - 1;
            Log.d("cjy", "changeMusic: mList.size()"+mList.size());
        }else if (position > mList.size() - 1){
            mCurrentPosition = position = 0;
        }
        Log.d("cjy", "changeMusic: position" + position);
        if (mediaPlayer == null){
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);//监听音乐播放完毕，自动播放下一首
        }
        try {
            //释放掉之前的资源
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mList.get(position).path);
            tvPlaySongInfo.setText("歌名："+mList.get(position).song +
                    "歌手："+mList.get(position).singer);
            tvPlaySongInfo.setSelected(true); //跑马灯效果
            playStateLay.setVisibility(View.VISIBLE);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //切歌时重置进度条并展示歌曲时长
        timeSeekBar.setProgress(0);
        timeSeekBar.setMax(mediaPlayer.getDuration());
        tvTotalTime.setText(parseTime(mediaPlayer.getDuration()));
        
        updateProgress();
        if (mediaPlayer.isPlaying()){
            btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_play));
            playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_pause_state));
        }else {
            btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_pause));
            playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_play_state));

        }
    }

    private void updateProgress() {
        //使用Handle每间隔1s发送一次空消息，通知进度条更新
        Message msg = Message.obtain();//获取一个现成的消息
        //
        int progress = mediaPlayer.getCurrentPosition();
        msg.arg1 = progress;
        mHandler.sendMessageDelayed(msg,INTERNAL_TIME);
    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        changeMusic(++mCurrentPosition);
    }
}