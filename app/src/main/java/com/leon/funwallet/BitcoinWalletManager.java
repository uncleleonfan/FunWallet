package com.leon.funwallet;

import android.content.ContextWrapper;

import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletFiles;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BitcoinWalletManager {

    private static BitcoinWalletManager sManager;

    private final Executor getWalletExecutor = Executors.newSingleThreadExecutor();

    private Wallet wallet;

    private BitcoinWalletManager() {
    }

    public static BitcoinWalletManager getInstance() {
        if (sManager == null) {
            synchronized (BitcoinWalletManager.class) {
                if (sManager == null) {
                    sManager = new BitcoinWalletManager();
                }
            }
        }
        return sManager;
    }

    public void loadWallet(final ContextWrapper activity, final OnWalletLoadedListener listener) {
        if (wallet != null && listener != null) {
            listener.onWalletLoaded(wallet);
            return;
        }
        getWalletExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File walletFile = activity.getFileStreamPath("wallet-protobuf");
                    if (walletFile.exists()) {
                        InputStream inputStream = new FileInputStream(walletFile);
                        wallet  = new WalletProtobufSerializer().readWallet(inputStream);
                        wallet.autosaveToFile(walletFile, 3 * 1000, TimeUnit.MILLISECONDS, null);
                        wallet.cleanup();
                    } else {
                        wallet = new Wallet(Constants.NETWORK_PARAMETERS);
                        WalletFiles walletFiles = wallet.autosaveToFile(walletFile, 3 * 1000, TimeUnit.MILLISECONDS, null);
                        walletFiles.saveNow();
                    }
                    if (listener != null) {
                        listener.onWalletLoaded(wallet);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (UnreadableWalletException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Wallet getWallet() {
        return wallet;
    }


    public static interface OnWalletLoadedListener {
        void onWalletLoaded(Wallet wallet);
    }
}
