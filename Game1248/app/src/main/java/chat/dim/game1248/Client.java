package chat.dim.game1248;

import android.content.Context;

import java.util.Map;
import java.util.Set;

import chat.dim.ClientMessenger;
import chat.dim.CommonFacebook;
import chat.dim.CompatibleMessenger;
import chat.dim.Config;
import chat.dim.Terminal;
import chat.dim.cache.game.HallCache;
import chat.dim.cache.game.HistoryCache;
import chat.dim.cache.game.TableCache;
import chat.dim.core.Processor;
import chat.dim.database.CipherKeyDatabase;
import chat.dim.database.DocumentDatabase;
import chat.dim.database.GroupDatabase;
import chat.dim.database.MetaDatabase;
import chat.dim.database.PrivateKeyDatabase;
import chat.dim.database.UserDatabase;
import chat.dim.dbi.MessageDBI;
import chat.dim.dbi.SessionDBI;
import chat.dim.filesys.ExternalStorage;
import chat.dim.g1248.AppMessageProcessor;
import chat.dim.g1248.GlobalVariable;
import chat.dim.g1248.SharedDatabase;
import chat.dim.g1248.handler.HallHandler;
import chat.dim.g1248.handler.TableHandler;
import chat.dim.g1248.model.History;
import chat.dim.g1248.protocol.GameCustomizedContent;
import chat.dim.g1248.protocol.GameHallContent;
import chat.dim.g1248.protocol.GameTableContent;
import chat.dim.network.ClientSession;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sqlite.DatabaseConnector;
import chat.dim.sqlite.account.AccountDatabase;
import chat.dim.sqlite.message.MessageDatabase;
import chat.dim.type.Triplet;
import chat.dim.utils.Log;

public class Client extends Terminal implements Observer {

    public int tid = -1;
    public int bid = -1;

    private final ID gameBot;

    public Client(CommonFacebook barrack, SessionDBI sdb, ID bot) {
        super(barrack, sdb);

        gameBot = bot;

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.PlayNextMove);
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (!name.equals(NotificationNames.PlayNextMove)) {
            // should not happen
            return;
        }
        History history = (History) info.get("history");
        assert history != null : "history not found";

        GameTableContent request = GameTableContent.play(history);

//        Log.info("[GAME] sending request: " + gameBot + ", " + request);
//        ClientMessenger messenger = getMessenger();
//        messenger.sendContent(null, gameBot, request, 0);
    }

    @Override
    protected boolean isExpired(long last, long now) {
        // keep online every minute
        return now < (last + 60 * 1000);
    }

    @Override
    protected void keepOnline(ID uid, ClientMessenger messenger) {
        super.keepOnline(uid, messenger);

        GameCustomizedContent request;
        if (tid == -1 || bid == -1) {
            request = GameHallContent.seek(0, 20);
        } else {
            request = GameTableContent.watch(tid, bid);
        }
        Log.info("[GAME] sending request: " + gameBot + ", " + request);
        messenger.sendContent(null, gameBot, request, 0);
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getVersionName() {
        return null;
    }

    @Override
    public String getSystemVersion() {
        return null;
    }

    @Override
    public String getSystemModel() {
        return null;
    }

    @Override
    public String getSystemDevice() {
        return null;
    }

    @Override
    public String getDeviceBrand() {
        return null;
    }

    @Override
    public String getDeviceBoard() {
        return null;
    }

    @Override
    public String getDeviceManufacturer() {
        return null;
    }

    @Override
    protected Processor createProcessor(CommonFacebook facebook, ClientMessenger messenger) {
        return new AppMessageProcessor(facebook, messenger);
    }

    @Override
    protected ClientMessenger createMessenger(ClientSession session, CommonFacebook facebook) {
        MessageDBI mdb = (MessageDBI) facebook.getDatabase();
        return new CompatibleMessenger(session, facebook, mdb);
    }

    private static SharedDatabase createDatabase(Config config, Context context) {
        String rootDir = config.getDatabaseRoot();
        String pubDir = config.getDatabasePublic();
        String priDir = config.getDatabasePrivate();

        // FIXME: Environment.getExternalStorageDirectory().getAbsolutePath();
        ExternalStorage.setRoot(rootDir);

        String adbFile = config.getString("sqlite", "account");
        String mdbFile = config.getString("sqlite", "message");
        //String sdbFile = config.getString("sqlite", "session");
        //String gdbFile = config.getString("sqlite", "game");

        DatabaseConnector adb = new AccountDatabase(context, adbFile);
        DatabaseConnector mdb = new MessageDatabase(context, mdbFile);
        //DatabaseConnector sdb = new SessionDatabase(context, sdbFile);
        //DatabaseConnector gdb = new GameDatabase(context, gdbFile);

        SharedDatabase db = SharedDatabase.getInstance();
        db.privateKeyDatabase = new PrivateKeyDatabase(rootDir, pubDir, priDir, adb);
        db.metaDatabase = new MetaDatabase(rootDir, pubDir, priDir, adb);
        db.documentDatabase = new DocumentDatabase(rootDir, pubDir, priDir, adb);
        db.userDatabase = new UserDatabase(rootDir, pubDir, priDir, adb);
        db.groupDatabase = new GroupDatabase(rootDir, pubDir, priDir, adb);
        db.cipherKeyDatabase = new CipherKeyDatabase(rootDir, pubDir, priDir, mdb);

        db.hallDatabase = new HallCache();
        db.tableDatabase = new TableCache();
        db.historyDatabase = new HistoryCache();
        return db;
    }

    Triplet<String, Integer, ID> getNeighborStation() {
        Set<Triplet<String, Integer, ID>> neighbors = database.allNeighbors();
        if (neighbors != null) {
            for (Triplet<String, Integer, ID> station : neighbors) {
                if (station.first != null && station.second > 0) {
                    return station;
                }
            }
        }
        return null;
    }

    static void prepare(String iniFileContent, Context context) {
        GlobalVariable shared = GlobalVariable.getInstance();
        if (shared.terminal != null) {
            // already loaded
            return;
        }
        // Step 1: load config
        Config config = Config.load(iniFileContent);
        shared.config = config;
        ID bot = config.getANS("g1248");
        assert bot != null : "bot id not set";

        // Step 2: create database
        SharedDatabase db = createDatabase(config, context);
        shared.adb = db;
        shared.mdb = db;
        shared.sdb = db;

        // Step 3: create facebook
        CommonFacebook facebook = new CommonFacebook(db);
        shared.facebook = facebook;

        // Step 4: create terminal
        Client client = new Client(facebook, db, bot);
        Thread thread = new Thread(client);
        thread.setDaemon(false);
        thread.start();
        shared.terminal = client;

        // Step 5: create customized content handlers
        shared.gameHallContentHandler = new HallHandler(db);
        shared.gameTableContentHandler = new TableHandler(db);
    }
}
