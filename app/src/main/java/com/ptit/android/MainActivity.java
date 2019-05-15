package com.ptit.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.ptit.android.Fragment.HomeFragment;
import com.ptit.android.Fragment.OfflineFragment;
import com.ptit.android.Fragment.OnlineFragment;
import com.ptit.android.Fragment.PersonalFragment;
import com.ptit.android.Fragment.PlayMusicFragment;
import com.ptit.android.authentication.LoginActivity;
import com.ptit.android.authentication.UserInfoActivity;
import com.ptit.android.speechrecognize.RecognizeCommands;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


public class MainActivity<recordingBufferLock> extends AppCompatActivity {
    private static final int REQUEST_ID_READ_PERMISSION = 100;
    private static final int REQUEST_ID_WRITE_PERMISSION = 200;

    private Button btnOffline;
    private ImageButton btnOnline;
    private ListView lvSong;
    private SongsManager songsManager = new SongsManager();
    private TextView lblSeachResult;
    private ArrayAdapter<String> adapter;

    private EditText edtSearch;
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 1000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    private static final long AVERAGE_WINDOW_DURATION_MS = 1000;
    private static final float DETECTION_THRESHOLD = 0.60f;
    private static final int SUPPRESSION_MS = 500;
    private static final int MINIMUM_COUNT = 4;
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
    private static final String LABEL_FILENAME = "file:///android_asset/labels_v2.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/conv_v25.tflite";


    // Working variables.
    short[] recordingBuffer = new short[RECORDING_LENGTH];
    int recordingOffset = 0;
    boolean shouldContinue = true;
    private Thread recordingThread;
    boolean shouldContinueRecognition = true;
    private Thread recognitionThread;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();

    private List<String> labels = new ArrayList<String>();
    private List<String> displayedLabels = new ArrayList<>();
    private RecognizeCommands recognizeCommands = null;
    private Interpreter tfLite;
    private String txtSearch;
    public static Fragment onlineFragment = new OnlineFragment();
    public static Fragment offlineFragment = new OfflineFragment();
    public static Fragment playMusicFragment = new PlayMusicFragment();
    public static Fragment homeFragment = new HomeFragment();
    public static Fragment personalFragment = new PersonalFragment();
    public static FragmentManager fragmentManager;
    public static BottomNavigationView navigationView;
    // UI elements.
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private FirebaseAuth auth;

    /**
     * Memory-map the model file in Assets.
     */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        navigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigationView.setOnNavigationItemSelectedListener(navListener);
        lblSeachResult = findViewById(R.id.lblSearchResult);
        fragmentManager = getSupportFragmentManager();
        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            System.out.println("Emaillllllllllll" + auth.getCurrentUser().getEmail());
            if (!checkIfFragmentExisted("personalFragment")) {
                loadFragment(personalFragment, "personalFragment");
            }
        } else {
            if (!checkIfFragmentExisted("homeFragment")) {
                loadFragment(homeFragment, "homeFragment");
            }
        }

        btnOnline = (ImageButton) findViewById(R.id.btnSearch);
