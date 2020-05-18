package com.example.musicplayer;

import com.example.musicplayer.MusicService.MusicBinder;

import android.Manifest;
import android.app.Activity;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.widget.ListView;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;


import android.widget.MediaController.MediaPlayerControl;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity implements MediaPlayerControl {

    //song list
    private ArrayList<Song> songList;
    private ListView songView;
    //controller
    private MusicController controller;
    //activity and playback pause flags
    private boolean paused=false, playbackPaused=false;
    //binding
    private boolean musicBound=false;
    private MusicService musicSrv;
    private Intent playIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                return;
            }
        }
        getSongList();
        setController();

        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTittle().compareTo(b.getTittle());
            }
        });




        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

    }

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }



    public void getSongList(){
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri,null,null, null, null);

        if (musicCursor!= null && musicCursor.moveToFirst()){
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId,thisTitle,thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }
    @Override
    public boolean canPause(){
        return true;
    }

    @Override
    public boolean canSeekBackward(){
        return true;
    }

    @Override
    public boolean canSeekForward(){
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition(){
        if (musicSrv!=null && musicBound && musicSrv.ispng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public int getDuration(){
        if (musicSrv!=null && musicBound && musicSrv.ispng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public  boolean isPlaying(){
        if (musicSrv!=null && musicBound)
            return musicSrv.ispng();
        return false;
    }

    @Override
    public void pause(){
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos){
        musicSrv.seek(pos);
    }

    @Override
    public void start(){
        musicSrv.go();
    }


    private void setController(){
        controller = new MusicController(this);

        //mengatur tombol previous dan next
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) { playNext(); }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) { playPrev(); }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);

    }
    //play next
    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
    //play previous
    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_shuffle :
                musicSrv.setShuffle();
                break;
            case R.id.action_end :
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    //user song select
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    //connect to the service
        private ServiceConnection musicConnection = new ServiceConnection(){

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicBinder binder = (MusicBinder)service;
                //get service
                musicSrv = binder.getService();
                //pass list
                musicSrv.setList(songList);
                musicBound = true;
            }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

}
