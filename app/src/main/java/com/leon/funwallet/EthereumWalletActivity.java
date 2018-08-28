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

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class EthereumWalletActivity extends AppCompatActivity {

    private static final String TAG = "EthereumWalletActivity";

    private WalletFile mWalletFile;

    private EditText mWalletAddressText;

    private static final String HEX_PREFIX = "0x";

    private TextView mBalanceText;

    private TextView mTokenBalanceText;

    private EditText mToAddressEdit;
    private EditText mAmountEdit;

    private EditText mTokenToAddressEdit;
    private EditText mTokenAmountEdit;



    private Web3j mWeb3j = Web3jFactory.build(new HttpService("https://ropsten.infura.io/1UoO4I/"));
    private String CONTRACT_ADDRESS = "0xaac1a52900b8651c9e1e2972d8e4c80cab2ce875";

    private String mAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethereum_wallet);
        mWalletAddressText = findViewById(R.id.address);
        mBalanceText = findViewById(R.id.balance);
        mToAddressEdit = findViewById(R.id.to);
        mAmountEdit = findViewById(R.id.amount);
        mTokenBalanceText = findViewById(R.id.token_balance);
        mTokenToAddressEdit = findViewById(R.id.token_to);
        mTokenAmountEdit = findViewById(R.id.token_amount);

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
                    Function function = balanceOf(mAddress);
                    String s = callSmartContractFunction(function, CONTRACT_ADDRESS);
                    Log.d(TAG, "run: updateBalance " + s);
                    List<Type> decode = FunctionReturnDecoder.decode(s, function.getOutputParameters());
                    if (decode != null && decode.size() > 0) {
                        Uint256 type = (Uint256) decode.get(0);
                        BigInteger tokenBalance = type.getValue();
                        if (tokenBalance != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTokenBalanceText.setText(tokenBalance.toString());
                                }
                            });
                        }

                    }

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
                } catch (Exception e) {
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
            case R.id.mnemonics:
                Intent intent = new Intent(this, MnemonicActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportPrivateKey() {
        if (mWalletFile == null) {
            return;
        }

        Intent intent = new Intent(this, PrivateKeyActivity.class);
        intent.putExtra("pk", EthWalletManager.getInstance().exportPrivateKey(mWalletFile));
        startActivity(intent);

    }

    private void exportKeyStore() {
        if (mWalletFile == null) {
            return;
        }
        Intent intent = new Intent(this, KeyStoreActivity.class);
        intent.putExtra("keystore", EthWalletManager.getInstance().exportKeyStore(mWalletFile));
        startActivity(intent);
    }

    private Function balanceOf(String owner) {
        return new Function("balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>(){}));
    }

    private String callSmartContractFunction(
            Function function, String contractAddress) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.response.EthCall response = mWeb3j.ethCall(
                Transaction.createEthCallTransaction(
                        mAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        return response.getValue();
    }

    private Function transfer(String to, BigInteger value) {
        return new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(value)),
                Collections.singletonList(new TypeReference<Bool>() {}));
    }

    public void onSendMET(View view) {
        if (TextUtils.isEmpty(mTokenToAddressEdit.getText())
                || TextUtils.isEmpty(mTokenAmountEdit.getText())) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String to = mTokenToAddressEdit.getText().toString();
                    String amount = mTokenAmountEdit.getText().toString();
                    Function transfer = transfer(to, new BigInteger(amount));
                    ECKeyPair ecKeyPair = Wallet.decrypt("a12345678", mWalletFile);
                    Credentials credentials = Credentials.create(ecKeyPair);
                    String transactionHash = execute(credentials, transfer, CONTRACT_ADDRESS);
                    Log.d(TAG, "onSendMET: " + transactionHash);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(EthereumWalletActivity.this, transactionHash, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (CipherException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private String execute(
            Credentials credentials, Function function, String contractAddress) throws Exception {
        BigInteger nonce =  mWeb3j.ethGetTransactionCount(mAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount();
        BigInteger gasPrice = mWeb3j.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = new BigInteger("200000");
        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                contractAddress,
                encodedFunction);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction transactionResponse = mWeb3j.ethSendRawTransaction(hexValue)
                .sendAsync().get();

        return transactionResponse.getTransactionHash();
    }
}
