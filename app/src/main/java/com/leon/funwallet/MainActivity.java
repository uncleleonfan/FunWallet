package com.leon.funwallet;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.bitcoinj.core.Address;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Wallet;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView mAddressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAddressText = findViewById(R.id.address);
        BitcoinWalletManager.getInstance().getWallet(this, new BitcoinWalletManager.OnWalletLoadedListener() {
            @Override
            public void onWalletLoaded(final Wallet wallet) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
                        mAddressText.setText(address.toString());
                    }
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, BlockChainService.class));
    }
}
