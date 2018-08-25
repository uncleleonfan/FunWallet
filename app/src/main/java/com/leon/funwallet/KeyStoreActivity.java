package com.leon.funwallet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

public class KeyStoreActivity extends AppCompatActivity {

    private EditText mKeyStoreEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keystore);
        mKeyStoreEdit = findViewById(R.id.key_store);

        String keystore = getIntent().getStringExtra("keystore");
        mKeyStoreEdit.setText(keystore);

    }
}
