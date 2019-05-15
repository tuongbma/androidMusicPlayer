package com.ptit.android.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.ptit.android.Constants;
import com.ptit.android.MainActivity;
import com.ptit.android.MyAdapter.MyArrayAdapter;
import com.ptit.android.R;
import com.ptit.android.SongsManager;
import com.ptit.android.model.Song;

import java.io.File;
import java.util.ArrayList;

public class OfflineFragment extends ListFragment {
    private static final int REQUEST_ID_READ_PERMISSION = 100;
    private static final int REQUEST_ID_WRITE_PERMISSION = 200;
    // Songs list
    public ArrayList<Song> songsList = new ArrayList<>();
    private PlayMusicFragment playMusicFragment = new PlayMusicFragment();
    private Bundle bundle = new Bundle();
    private EditText edtSearch;
    private File[] listFiles;
    private SongsManager songsManager;
    private String txtSearch;
    private ListView lv;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.show_list_songs, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        edtSearch = view.findViewById(R.id.txtSearch);
        songsManager = new SongsManager();
        // get all songs from sdcard
        askReadPermission();
        askWritePermission();
        if (songsList.size() == 0) {
            toastMessage("Khong co bai hat nao");
        } else {
            // selecting single ListView item
            lv = getListView();
            MyArrayAdapter mayArr = new MyArrayAdapter(getActivity(), R.layout.list_row, songsList);
            lv.setAdapter(mayArr);
            // listening to single listitem click
            lv.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // getting listitem index
                    int songIndex = position;
                    txtSearch = edtSearch.getText().toString();

                    bundle.putInt("songIndex", songIndex);
                    bundle.putLong("MODE", Constants.MODE.OFFLINE);
                    bundle.putString("txtSearch", txtSearch);
                    bundle.putLong("typeSearch", Constants.SEARCH_TYPE.TITLE);

                    playMusicFragment = new PlayMusicFragment();
                    playMusicFragment.setArguments(bundle);

                    FragmentManager fragmentManager = MainActivity.fragmentManager;
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    MainActivity.playMusicFragment = playMusicFragment;
                    fragmentTransaction.replace(R.id.fragment_container, playMusicFragment, "playMusicFragment");
                    fragmentTransaction.commit();
                }
            });

            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    performSearch(edtSearch.getText().toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }
    }

    private void askReadPermission() {
        boolean canRead = this.askPermission(REQUEST_ID_READ_PERMISSION,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (canRead) {
//            this.songsList = songsManager.getOfflineList();
//            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBB");
        }
    }
    private void askWritePermission() {
        boolean canWrite = this.askPermission(REQUEST_ID_WRITE_PERMISSION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //
        if (canWrite) {
            this.songsList = songsManager.getOfflineList();
            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBB");
        }
    }

    private boolean askPermission(int requestId, String permissionName) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // Check if we have permission
            int permission = ActivityCompat.checkSelfPermission(getActivity(), permissionName);
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //
        // Note: If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0) {
            switch (requestCode) {
                case REQUEST_ID_READ_PERMISSION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getActivity(), "Read Permission Allowed!", Toast.LENGTH_SHORT).show();
                    }
                }
                case REQUEST_ID_WRITE_PERMISSION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getActivity(), "Write Permission Allowed!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            Toast.makeText(getActivity(), "Permission Cancelled!", Toast.LENGTH_SHORT).show();
        }
    }

    public void performSearch(String txtSearch) {
        ArrayList<Song> songSearch = new ArrayList<>();
        songsManager = new SongsManager();
        songSearch = songsManager.getSearchSongOffline(songsList, Constants.SEARCH_TYPE.TITLE, txtSearch);
        MyArrayAdapter mayArr = new MyArrayAdapter(getActivity(), R.layout.list_row, songSearch);
        lv.setAdapter(mayArr);
    }

    private void toastMessage(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

}
