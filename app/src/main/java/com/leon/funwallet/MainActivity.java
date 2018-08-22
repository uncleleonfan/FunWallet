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
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView mAddressText;
    private TextView mBalanceText;
    private ImageView mQrImageView;
    private Wallet wallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAddressText = findViewById(R.id.address);
        mBalanceText = findViewById(R.id.balance);
        mQrImageView = findViewById(R.id.qr_code);
        BitcoinWalletManager.getInstance().loadWallet(this, new BitcoinWalletManager.OnWalletLoadedListener() {
            @Override
            public void onWalletLoaded(final Wallet w) {
                wallet = w;
                addWalletListeners();
                updateUI(wallet);
                startService(new Intent(MainActivity.this, BlockChainService.class));

            }

        });
    }

    private void addWalletListeners() {
        wallet.addChangeEventListener(mWalletListener);
        wallet.addCoinsReceivedEventListener(mWalletListener);
        wallet.addCoinsSentEventListener(mWalletListener);
        wallet.addReorganizeEventListener(mWalletListener);
    }

    private void removeWalletListener() {
        wallet.removeChangeEventListener(mWalletListener);
        wallet.removeCoinsReceivedEventListener(mWalletListener);
        wallet.removeCoinsSentEventListener(mWalletListener);
        wallet.removeReorganizeEventListener(mWalletListener);
    }

    private void updateUI(final Wallet wallet) {
        final Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        String s = BitcoinURI.convertToBitcoinURI(address, null, null, null);
        final Bitmap bitmap = Qr.bitmap(s);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                org.bitcoinj.core.Context.propagate(Constants.CONTEXT);
                Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                mAddressText.setText(address.toString());
                mBalanceText.setText(String.valueOf(balance.value));
                BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                bitmapDrawable.setFilterBitmap(false);
                mQrImageView.setImageDrawable(bitmapDrawable);
            }
        });
    }

    private WalletListener mWalletListener = new WalletListener();

    private class WalletListener implements WalletChangeEventListener,
            WalletCoinsSentEventListener, WalletReorganizeEventListener, WalletCoinsReceivedEventListener {

        @Override
        public void onWalletChanged(Wallet wallet) {
            updateUI(wallet);
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            updateUI(wallet);
        }

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            updateUI(wallet);
        }

        @Override
        public void onReorganize(Wallet wallet) {
            updateUI(wallet);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeWalletListener();
        stopService(new Intent(this, BlockChainService.class));
    }
}
