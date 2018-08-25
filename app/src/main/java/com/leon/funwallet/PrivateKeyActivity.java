package com.leon.funwallet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

public class PrivateKeyActivity extends AppCompatActivity{

    private EditText mPrivateKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_key);

        String pk = getIntent().getStringExtra("pk");

        mPrivateKey = findViewById(R.id.private_key);
        mPrivateKey.setText(pk);

    }
}
