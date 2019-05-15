package com.ptit.android.Fragment;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ptit.android.Constants;
import com.ptit.android.MainActivity;
import com.ptit.android.R;
import com.ptit.android.ShakeListener;
import com.ptit.android.SongsManager;
import com.ptit.android.Utilities;
import com.ptit.android.model.Song;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class PlayMusicFragment extends Fragment implements OnCompletionListener, SeekBar.OnSeekBarChangeListener  {
    ImageButton btnSearch;
    EditText searchText;
    private ImageButton btnPlay;
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private ImageButton btnNext;
    private ImageButton btnPrevious;
    private ImageButton btnPlaylist;
    private ImageButton btnDownload;
    private ImageButton btnRepeat;
    private ImageButton btnShuffle;
    private SongsManager songManager;
    private SeekBar songProgressBar;
    private TextView songTitleLabel;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    private ImageView albumPic;
    private ImageButton btnLike;

    public String command = null;

    // Media Player
    private MediaPlayer mp;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();
    private Long mode;
    private Long typeSearch;
    private Utilities utils;
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds
    private int currentSongIndex = -1;
    private String textSearch;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private static final String SERVER_STORAGE = "https://firebasestorage.googleapis.com/v0/b/musicapplication-f21a5.appspot.com/o/";
    private ArrayList<HashMap<String, String>> songsListOffline = new ArrayList<HashMap<String, String>>();
    private ArrayList<HashMap<String, String>> songsListOnline = new ArrayList<HashMap<String, String>>();
    private ArrayList<Song> songsList = new ArrayList<Song>();
    private ShakeListener mShaker;
    private DownloadManager downloadManager;
    private AlertDialog.Builder builder;

    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.play_music, null);
