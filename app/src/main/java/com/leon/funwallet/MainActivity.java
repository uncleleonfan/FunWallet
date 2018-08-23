package com.leon.funwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity{


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onStartBitcoinWallet(View view) {
        Intent intent = new Intent(this, BitcoinWalletActivity.class);
        startActivity(intent);
    }

    public void onstartEthWallet(View view) {
        Intent intent = new Intent(this, EthereumWalletActivity.class);
        startActivity(intent);
    }
}
