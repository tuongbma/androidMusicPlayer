package com.ptit.android.Fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.ptit.android.Constants;
import com.ptit.android.MainActivity;
import com.ptit.android.MyAdapter.MyArrayAdapter;
import com.ptit.android.R;
import com.ptit.android.SongsManager;
import com.ptit.android.model.Song;

import java.util.ArrayList;
import java.util.HashMap;

public class OnlineFragment extends ListFragment {
    private String TAG = "FIREBASE";
    private static String STORE_FIREBASE_SERVER = "https://firebasestorage.googleapis.com/v0/b/musicapplication-f21a5.appspot.com/o/";
    private Button btnSearch;
    private ListView lvSong;
    private EditText edtSearch;
    private ArrayAdapter<String> adapter;
    private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private int currentSongIndex = 0;
    private String txtSearch;
    private Long typeSearch;
    private PlayMusicFragment playMusicFragment = new PlayMusicFragment();
    private Bundle bundle = new Bundle();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.online_activity, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        edtSearch = view.findViewById(R.id.txtSearch);

        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        lvSong = getListView();
        if (typeSearch == null) {
            typeSearch = Constants.SEARCH_TYPE.TITLE;
        }
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

        lvSong.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // getting listitem index
                int songIndex = position;
                // Starting new intent
                txtSearch = edtSearch.getText().toString();
                bundle.putInt("songIndex", songIndex);
                bundle.putString("txtSearch", txtSearch);
                bundle.putLong("MODE", Constants.MODE.ONLINE);
                bundle.putLong("typeSearch", Constants.SEARCH_TYPE.TITLE);
                playMusicFragment = new PlayMusicFragment();
                playMusicFragment.setArguments(bundle);
                FragmentManager fragmentManager = MainActivity.fragmentManager;
                System.out.println(fragmentManager.getFragments().toString());
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                MainActivity.playMusicFragment = playMusicFragment;
                fragmentTransaction.remove(fragmentManager.findFragmentByTag("playMusicFragment"));
                fragmentTransaction.add(R.id.fragment_container, playMusicFragment, "playMusicFragment");
                fragmentTransaction.commit();
            }
        });
    }

    public void performSearch(String txtSearch) {
        final SongsManager songsManager = new SongsManager();
            songsManager.readData(txtSearch, typeSearch, new SongsManager.MyCallback() {
                @Override
                public void onCallback(ArrayList<Song> songList) {
                    System.out.println("size songlist:" + songList.size());
                    ArrayList<Song> songLst = new ArrayList<>();
                    for (Song song : songList) {
                        Song songBean = songsManager.getInfoSongFromSource(Constants.MODE.ONLINE, song.getSource());
                        songLst.add(songBean);
                    }
                    MyArrayAdapter mayArr = new MyArrayAdapter(getActivity(), R.layout.list_row, songLst);
                    lvSong.setAdapter(mayArr);
                }
            });
        }

}