//        btnOffline = (Button) findViewById(R.id.btnOffline);
        edtSearch = (EditText) findViewById(R.id.txtSearch);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        Log.i(LOG_TAG, "Reading labels from: " + actualLabelFilename);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
                if (line.charAt(0) != '_') {
                    displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
                }
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!", e);
        }

        // Set up an object to smooth recognition results to increase accuracy.
        recognizeCommands =
                new RecognizeCommands(
                        labels,
                        AVERAGE_WINDOW_DURATION_MS,
                        DETECTION_THRESHOLD,
                        SUPPRESSION_MS,
                        MINIMUM_COUNT,
                        MINIMUM_TIME_BETWEEN_SAMPLES_MS);

        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            tfLite = new Interpreter(loadModelFile(getAssets(), actualModelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tfLite.resizeInput(0, new int[]{RECORDING_LENGTH, 1});

        // Start the recording and recognition threads.
//        requestMicrophonePermission();
//        startRecording();
//        startRecognition();
    }


    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    private boolean askPermission(int requestId, String permissionName) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // Check if we have permission
            int permission = ActivityCompat.checkSelfPermission(this, permissionName);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{permissionName},
                        requestId
                );
                return false;
            }
        }
        return true;
    }

    private boolean askReadPermission() {
        boolean canRead = this.askPermission(REQUEST_ID_READ_PERMISSION,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        return canRead;
    }

    private boolean askWritePermission() {
        boolean canWrite = this.askPermission(REQUEST_ID_WRITE_PERMISSION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return canWrite;
    }

    private boolean askRecordPermission() {
        boolean canRecord = this.askPermission(REQUEST_RECORD_AUDIO,
                Manifest.permission.RECORD_AUDIO);
        return canRecord;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults.length > 0) {
            switch (requestCode) {
                case REQUEST_ID_READ_PERMISSION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    }
                }
                case REQUEST_ID_WRITE_PERMISSION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    }
                }
                case REQUEST_RECORD_AUDIO: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        startRecording();
                        startRecognition();
                    }
                }
            }
        } else {
            Toast.makeText(this, "Permission Cancelled!", Toast.LENGTH_SHORT).show();
        }
    }

    public synchronized void startRecording() {
        if (recordingThread != null) {
            return;
        }
        shouldContinue = true;
        recordingThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                record();
                            }
                        });
        recordingThread.start();
    }

    public synchronized void stopRecording() {
        if (recordingThread == null) {
            return;
        }
        shouldContinue = false;
        recordingThread = null;
    }

    private void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Estimate the buffer size we'll need for this device.
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }

        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {
            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = recordingBuffer.length;
            int newRecordingOffset = recordingOffset + numberRead;
            int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            int firstCopyLength = numberRead - secondCopyLength;
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock();
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
                recordingBufferLock.unlock();
            }
        }

        record.stop();
        record.release();
    }

    public synchronized void startRecognition() {
        if (recognitionThread != null) {
            return;
        }
        shouldContinueRecognition = true;
        recognitionThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                recognize();
                            }
                        });
        recognitionThread.start();
    }

    public synchronized void stopRecognition() {
        if (recognitionThread == null) {
            return;
        }
        shouldContinueRecognition = false;
        recognitionThread = null;
    }

    private void recognize() {

        Log.v(LOG_TAG, "Start recognition");

        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[][] floatInputBuffer = new float[RECORDING_LENGTH][1];
        float[][] outputScores = new float[1][labels.size()];
        int[] sampleRateList = new int[]{SAMPLE_RATE};

        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            long startTime = new Date().getTime();
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock();
            try {
                int maxLength = recordingBuffer.length;
                int firstCopyLength = maxLength - recordingOffset;
                int secondCopyLength = recordingOffset;
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
            } finally {
                recordingBufferLock.unlock();
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (int i = 0; i < RECORDING_LENGTH; ++i) {
                floatInputBuffer[i][0] = inputBuffer[i] / 32767.0f;
            }

            Object[] inputArray = {floatInputBuffer};
            Map<Integer, Object> outputMap = new HashMap<>();
            outputMap.put(0, outputScores);

            // Run the model.
            tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

            // Use the smoother to figure out if we've had a real recognition event.
            long currentTime = System.currentTimeMillis();
            final RecognizeCommands.RecognitionResult result =
                    recognizeCommands.processLatestResults(outputScores[0], currentTime);

            Log.d("LISTENING", result.foundCommand + " - " + result.score + " - " + result.isNewCommand);
            if (result.foundCommand.equals("kiki") && result.isNewCommand) {
                Log.d("LISTENING ", "ACTIVATEEEEEEEEEEEEEEEEEEE");
//                try {
//                    // We don't need to run too frequently, so snooze for a bit.
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    // Ignore
//                }
                stopRecording();
                stopRecognition();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, 10);
                } else {
                    Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
                }
            }
            try {
                // We don't need to run too frequently, so snooze for a bit.
                Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        Log.v(LOG_TAG, "End recognition");
    }

    private boolean loadFragment(Fragment fragment, String fragmentTag) {
        if (fragment != null) {
            fragmentManager.beginTransaction().add(R.id.fragment_container, fragment, fragmentTag).commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d("LISTENING ", result.get(0));
                    String txtSearch = result.get(0).toLowerCase();
                    Bundle bundle = new Bundle();
                    if (txtSearch != null && !txtSearch.isEmpty()) {
                        if (txtSearch.contains("pause")) {
                            ((PlayMusicFragment) playMusicFragment).setCommand("pause");
                        } else if (txtSearch.contains("next")) {
                            ((PlayMusicFragment) playMusicFragment).setCommand("next");
                        } else if (txtSearch.contains("play")) {
                            ((PlayMusicFragment) playMusicFragment).setCommand("play");
                        } else {
                            bundle.putString("txtSearch", txtSearch);
                            OfflineFragment offlineFragment = new OfflineFragment();
                            offlineFragment.setArguments(bundle);
                            FragmentManager fragmentManager = MainActivity.fragmentManager;
                            System.out.println(fragmentManager.getFragments().toString());
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            MainActivity.offlineFragment = offlineFragment;
                            fragmentTransaction.replace(R.id.fragment_container, offlineFragment, "offlineFragment");
                            fragmentTransaction.commit();
                        }
                    }
//                    textView.setText(result.get(0));
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                requestMicrophonePermission();
                startRecording();
                startRecognition();
                break;
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Fragment fragment = null;
                    if (offlineFragment != null && onlineFragment != null && playMusicFragment != null) {
                        switch (menuItem.getItemId()) {
                            case R.id.actionHome:
                                if (!checkIfFragmentExisted("homeFragment")) {
                                    loadFragment(homeFragment, "homeFragment");
                                }
                                showHideFragment(homeFragment, personalFragment, onlineFragment, offlineFragment, playMusicFragment);
                                break;
                            case R.id.actionOnline:
                                if (!checkIfFragmentExisted("onlineFragment")) {
                                    loadFragment(onlineFragment, "onlineFragment");
                                }
                                showHideFragment(onlineFragment, offlineFragment, playMusicFragment, homeFragment, personalFragment);
                                break;
                            case R.id.actionOffline:
                                if (!checkIfFragmentExisted("offlineFragment")) {
                                    loadFragment(offlineFragment, "offlineFragment");
                                }
                                showHideFragment(offlineFragment, onlineFragment, playMusicFragment, homeFragment, personalFragment);
                                break;
                            case R.id.actionPlaying:
                                if (!checkIfFragmentExisted("playMusicFragment")) {
                                    loadFragment(playMusicFragment, "playMusicFragment");
                                }
                                showHideFragment(playMusicFragment, onlineFragment, offlineFragment, homeFragment, personalFragment);
                                break;
                            case R.id.actionPersonal:
                                if (auth.getCurrentUser() != null) {
                                    System.out.println("DA LOGINNNNNNNNNNNNNNNN");
                                    if (!checkIfFragmentExisted("personalFragment")) {
                                        loadFragment(personalFragment, "personalFragment");
                                    }
                                    showHideFragment(personalFragment, homeFragment, onlineFragment, offlineFragment, playMusicFragment);
                                } else {
                                    System.out.println("CHUA LOGINNNNNNNNNNNNNNNN");
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();
                                }
                                break;
                        }
                        return true;
                    }
                    return false;
                }
            };

    public void showHideFragment(Fragment fragment1, Fragment fragment2, Fragment fragment3, Fragment fragment4, Fragment fragment5) {
        if (fragment1.isHidden()) {
            fragmentManager.beginTransaction()
                    .show(fragment1)
                    .commit();
        }
        fragmentManager.beginTransaction()
                .hide(fragment2)
                .hide(fragment3)
                .hide(fragment4)
                .hide(fragment5)
                .commit();
    }

    public boolean checkIfFragmentExisted(String fragmentTag) {
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
        if (fragment == null) {
            return false;
        } else {
            return true;
        }
    }

    public BottomNavigationView getNavigationView() {
        return navigationView;
    }

    public void setNavigationView(BottomNavigationView navigationView) {
        this.navigationView = navigationView;
    }
}
