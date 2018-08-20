package com.leon.funwallet;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletFiles;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView mAddressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAddressText = findViewById(R.id.address);
        new WalletTask().execute();
    }

    //TODO: leaks
    private class WalletTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String result = "";
            Wallet wallet;
            try {
                File walletFile = getFileStreamPath("wallet-protobuf");
                if (walletFile.exists()) {
                    InputStream inputStream = new FileInputStream(walletFile);
                    wallet  = new WalletProtobufSerializer().readWallet(inputStream);
                } else {
                    wallet = new Wallet(TestNet3Params.get());
                    WalletFiles walletFiles = wallet.autosaveToFile(walletFile, 3 * 1000, TimeUnit.MILLISECONDS, null);
                    walletFiles.saveNow();
                }
                result = wallet.currentReceiveAddress().toString();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnreadableWalletException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String address) {
            mAddressText.setText(address);
        }
    }
}
