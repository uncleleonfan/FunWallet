package com.leon.funwallet;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;


public class EthereumWalletActivity extends AppCompatActivity {

    private static final String TAG = "EthereumWalletActivity";

    private WalletFile mWalletFile;

    private EditText mWalletAddressText;

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
                        updateBalance(address);
                    }
                });
            }
        });

    }

    private void updateBalance(final String address) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: " + address);
                    final BigInteger balance = mWeb3j.ethGetBalance(address,
                            DefaultBlockParameterName.LATEST).send().getBalance();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BigDecimal bigDecimal = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);
                            String balanceString = bigDecimal.setScale(8, RoundingMode.FLOOR).toPlainString() + " eth";
                            mBalanceText.setText(balanceString);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onSendEth(View view) {

    }
}
