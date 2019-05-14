package com.ptit.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ptit.android.model.Song;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SongsManager {

    private ArrayList<Song> songList;
    private String DB_NAME = "songs";
    private FirebaseDatabase database;
    private static Long TITLE_SEARCH_TYPE = 1L;
    private static Long ARTST_SEARCH_TYPE = 2L;
    private MediaMetadataRetriever metaRetriver;

    // Constructor
    public SongsManager() {
        database = FirebaseDatabase.getInstance();
    }

    public DatabaseReference getFireBaseReference() {
        return database.getReference(DB_NAME);
    }

    /**
     * Function to read all mp3 files from sdcard
     * and store the details in ArrayList
     */
    public ArrayList<Song> getOfflineList() {
        // SDCard Path
        String MEDIA_PATH = new String( "/emulated/0/Download/");
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        System.out.println("URI"  + musicUri);
        System.out.println("EXTERNAL PATH: " + Environment.getExternalStorageDirectory());
        System.out.println(Environment.getExternalStorageDirectory().listFiles());
        System.out.println("MEDIA PATH" + MEDIA_PATH);
        songList = new ArrayList<>();
        File home = new File(MEDIA_PATH);
        try {
                for (File file : home.listFiles(new FileExtensionFilter())) {
                    String filePath = file.getAbsolutePath().replaceAll("\\s+", "");
                    Song bean = getInfoSongFromSource(Constants.MODE.OFFLINE, filePath);
                    songList.add(bean);
            }
        } catch (NullPointerException e) {

        }
        // return songs list array
        return songList;
    }

    public interface MyCallback {
        void onCallback(ArrayList<Song> value);
    }

    /**
     * ham search online
     *
     * @param text
     * @param searchType
     * @param myCallback
     */
    public void readData(final String text, final Long searchType, final MyCallback myCallback) {
        songList = new ArrayList<Song>();
        DatabaseReference myRef = getFireBaseReference();
        if (text != null && !text.isEmpty()) {
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        String searchTxt = text.toLowerCase();
                        Song s = data.getValue(Song.class);
                        String songTitle = s.getTitle();
                        String songArtist = s.getArtist();
                        if (TITLE_SEARCH_TYPE.equals(searchType)) {
                            if (songTitle.toLowerCase().contains(searchTxt)) {
                                songList.add(s);
                            }
                        } else if (ARTST_SEARCH_TYPE.equals(searchType)) {
                            if (songArtist.contains(text.toUpperCase())) {
                                // Adding each song to SongList
                                songList.add(s);
                            }
                        }
                    }
                    myCallback.onCallback(songList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    /**
     * Class to filter files which are having .mp3 extension
     */
    class FileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".mp3") || name.endsWith(".MP3"));
        }
    }

    /**
     * lay thong tin bai hat
     *
     * @param mode
     * @param source
     * @return
     */
    public Song getInfoSongFromSource(Long mode, String source) {
        Song song = new Song();
        metaRetriver = new MediaMetadataRetriever();
        if (Constants.MODE.ONLINE.equals(mode)) {
            source = Constants.STORE_FIREBASE_SERVER + source;
            metaRetriver.setDataSource(source, new HashMap<String, String>());
            System.out.println("source" + source);
        } else {
            metaRetriver.setDataSource(source);
        }

        byte[] art = metaRetriver.getEmbeddedPicture();
        Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
        song.setTitle(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        song.setArtist(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        String durationStr = formateMilliSeccond(Long.parseLong(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        song.setDuration(durationStr);
        song.setSongImage(songImage);
        song.setSource(source);
//		song.setSongId(id);
        return song;
//		songTitleLabel.setText(songTitle);
    }

    /**
     * Function to convert milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     */
    public static String formateMilliSeccond(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;
        // return timer string
        return finalTimerString;
    }

    /**
     * lay list search offline
     *
     * @param songList
     * @param searchType
     * @param txtSearch
     * @return
     */
    public ArrayList<Song> getSearchSongOffline(List<Song> songList, Long searchType, String txtSearch) {
        ArrayList<Song> lstResult = new ArrayList<>();
        for (Song s : songList) {
            String songTitle = s.getTitle();
            String songArtist = s.getArtist();
            if (TITLE_SEARCH_TYPE.equals(searchType)) {
                if (songTitle.toLowerCase().contains(txtSearch)) {
                    lstResult.add(s);
                }
            } else if (ARTST_SEARCH_TYPE.equals(searchType)) {
                if (songArtist.contains(txtSearch.toUpperCase())) {
                    lstResult.add(s);
                }
            } else {
                lstResult.add(s);
            }
        }
        return lstResult;
    }
}
