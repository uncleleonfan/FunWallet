package com.leon.funwallet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener;


public class BitcoinWalletActivity extends AppCompatActivity {

    private static final String TAG = "BitcoinWalletActivity";
    private TextView mAddressText;
    private TextView mBalanceText;
    private ImageView mQrImageView;
    private Wallet wallet;


    private EditText mToAddressEdit;
    private EditText mAmountEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitcoin_wallet);
        initView();
        BitcoinWalletManager.getInstance().loadWallet(this, new BitcoinWalletManager.OnWalletLoadedListener() {
            @Override
            public void onWalletLoaded(final Wallet w) {
                wallet = w;
                addWalletListeners();
                updateUI(wallet);
                startService(new Intent(BitcoinWalletActivity.this, BlockChainService.class));

            }

        });
    }

    private void initView() {
        mAddressText = findViewById(R.id.address);
        mBalanceText = findViewById(R.id.balance);
        mQrImageView = findViewById(R.id.qr_code);
        mToAddressEdit = findViewById(R.id.to);
        mAmountEdit = findViewById(R.id.amount);
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

    public void onSendBitcoin(View view) {
        String to = mToAddressEdit.getText().toString();
        String amount = mAmountEdit.getText().toString();
        if (TextUtils.isEmpty(to) || TextUtils.isEmpty(amount)) {
            return;
        }
        Address address = Address.fromBase58(Constants.NETWORK_PARAMETERS, to);
        Coin coin = MonetaryFormat.MBTC.parse(amount);
        SendRequest sendRequest = SendRequest.to(address, coin);
        try {
            Transaction transaction = wallet.sendCoinsOffline(sendRequest);
            BlockChainService.broadcastTransaction(BitcoinWalletActivity.this, transaction);
        } catch (InsufficientMoneyException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private class WalletListener implements WalletChangeEventListener,
            WalletCoinsSentEventListener, WalletReorganizeEventListener, WalletCoinsReceivedEventListener {

        @Override
        public void onWalletChanged(Wallet wallet) {
            Log.d(TAG, "onWalletChanged: " + wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS));
            updateUI(wallet);
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            Log.d(TAG, "onCoinsSent: " + tx.getHashAsString() + "preBalance: "
                    + prevBalance.getValue() + "newBalance: " + newBalance.getValue());
            updateUI(wallet);
        }

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            Log.d(TAG, "onCoinsReceived: " + tx.getHashAsString() + "prevBalance" + prevBalance.getValue()
                    + "newBalance " + newBalance.getValue());
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
    }
}
