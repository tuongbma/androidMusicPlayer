package com.ptit.android.MyAdapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ptit.android.R;
import com.ptit.android.model.Song;

import java.util.ArrayList;

public class MyArrayAdapter extends ArrayAdapter<Song> {
    Song song;
    Activity context = null;
    int layoutId;
    ArrayList<Song> arr = null;

    public MyArrayAdapter(Activity context, int layoutId, ArrayList<Song> list) {
        super(context, layoutId, list);
        this.context = context;
        this.layoutId = layoutId;
        this.arr = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        /*
         position: là vị trí của bàu hát trong list
         convertView: dùng để lấy về các control của mỗi item
         parent: chính là datasource được truyền vào từ MainActivity
         */
        if(convertView==null){
            LayoutInflater inflater = context.getLayoutInflater();
            convertView = inflater.inflate(layoutId, null);
        }
        //Lấy về bài hát ở vị trí được yêu cầu
        Song song = arr.get(position);
        //Lấy ra những control được định nghĩa trong cấu trúc của mỗi item
        ImageView icon = (ImageView) convertView.findViewById(R.id.list_image);
        TextView title = (TextView )convertView.findViewById(R.id.title);
        TextView singer = (TextView) convertView.findViewById(R.id.artist);
        TextView duration = (TextView )convertView.findViewById(R.id.duration);

        //Gán giá trị cho những control đó
        title.setText(song.getTitle());
        singer.setText(song.getArtist());

        duration.setText(song.getDuration());

        //Vì icon là ảnh nên ta phải lấy ra đường dẫn, dùng nó để lấy về image trong folder drawable
//        String uri_icon = "drawable/tuanhung";
//        int ImageResoure = convertView.getContext().getResources().getIdentifier(uri_icon, null, convertView.getContext().getApplicationContext().getPackageName());
//        Drawable image = convertView.getContext().getResources().getDrawable(ImageResoure);
//        icon.setImageDrawable(image);
        icon.setImageBitmap(song.getSongImage());
//        icon.setImageDrawable(drawableFromUrl("url"));
//        public Drawable drawableFromUrl(String url) throws IOException {
//            Bitmap x;
//            HttpURLConnection connection = (HttpURLConnection) new URL(url)
//                    .openConnection();
//            connection.connect();
//            InputStream input = connection.getInputStream();
//            x = BitmapFactory.decodeStream(input);
//            return new BitmapDrawable(x);
//        }

        return convertView;
    }

    public ArrayList<Song> getArr() {
        return arr;
    }

    public void setArr(ArrayList<Song> arr) {
        this.arr = arr;
    }
}
