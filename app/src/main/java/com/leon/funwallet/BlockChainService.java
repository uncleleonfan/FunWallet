package com.leon.funwallet;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;
import org.bitcoinj.core.listeners.PeerDiscoveredEventListener;
import org.bitcoinj.net.discovery.MultiplexingDiscovery;
import org.bitcoinj.net.discovery.PeerDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BlockChainService extends Service {

    private static final String TAG = "BlockChainService";

    public static final String USER_AGENT = "Bitcoin Wallet";

    public static final String ACTION_BROADCAST_TRANSACTION = "action_broadcast_transaction";
    public static final String ACTION_BROADCAST_TRANSACTION_HASH = "action_broadcast_transaction_hash";

    private SPVBlockStore blockStore;
    private PeerGroup peerGroup;
    private BlockChain blockChain;
    private Wallet wallet;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        wallet = BitcoinWalletManager.getInstance().getWallet();
        if (wallet != null) {
            createBlockChain();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent !=  null) {
            String action = intent.getAction();
            if (action != null && action.equalsIgnoreCase(ACTION_BROADCAST_TRANSACTION)) {
                byte[] hashArray = intent.getByteArrayExtra(ACTION_BROADCAST_TRANSACTION_HASH);
                Sha256Hash sha256Hash = Sha256Hash.wrap(hashArray);
                Transaction transaction = wallet.getTransaction(sha256Hash);
                if (peerGroup != null)  {
                    Log.d(TAG, "onStartCommand: broadcast transaction " + transaction.getHashAsString());
                    peerGroup.broadcastTransaction(transaction);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createBlockChain() {
        Log.d(TAG, "createBlockChain: ");
        File blockChainFile = new File(getDir("blockstore", Context.MODE_PRIVATE), "blockchain");
        boolean blockChainFileExists = blockChainFile.exists();
        if (!blockChainFileExists) {
            wallet.reset();
        }

        try {
            blockStore = new SPVBlockStore(Constants.NETWORK_PARAMETERS, blockChainFile);
            blockStore.getChainHead(); // detect corruptions as early as possible

            final long earliestKeyCreationTime = wallet.getEarliestKeyCreationTime();

            if (!blockChainFileExists && earliestKeyCreationTime > 0) {
                try {
                    final InputStream checkpointsInputStream = getAssets()
                            .open("checkpoints-testnet.txt");
                    CheckpointManager.checkpoint(Constants.NETWORK_PARAMETERS, checkpointsInputStream,
                            blockStore, earliestKeyCreationTime);
                } catch (final IOException x) {
                    x.printStackTrace();
                }
            }
        } catch (final BlockStoreException x) {
            blockChainFile.delete();
            x.printStackTrace();
        }
        try {
            blockChain = new BlockChain(Constants.NETWORK_PARAMETERS, wallet, blockStore);
        } catch (final BlockStoreException x) {
            throw new Error("blockchain cannot be created", x);
        }
        startup();
    }


    private void startup() {
        Log.d(TAG, "startup: ");
        peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS, blockChain);
        peerGroup.setDownloadTxDependencies(0); // recursive implementation causes StackOverflowError
        peerGroup.addWallet(wallet);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            peerGroup.setUserAgent(USER_AGENT, packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        peerGroup.setMaxConnections(8);
        int connectTimeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
        peerGroup.setConnectTimeoutMillis(connectTimeout);
        int discoveryTimeout = (int) (10 * DateUtils.SECOND_IN_MILLIS);
        peerGroup.addConnectedEventListener(mPeerConnectedEventListener);
        peerGroup.addDisconnectedEventListener(mPeerDisconnectedEventListener);
        peerGroup.addDiscoveredEventListener(mPeerDiscoveredEventListener);
        peerGroup.setPeerDiscoveryTimeoutMillis(discoveryTimeout);
        peerGroup.addPeerDiscovery(new PeerDiscovery() {
            private final PeerDiscovery normalPeerDiscovery = MultiplexingDiscovery
                    .forServices(Constants.NETWORK_PARAMETERS, 0);

            @Override
            public InetSocketAddress[] getPeers(final long services, final long timeoutValue,
                                                final TimeUnit timeoutUnit) throws PeerDiscoveryException {
                return normalPeerDiscovery.getPeers(services, timeoutValue, timeoutUnit);
            }

            @Override
            public void shutdown() {
                normalPeerDiscovery.shutdown();
            }
        });
        peerGroup.startAsync();
        peerGroup.startBlockChainDownload(null);
    }


    private PeerConnectedEventListener mPeerConnectedEventListener = new PeerConnectedEventListener() {
        @Override
        public void onPeerConnected(Peer peer, int peerCount) {
            Log.d(TAG, "onPeerConnected: " + peer.toString());
        }
    };

    private PeerDisconnectedEventListener mPeerDisconnectedEventListener = new PeerDisconnectedEventListener() {
        @Override
        public void onPeerDisconnected(Peer peer, int peerCount) {
            Log.d(TAG, "onPeerDisconnected: " + peer.toString());
        }
    };

    private PeerDiscoveredEventListener mPeerDiscoveredEventListener = new PeerDiscoveredEventListener() {
        @Override
        public void onPeersDiscovered(Set<PeerAddress> peerAddresses) {
            if (peerAddresses != null) {
                Log.d(TAG, "onPeersDiscovered: " + peerAddresses.iterator().next().getHostname());
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    private void shutdown() {
        Log.d(TAG, "shutdown: ");
        peerGroup.removeWallet(wallet);
        peerGroup.stopAsync();
        peerGroup.removeConnectedEventListener(mPeerConnectedEventListener);
        peerGroup.removeDisconnectedEventListener(mPeerDisconnectedEventListener);
        peerGroup.removeDiscoveredEventListener(mPeerDiscoveredEventListener);
        peerGroup = null;
    }

    public static void broadcastTransaction(Context context, Transaction transaction) {
        Intent intent = new Intent(ACTION_BROADCAST_TRANSACTION, null, context, BlockChainService.class);
        intent.putExtra(ACTION_BROADCAST_TRANSACTION_HASH, transaction.getHash().getBytes());
        context.startService(intent);
    }
}
