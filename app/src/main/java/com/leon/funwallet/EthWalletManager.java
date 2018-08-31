package com.leon.funwallet;


import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EthWalletManager {

    private static final String TAG = "EthWalletManager";

    private WalletFile wallet;


    private static final String PASSWORD = "a12345678";

    private ObjectMapper objectMapper = new ObjectMapper();

    private static EthWalletManager sEthWalletManager;



    private EthWalletManager() {
    }


    public static EthWalletManager getInstance() {
        if (sEthWalletManager == null) {
            synchronized (EthWalletManager.class) {
                if (sEthWalletManager == null) {
                    sEthWalletManager = new EthWalletManager();
                }
            }
        }
        return sEthWalletManager;
    }

    public void loadWallet(final ContextWrapper contextWrapper, final OnWalletLoadedListener listener) {
        if (wallet != null && listener != null) {
            listener.onWalletLoaded(wallet);
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File walletDir = contextWrapper.getDir("eth", Context.MODE_PRIVATE);
                    if (walletDir.exists() && walletDir.listFiles().length > 0) {
                        File[] files = walletDir.listFiles();
                        wallet = objectMapper.readValue(files[0], WalletFile.class);
                    } else {
                        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
                        wallet  = Wallet.createLight(PASSWORD, ecKeyPair);
                        String walletFileName = getWalletFileName(wallet);
                        File destination = new File(walletDir, walletFileName);
                        objectMapper.writeValue(destination, wallet);
                    }
                    if (listener != null && wallet != null) {
                        listener.onWalletLoaded(wallet);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                } catch (CipherException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private static String getWalletFileName(WalletFile walletFile) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("'UTC--'yyyy-MM-dd'T'HH-mm-ss.SSS'--'");
        return dateFormat.format(new Date()) + walletFile.getAddress() + ".json";
    }


    public String exportKeyStore(WalletFile wallet) {
        try {
            return objectMapper.writeValueAsString(wallet);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String exportPrivateKey(WalletFile wallet) {
        try {
            ECKeyPair ecKeyPair = Wallet.decrypt(PASSWORD, wallet);
            BigInteger privateKey = ecKeyPair.getPrivateKey();
            return  Numeric.toHexStringNoPrefixZeroPadded(privateKey, Keys.PRIVATE_KEY_LENGTH_IN_HEX);
        } catch (CipherException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static interface OnWalletLoadedListener {
        void onWalletLoaded(WalletFile wallet);
    }
}
