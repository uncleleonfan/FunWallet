package com.leon.funwallet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class MnemonicActivity extends AppCompatActivity {

    private static final String TAG = "MnemonicActivity";

    private EditText mnemonicEdit;

    private TextView walletAddressEdit;

    private EditText importMnemonicEdit;
    private TextView importWalletAddress;

    private static final String PASSWORD = "a12345678";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnemonic);
        mnemonicEdit = findViewById(R.id.mnemonics);
        walletAddressEdit = findViewById(R.id.address);
        importMnemonicEdit = findViewById(R.id.import_mnemonic);
        importWalletAddress = findViewById(R.id.import_address);
    }

    public void onCreateMnemonicWallet(View view) {

//        List<String> wordList = MnemonicCode.INSTANCE.getWordList();
        try {
            List<String> words = createMnemonics();
            mnemonicEdit.setText(words.toString());
            WalletFile light = createWalletFile(words);
            String address = "0x" + light.getAddress();
            walletAddressEdit.setText(address);
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicChecksumException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicWordException e) {
            e.printStackTrace();
        } catch (CipherException e) {
            e.printStackTrace();
        }
    }

    private WalletFile createWalletFile(List<String> words) throws MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException, CipherException {
        byte[] seeds = MnemonicCode.INSTANCE.toEntropy(words);
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seeds);
        DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);
        DeterministicKey child = deterministicHierarchy.deriveChild(DeterministicKeyChain.BIP44_ACCOUNT_ZERO_PATH, true,
                true, ChildNumber.ZERO);
        ECKeyPair ecKeyPair = ECKeyPair.create(child.getPrivKeyBytes());
        return Wallet.createLight(PASSWORD, ecKeyPair);
    }

    public List<String> createMnemonics() throws MnemonicException.MnemonicLengthException {
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = new byte[DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8];
        secureRandom.nextBytes(entropy);
        return  MnemonicCode.INSTANCE.toMnemonic(entropy);
    }

    public void onImportMnemonics(View view) throws CipherException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException {
        String s = importMnemonicEdit.getText().toString().trim();
        String[] split = s.split(",");
        ArrayList<String> list = new ArrayList<>();
        for (String one : split) {
            String word = one.trim();
            list.add(word);
        }
        WalletFile walletFile = createWalletFile(list);
        String address = "0x" + walletFile.getAddress();
        importWalletAddress.setText(address);
    }
}
