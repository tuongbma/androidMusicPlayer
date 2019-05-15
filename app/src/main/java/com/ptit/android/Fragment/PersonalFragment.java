package com.ptit.android.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ptit.android.MainActivity;
import com.ptit.android.R;
import com.ptit.android.authentication.LoginActivity;
import com.ptit.android.authentication.UserInfoActivity;
import com.ptit.android.model.User;


public class PersonalFragment extends Fragment {
    private Button btnSignOut;
    private TextView txtPhone, txtBirthday, txtName, txtEmail;
    private ProgressBar progressBar;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.user_info_activity, container, false);
        return v;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity.navigationView.setSelectedItemId(R.id.actionPersonal);
        txtPhone = (TextView) view.findViewById(R.id.txtPhone);
        txtBirthday = (TextView) view.findViewById(R.id.txtBirthday);
        txtName = (TextView) view.findViewById(R.id.txtName);
        txtEmail = (TextView) view.findViewById(R.id.txtEmail);
        btnSignOut =(Button) view.findViewById(R.id.btn_signout);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setTitle(getString(R.string.app_name));
//        setSupportActionBar(toolbar);

        //get firebase auth instance
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        //get firebase auth instance
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        //get current user
        DatabaseReference dbRef = database.getReference("Users");
        System.out.println("On Creeeeeeeeeeeeeeeeeeeeeeee");

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // user auth state is changed - user is null
            // launch login activity
//            startActivity(new Intent(UserInfoActivity.this, LoginActivity.class));
//            finish();
        } else {
            String uID = user.getUid();
            System.out.println("UIDDDDDD" + uID);
            dbRef.child(uID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    System.out.println("SNAP SHOT : " + dataSnapshot.toString());
                    User u = dataSnapshot.getValue(User.class);
                    System.out.println("uSSERRRR: " + u.getEmail());
                    txtPhone.setText(u.getPhone());
                    txtEmail.setText(u.getEmail());
                    txtBirthday.setText(u.getBirthday());
                    txtName.setText(u.getName());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Sign outttttttttttttttttt");
                auth.signOut();
                MainActivity.navigationView.setSelectedItemId(R.id.actionHome);
//                startActivity(new Intent(get, MainActivity.class));
            }
        });
    }

}
