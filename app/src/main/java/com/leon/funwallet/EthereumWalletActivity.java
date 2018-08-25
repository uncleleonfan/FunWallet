package com.leon.funwallet;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

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

    private EditText mToAddressEdit;
    private EditText mAmountEdit;

    private Web3j mWeb3j = Web3jFactory.build(new HttpService("https://ropsten.infura.io/1UoO4I/"));


    private String mAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethereum_wallet);
        mWalletAddressText = findViewById(R.id.address);
        mBalanceText = findViewById(R.id.balance);
        mToAddressEdit = findViewById(R.id.to);
        mAmountEdit = findViewById(R.id.amount);

        EthWalletManager.getInstance().loadWallet(this, new EthWalletManager.OnWalletLoadedListener() {
            @Override
            public void onWalletLoaded(WalletFile w) {
                mWalletFile = w;
                Log.d(TAG, "onWalletLoaded: " + mWalletFile.getAddress().length());
                mAddress = HEX_PREFIX + mWalletFile.getAddress();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWalletAddressText.setText(mAddress);
                        updateBalance();
                    }
                });
            }
        });

    }

    private void updateBalance() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: " + mAddress);
                    final BigInteger balance = mWeb3j.ethGetBalance(mAddress,
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
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (mAddress == null || TextUtils.isEmpty(mToAddressEdit.getText().toString())
                        || TextUtils.isEmpty(mAmountEdit.getText().toString())) return;

                try {
                    BigInteger transactionCount = mWeb3j.ethGetTransactionCount(mAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount();
                    BigInteger gasPrice = mWeb3j.ethGasPrice().send().getGasPrice();
                    Log.d(TAG, "run: " + transactionCount + ", " + gasPrice);
                    BigInteger gasLimit = new BigInteger("200000");
                    BigDecimal value = Convert.toWei(mAmountEdit.getText().toString().trim(), Convert.Unit.ETHER);
                    Log.d(TAG, "run: value wei" + value.toPlainString());
                    String to = mToAddressEdit.getText().toString().trim();
                    RawTransaction etherTransaction = RawTransaction.createEtherTransaction(transactionCount, gasPrice, gasLimit, to, value.toBigInteger());
                    ECKeyPair ecKeyPair = Wallet.decrypt("a12345678", mWalletFile);
                    Credentials credentials = Credentials.create(ecKeyPair);
                    byte[] bytes = TransactionEncoder.signMessage(etherTransaction, credentials);
                    String hexValue = Numeric.toHexString(bytes);
                    final String transactionHash = mWeb3j.ethSendRawTransaction(hexValue).send().getTransactionHash();
                    Log.d(TAG, "run: transactionHash " + transactionHash);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(EthereumWalletActivity.this, "Send success!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CipherException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.eth_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_wallet_file:
                exportKeyStore();
                break;
            case R.id.export_private_key:
                exportPrivateKey();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportPrivateKey() {
        if (mWalletFile == null) {
            return;
        }
        try {
            ECKeyPair ecKeyPair = Wallet.decrypt("a12345678", mWalletFile);
            BigInteger privateKey = ecKeyPair.getPrivateKey();
            String privateKeyString = Numeric.toHexStringNoPrefixZeroPadded(privateKey, Keys.PRIVATE_KEY_LENGTH_IN_HEX);
            Intent intent = new Intent(this, PrivateKeyActivity.class);
            intent.putExtra("pk", privateKeyString);
            startActivity(intent);
        } catch (CipherException e) {
            e.printStackTrace();
        }
    }

    private void exportKeyStore() {
        if (mWalletFile == null) {
            return;
        }
        Intent intent = new Intent(this, KeyStoreActivity.class);
        intent.putExtra("keystore", EthWalletManager.getInstance().exportKeyStore(mWalletFile));
        startActivity(intent);
    }
}
