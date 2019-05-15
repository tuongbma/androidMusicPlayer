package com.ptit.android;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ptit.android.model.Song;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SongsManager {

    private ArrayList<Song> songList = new ArrayList<>();
    private String DB_NAME = "songs";
    private FirebaseDatabase database;
    private static Long TITLE_SEARCH_TYPE = 1L;
    private static Long ARTST_SEARCH_TYPE = 2L;
    private MediaMetadataRetriever metaRetriver;
    private DatabaseReference myRef;


    // Constructor
    public SongsManager() {
        database = FirebaseDatabase.getInstance();
        myRef = getFireBaseReference();
    }

    public DatabaseReference getFireBaseReference() {
        return database.getReference(DB_NAME);
    }

    /**
     * Function to read all mp3 files from sdcard
     * and store the details in ArrayList
     */
    public ArrayList<Song> getOfflineList() {

        songList = new ArrayList<>();
        for (File file : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles(new FileExtensionFilter())) {
            String filePath = file.getAbsolutePath();
            Song bean = getInfoSongFromSource(Constants.MODE.OFFLINE, filePath);
            songList.add(bean);
        }
//        }
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
        if (text != null && !text.isEmpty()) {

//            Query query =  myRef.orderByChild("title").startAt(text.toUpperCase()).endAt(text.toLowerCase()+"\uf8ff");
//            query.addValueEventListener(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    for (DataSnapshot data : dataSnapshot.getChildren()) {
////                        String searchTxt = text.toLowerCase();
//                        Song s = data.getValue(Song.class);
//                        songList.add(s);
////                        String songTitle = s.getTitle();
////                        String songArtist = s.getArtist();
////                        if (TITLE_SEARCH_TYPE.equals(searchType)) {
////                            if (songTitle.toLowerCase().contains(searchTxt)) {
////                                songList.add(s);
////                            }
////                        } else if (ARTST_SEARCH_TYPE.equals(searchType)) {
////                            if (songArtist.contains(text.toUpperCase())) {
////                                // Adding each song to SongList
////                                songList.add(s);
////                            }
////                        }
//                    }
//                    myCallback.onCallback(songList);
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                }
//            });


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
            metaRetriver.setDataSource(source, new HashMap<String, String>());
            System.out.println("source" + source);
        } else {
            System.out.println("SOURCE: " + source);
            try (FileInputStream is = new FileInputStream(source)) {
                FileDescriptor fd = is.getFD();
            } catch (FileNotFoundException fileEx) {
                System.out.println("FILE NOT FOUND");
            } catch (IOException ioEx) {
                System.out.println("IO Exception");
            }
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
        sortList(lstResult);
        return lstResult;
    }

    public void sortList(ArrayList<Song> songList) {
        Comparator<Song> compareByTitle = new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        };
    }
}
