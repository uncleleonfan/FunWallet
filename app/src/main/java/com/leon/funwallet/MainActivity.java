package com.leon.funwallet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Wallet;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView mAddressText;
    private TextView mBalanceText;
    private ImageView mQrImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAddressText = findViewById(R.id.address);
        mBalanceText = findViewById(R.id.balance);
        mQrImageView = findViewById(R.id.qr_code);
        BitcoinWalletManager.getInstance().getWallet(this, new BitcoinWalletManager.OnWalletLoadedListener() {
            @Override
            public void onWalletLoaded(final Wallet wallet) {
                final Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
                String s = BitcoinURI.convertToBitcoinURI(address, null, null, null);
                final Bitmap bitmap = Qr.bitmap(s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                        mAddressText.setText(address.toString());
                        mBalanceText.setText(String.valueOf(balance.value));
                        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                        bitmapDrawable.setFilterBitmap(false);
                        mQrImageView.setImageDrawable(bitmapDrawable);
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
