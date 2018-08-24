package com.leon.funwallet;


import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EthWalletManager {

    private static final String TAG = "EthWalletManager";

    private Wallet wallet;

    private static final String PASSWORD = "a12345678";


    private final Executor getWalletExecutor = Executors.newSingleThreadExecutor();

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

    public void loadWallet(final ContextWrapper contextWrapper) {
        getWalletExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File walletDir = contextWrapper.getDir("eth", Context.MODE_PRIVATE);
                    String fileName = WalletUtils.generateLightNewWalletFile(PASSWORD, walletDir);
                    Log.d(TAG, "fileName: " + walletDir);
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


    public static interface OnWalletLoadedListener {
        void onWalletLoaded(Wallet wallet);
    }
}
