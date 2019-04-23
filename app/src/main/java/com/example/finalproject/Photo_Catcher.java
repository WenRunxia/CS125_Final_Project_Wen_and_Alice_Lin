package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

public class Photo_Catcher {
    public final class photo_catcher extends AppCompatActivity {
        private static final String TAG = "photo catcher";
        private ImageView menu;
        private ImageButton camera;
        private Button reset;
        private Button start;
        protected void onCreate(final Bundle savedInstanceState) {
            setContentView(R.layout.photo_catcher);
            Intent intendt = getIntent();

        }
    }
}