//
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        btnPlay = (ImageButton) v.findViewById(R.id.btnPlay);
        btnForward = (ImageButton) v.findViewById(R.id.btnForward);
        btnBackward = (ImageButton) v.findViewById(R.id.btnBackward);
        btnNext = (ImageButton) v.findViewById(R.id.btnNext);
        btnPrevious = (ImageButton) v.findViewById(R.id.btnPrevious);
        btnPlaylist = (ImageButton) v.findViewById(R.id.btnPlaylist);
        btnDownload = (ImageButton) v.findViewById(R.id.btnDownload);
        btnRepeat = (ImageButton) v.findViewById(R.id.btnRepeat);
        btnShuffle = (ImageButton) v.findViewById(R.id.btnShuffle);
        songProgressBar = (SeekBar) v.findViewById(R.id.songProgressBar);
        songTitleLabel = (TextView) v.findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView) v.findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = (TextView) v.findViewById(R.id.songTotalDurationLabel);
        btnLike =(ImageButton) v.findViewById(R.id.btnLike);
        albumPic = v.findViewById(R.id.albumPic);
        Animation aniRotate = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate);
        albumPic.startAnimation(aniRotate);
        MainActivity.navigationView.setSelectedItemId(R.id.actionPlaying);
        // Mediaplayer
        mp = new MediaPlayer();
        songManager = new SongsManager();
        utils = new Utilities();

        // Listeners
        songProgressBar.setOnSeekBarChangeListener(this); // Important
        mp.setOnCompletionListener(this); // Important

        Bundle bundle = getArguments();
        try{
            mode = bundle.getLong("MODE");
            typeSearch = bundle.getLong("typeSearch");
            currentSongIndex = bundle.getInt("songIndex");
            textSearch = bundle.getString("txtSearch");

            System.out.println("song list online: " + songsList.size());
            if(typeSearch == 0) {
                typeSearch = Constants.SEARCH_TYPE.TITLE;
            }
            //kiem tra xem online hay offline
            if(Constants.MODE.ONLINE.equals(mode)) {

                songsList = (ArrayList<Song>) bundle.getSerializable("songListOnline");
                playSongOnline(currentSongIndex);
            } else {
                SongsManager songMng = new SongsManager();
                ArrayList<Song> totalSongOffline = songManager.getOfflineList();
                songsList = songMng.getSearchSongOffline(totalSongOffline, Constants.SEARCH_TYPE.TITLE, textSearch);
                System.out.println("current song " + currentSongIndex);
                playSongOffline(currentSongIndex);
            }

        } catch (NullPointerException e){
            Toast.makeText(getActivity(), "Không có bài hát nào được phát", Toast.LENGTH_SHORT).show();
        }

        if (currentSongIndex == -1){
            btnPlay.setEnabled(false);
            btnNext.setEnabled(false);
            btnPrevious.setEnabled(false);
            btnShuffle.setEnabled(false);
            btnRepeat.setEnabled(false);
            btnBackward.setEnabled(false);
            btnForward.setEnabled(false);
            btnBackward.setEnabled(false);
        } else{
            btnPlay.setEnabled(true);
            btnNext.setEnabled(true);
            btnPrevious.setEnabled(true);
            btnShuffle.setEnabled(true);
            btnRepeat.setEnabled(true);
            btnBackward.setEnabled(true);
            btnForward.setEnabled(true);
            btnBackward.setEnabled(true);
        }

        final Vibrator vibe = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        mShaker = new ShakeListener(getActivity());
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener () {
            public void onShake()
            {
                vibe.vibrate(100);
                if(Constants.MODE.ONLINE.equals(mode)) {
                    if (currentSongIndex < (songsList.size() - 1)) {
                        playSongOnline(currentSongIndex + 1);
                        currentSongIndex = currentSongIndex + 1;
                    } else {
                        // play first song
                        playSongOnline(0);
                        currentSongIndex = 0;
                    }
                } else {
                    if (currentSongIndex < (songsList.size() - 1)) {
                        playSongOffline(currentSongIndex + 1);
                        currentSongIndex = currentSongIndex + 1;
                    } else {
                        // play first song
                        playSongOffline(0);
                        currentSongIndex = 0;
                    }
                }
            }
        });

        /**
         * Play button click event
         * plays a song and changes button to pause image
         * pauses a song and changes button to play image
         * */
        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // check for already playing
                try{
                    if (mp.isPlaying()) {
                        if (mp != null) {
                            mp.pause();
                            // Changing button image to play button
                            btnPlay.setImageResource(R.drawable.btn_play);
                        }
                    } else {
                        // Resume song
                        if (mp != null) {
                            mp.start();
                            // Changing button image to pause button
                            btnPlay.setImageResource(R.drawable.btn_pause);
                        }
                    }
                } catch (Exception e){
                    Toast.makeText(getActivity(), "Không có bài hát để chạy",Toast.LENGTH_SHORT);
                }


            }
        });

        /**
         * Forward button click event
         * Forwards song specified seconds
         * */
        btnForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // get current song position
                int currentPosition = mp.getCurrentPosition();
                // check if seekForward time is lesser than song duration
                if (currentPosition + seekForwardTime <= mp.getDuration()) {
                    // forward song
                    mp.seekTo(currentPosition + seekForwardTime);
                } else {
                    // forward to end position
                    mp.seekTo(mp.getDuration());
                }
            }
        });

        /**
         * Backward button click event
         * Backward song to specified seconds
         * */
        btnBackward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // get current song position
                int currentPosition = mp.getCurrentPosition();
                // check if seekBackward time is greater than 0 sec
                if (currentPosition - seekBackwardTime >= 0) {
                    // forward song
                    mp.seekTo(currentPosition - seekBackwardTime);
                } else {
                    // backward to starting position
                    mp.seekTo(0);
                }

            }
        });

        /**
         * Next button click event
         * Plays next song by taking currentSongIndex + 1
         * */
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // check if next song is there or not
                if(Constants.MODE.ONLINE.equals(mode)) {
                    if (currentSongIndex < (songsList.size() - 1)) {
                        playSongOnline(currentSongIndex + 1);
                        currentSongIndex = currentSongIndex + 1;
                    } else {
                        // play first song
                        playSongOnline(0);
                        currentSongIndex = 0;
                    }
                } else {
                    if (currentSongIndex < (songsList.size() - 1)) {
                        playSongOffline(currentSongIndex + 1);
                        currentSongIndex = currentSongIndex + 1;
                    } else {
                        // play first song
                        playSongOffline(0);
                        currentSongIndex = 0;
                    }
                }
            }
        });

        /**
         * Back button click event
         * Plays previous song by currentSongIndex - 1
         * */
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if(Constants.MODE.ONLINE.equals(mode)) {
                    if (currentSongIndex > 0) {
                        playSongOnline(currentSongIndex - 1);
                        currentSongIndex = currentSongIndex - 1;
                    } else {
                        // play last song
                        playSongOnline(songsList.size() - 1);
                        currentSongIndex = songsList.size() - 1;
                    }
                } else {
                    if (currentSongIndex > 0) {
                        playSongOffline(currentSongIndex - 1);
                        currentSongIndex = currentSongIndex - 1;
                    } else {
                        // play last song
                        playSongOffline(songsList.size() - 1);
                        currentSongIndex = songsList.size() - 1;
                    }
                }
            }
        });

        /**
         * Button Click event for Repeat button
         * Enables repeat flag to true
         * */
        btnRepeat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (isRepeat) {
                    isRepeat = false;
                    Toast.makeText(getActivity(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
                    btnRepeat.setImageResource(R.drawable.btn_repeat);
                } else {
                    // make repeat to true
                    isRepeat = true;
                    Toast.makeText(getActivity(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isShuffle = false;
                    btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
                    btnShuffle.setImageResource(R.drawable.btn_shuffle);
                }
            }
        });

        /**
         * Button Click event for Shuffle button
         * Enables shuffle flag to true
         * */
        btnShuffle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (isShuffle) {
                    isShuffle = false;
                    Toast.makeText(getActivity(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
                    btnShuffle.setImageResource(R.drawable.btn_shuffle);
                } else {
                    // make repeat to true
                    isShuffle = true;
                    Toast.makeText(getActivity(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isRepeat = false;
                    btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
                    btnRepeat.setImageResource(R.drawable.btn_repeat);
                }
            }
        });
        if(Constants.MODE.ONLINE.equals(mode)) {
            btnDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    builder = new AlertDialog.Builder(getActivity());
                    Song song = songsList.get(currentSongIndex);
//                    builder.setTitle("Confirm dialog demo !");
                    builder.setMessage("Do you want to download " + song.getTitle() +"?");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            downloadFile();
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.show();
                }
            });
        }

        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//
            }
        });


