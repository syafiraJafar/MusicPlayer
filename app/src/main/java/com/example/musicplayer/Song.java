package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class Song extends AppCompatActivity {
    private long id;
    private String tittle;
    private String artist;

    public long getID(){return id;}
    public  String getTittle(){return tittle;}
    public String getArtist(){return artist;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);
    }
    public Song(long songID, String songTitle, String songArtist){
        id = songID;
        tittle = songTitle;
        artist = songArtist;
    }
}
