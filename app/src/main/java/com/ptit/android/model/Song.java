package com.ptit.android.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String songId;
    private String title;
    private String artist;
    private String source;
    private String duration;
    private Bitmap songImage;
    private String genre;
    private String linkDownload;

    public Song() {
    }

    protected Song(Parcel in) {
        songId = in.readString();
        title = in.readString();
        artist = in.readString();
        source = in.readString();
        duration = in.readString();
        songImage = in.readParcelable(Bitmap.class.getClassLoader());
        genre = in.readString();
        linkDownload = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Bitmap getSongImage() {
        return songImage;
    }

    public void setSongImage(Bitmap songImage) {
        this.songImage = songImage;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.songId,
                this.title,
                this.artist,
        this.source, this.duration, String.valueOf(this.songImage), this.genre, this.linkDownload});
    }
}
