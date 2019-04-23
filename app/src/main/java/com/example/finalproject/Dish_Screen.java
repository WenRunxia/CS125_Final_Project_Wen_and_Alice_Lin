package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public final class Dish_Screen extends AppCompatActivity {
    private static final String TAG = "Dish Screen";
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dish_screen);
        findViewById(R.id.backToHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, Photo_Catcher.class);
            startActivity(intent);
        });
    }
}
