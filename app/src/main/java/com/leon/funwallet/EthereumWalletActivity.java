package com.leon.funwallet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;


public class EthereumWalletActivity extends AppCompatActivity {

    private static final String TAG = "EthereumWalletActivity";

    private WalletFile mWalletFile;

    private TextView mWalletAddressText;

    private static final String HEX_PREFIX = "0x";

    private TextView mBalanceText;

    private Web3j mWeb3j = Web3jFactory.build(new HttpService("https://ropsten.infura.io/1UoO4I/"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethereum_wallet);
        mWalletAddressText = findViewById(R.id.address);
        mBalanceText = findViewById(R.id.balance);

        EthWalletManager.getInstance().loadWallet(this, new EthWalletManager.OnWalletLoadedListener() {
            @Override
            public void onWalletLoaded(WalletFile w) {
                mWalletFile = w;
                Log.d(TAG, "onWalletLoaded: " + mWalletFile.getAddress().length());
                final String address = HEX_PREFIX + mWalletFile.getAddress();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWalletAddressText.setText(address);
                    }
                });


                try {
                    final BigInteger balance = mWeb3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBalanceText.setText(String.valueOf(balance.intValue()));
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
