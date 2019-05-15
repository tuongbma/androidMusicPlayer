package com.ptit.android.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ptit.android.MainActivity;
import com.ptit.android.R;
import com.ptit.android.model.Song;
import com.ptit.android.model.User;

public class UserInfoActivity extends AppCompatActivity {

    private Button btnSignOut;
    private TextView txtPhone, txtBirthday, txtName, txtEmail;
    private ProgressBar progressBar;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_info_activity);

        txtPhone = (TextView) findViewById(R.id.txtPhone);
        txtBirthday = (TextView) findViewById(R.id.txtBirthday);
        txtName = (TextView) findViewById(R.id.txtName);
        txtEmail = (TextView) findViewById(R.id.txtEmail);
        btnSignOut =(Button) findViewById(R.id.btn_signout);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setTitle(getString(R.string.app_name));
//        setSupportActionBar(toolbar);

        //get firebase auth instance
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        //get current user
        final DatabaseReference dbRef = database.getReference("Users");
        System.out.println("On Creeeeeeeeeeeeeeeeeeeeeeee");

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // user auth state is changed - user is null
            // launch login activity
            startActivity(new Intent(UserInfoActivity.this, LoginActivity.class));
            finish();
        } else {
            String uID = user.getUid();
            dbRef.child(uID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User u = dataSnapshot.getValue(User.class);
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
                startActivity(new Intent(UserInfoActivity.this, MainActivity.class));
            }
        });
    }


}