//        /**
//         * Button Click event for Play list click event
//         * Launches list activity which displays list of songs
//         * */
//        btnPlaylist.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View arg0) {
//                Intent in;
//                if(Constants.MODE.OFFLINE.equals(mode)) {
//                    songsList = songsListOffline;
//                    in = new Intent(getActivity(), OnlineFragment.class);
//                    startActivityForResult(in, Constants.MODE.OFFLINE.intValue());
//                } else {
//                    in = new Intent(getActivity(), OfflineFragment.class);
//                    in.putExtra("txtSearch", textSearch);
//                    in.putExtra("typeSearch", typeSearch);
//                    startActivityForResult(in, Constants.MODE.ONLINE.intValue());
//                }
//
////				startActivity(i);
//            }
//        });
    }

    public void downloadFile() {
        Song song = songsList.get(currentSongIndex);
        File direct = new File(Environment.getExternalStorageDirectory()
                + "/Download");

        if (!direct.exists()) {
            direct.mkdirs();
        }
        downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(song.getSource());
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                | DownloadManager.Request.NETWORK_MOBILE);
        request.setTitle(song.getTitle());
        request.setDestinationInExternalPublicDir("/Download", song.getTitle() + ".mp3");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadManager.enqueue(request);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) { // play offline
            try{
                currentSongIndex = data.getExtras().getInt("songOfflineIndex");
                playSongOffline(currentSongIndex);
            } catch (NullPointerException ex){

            }
            // play selected song

        }
        if (requestCode == Constants.MODE.ONLINE.intValue()) { // play online
            try{
                textSearch = data.getExtras().getString("txtSearch");
                currentSongIndex = data.getExtras().getInt("songOnlineIndex");
                playSongOnline(currentSongIndex);
            } catch (NullPointerException ex){

            }
            // play selected song

        }
    }

    /**
     * Function to play a song
     *
     *
     * @param songIndex - index of song
     */
    public void playSongOffline(int songIndex) {
        // Play song
        try {
            mp.reset();
            String source = songsList.get(songIndex).getSource();
            System.out.println("soure song: " + source);
            mp.setDataSource(source);
            setInfoPlayingSong(source);
            mp.prepare();
            mp.start();

            // Changing Button Image to pause image
            btnPlay.setImageResource(R.drawable.btn_pause);

            // set Progress bar values
            songProgressBar.setProgress(0);
            songProgressBar.setMax(100);

            // Updating progress bar
            updateProgressBar();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void  playSongOnline(final int songIndex){
        // Play song
        try {
            mp.reset();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            String source = songsList.get(songIndex).getSource();
            setInfoPlayingSong(source);
            mp.setDataSource(source);
            mp.prepare();
            mp.start();
            btnPlay.setImageResource(R.drawable.btn_pause);
            // set Progress bar values
            songProgressBar.setProgress(0);
            songProgressBar.setMax(100);
//            songManager.readData(textSearch, typeSearch, new SongsManager.MyCallback() {
//                @Override
//                public void onCallback(ArrayList<Song> songList) {
//                    try {
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
            updateProgressBar();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);


    }

    public void setInfoPlayingSong(String source) {
        MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
        if(Constants.MODE.ONLINE.equals(mode)) {
            metaRetriver.setDataSource(source, new HashMap<String,String>());
        } else {
            metaRetriver.setDataSource(source);
        }

        byte[] art = metaRetriver.getEmbeddedPicture();
        Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
        albumPic.setImageBitmap(songImage);

        String songTitle = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        songTitleLabel.setText(songTitle);
    }

    /**
     * Background Runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            try{
                long totalDuration = mp.getDuration();
                long currentDuration = mp.getCurrentPosition();

                // Displaying Total Duration time
                songTotalDurationLabel.setText("" + utils.milliSecondsToTimer(totalDuration));
                // Displaying time completed playing
                songCurrentDurationLabel.setText("" + utils.milliSecondsToTimer(currentDuration));

                // Updating progress bar
                int progress = (int) (utils.getProgressPercentage(currentDuration, totalDuration));
                //Log.d("Progress", ""+progress);
                songProgressBar.setProgress(progress);

                // Running this thread after 100 milliseconds
                mHandler.postDelayed(this, 100);

                if (command != null){
                    System.out.println("CMD IN PLAY MUSIC: " + command);
                    switch (command){
                        case "pause":
                            if (mp.isPlaying()) {
                                if (mp != null) {
                                    mp.pause();
                                    // Changing button image to play button
                                    btnPlay.setImageResource(R.drawable.btn_play);
                                }
                            } else {
                                // Resume song
                                if (mp != null) {
                                    mp.start();
                                    // Changing button image to pause button
                                    btnPlay.setImageResource(R.drawable.btn_pause);
                                }
                            }
                            break;
                        case "next":
                            System.out.println("VO NEXT ROI");
                            if(Constants.MODE.ONLINE.equals(mode)) {
                                if (currentSongIndex < (songsList.size() - 1)) {
                                    playSongOnline(currentSongIndex + 1);
                                    currentSongIndex = currentSongIndex + 1;
                                } else {
                                    // play first song
                                    playSongOnline(0);
                                    currentSongIndex = 0;
                                }
                            } else {
                                if (currentSongIndex < (songsList.size() - 1)) {
                                    playSongOffline(currentSongIndex + 1);
                                    currentSongIndex = currentSongIndex + 1;
                                } else {
                                    // play first song
                                    playSongOffline(0);
                                    currentSongIndex = 0;
                                }
                            }
                            break;
                        case "play":
                                // Resume song
                                if (mp != null) {
                                    mp.start();
                                    // Changing button image to pause button
                                    btnPlay.setImageResource(R.drawable.btn_pause);
                                } else{
                                    playSongOffline(0);
                                }

                    }
                }
                command = null;
            } catch (Exception e){

            }
        }
    };

    /**
     *
     * */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    /**
     * When user starts moving the progress handler
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    /**
     * When user stops moving the progress hanlder
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mp.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mp.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    /**
     * On Song Playing completed
     * if repeat is ON play same song again
     * if shuffle is ON play random song
     */
    @Override
    public void onCompletion(MediaPlayer arg0) {

        // check for repeat is ON or OFF
        if (isRepeat) {
            // repeat is on play same song again
            playSongOffline(currentSongIndex);
        } else if (isShuffle) {
            // shuffle is on - play a random song
            Random rand = new Random();
            currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
            playSongOffline(currentSongIndex);
        } else {
            // no repeat or shuffle ON - play next song
            try{
                if (currentSongIndex < (songsList.size() - 1)) {
                    playSongOffline(currentSongIndex + 1);
                    currentSongIndex = currentSongIndex + 1;
                } else {
                    // play first song
                    playSongOffline(0);
                    currentSongIndex = 0;
                }
            } catch (Exception e){
                Toast.makeText(getActivity(), "Không có bài hát nào để chạy", Toast.LENGTH_SHORT);
            }

        }
    }


    @Override
    public void onResume()
    {
        mShaker.resume();
        super.onResume();
    }
    @Override
    public void onPause()
    {
        mShaker.pause();
        super.onPause();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mp.release();
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
