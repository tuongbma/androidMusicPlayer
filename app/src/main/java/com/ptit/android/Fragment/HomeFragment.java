package com.ptit.android.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;

import com.ptit.android.MainActivity;
import com.ptit.android.R;
import com.ptit.android.SongsManager;
import com.ptit.android.model.Song;

import java.util.ArrayList;


public class HomeFragment extends Fragment {
    private Integer[] images = {R.drawable.kill_this_love,R.drawable.du_du,R.drawable.reputation,
           };
    private ImageView imageview;
    private SongsManager songsManager;
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.home, container, false);
        return v;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity.navigationView.setSelectedItemId(R.id.actionHome);

        Gallery imgGallery = (Gallery) view.findViewById(R.id.gallery);

        imgGallery.setAdapter(new ImageAdapter(getContext()));
        imgGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
                Toast.makeText(getContext(), "Image " + arg2,Toast.LENGTH_SHORT).show();
            }
        });
    }



    public class ImageAdapter extends BaseAdapter {
        private Context context;
        int imageBackground;

        public ImageAdapter(Context context) {

            this.context = context;
        }

        @Override
        public int getCount() {

            return images.length;
        }

        @Override
        public Object getItem(int arg0) {

            return arg0;
        }

        @Override
        public long getItemId(int arg0) {

            return arg0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {

            ImageView imageView = new ImageView(context);
            imageView.setImageResource(images[arg0]);
            return imageView;
        }
    }
}
