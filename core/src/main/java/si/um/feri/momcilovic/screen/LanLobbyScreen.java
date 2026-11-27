package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;
import si.um.feri.momcilovic.net.LanDiscovery;
import si.um.feri.momcilovic.net.NetConnection;
import si.um.feri.momcilovic.net.NetSession;

/** Host a LAN game or search for and join one hosted by someone else. */
public class LanLobbyScreen extends ScreenAdapter {

    private static final String PREFS_NAME = "settingsPreferences";
    private static final String LAN_NAME_KEY = "lanPlayerName";
    private static final int MAX_NAME = 16;
    private static final int DEFAULT_PORT = 45455;

    private enum State {BROWSE, HOSTING, HANDSHAKE}

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private Stage stage;
    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private TextField nameField;
    private TextField gameNameField;
    private TextField searchField;
    private TextField ipField;
    private Label messageLabel;
    private Table gamesTable;

    private State state = State.BROWSE;
    private LanDiscovery.Listener listener;
    private LanDiscovery.Broadcaster broadcaster;
    private ServerSocket serverSocket;

    private volatile NetConnection pendingConnection;
    private volatile String pendingError;
    private boolean pendingIsHost;
    private NetSession session;
    private float handshakeTime;
    private float refreshTimer;
    private String lastListKey = null;

    public LanLobbyScreen(MomcilovicBattleshipGame game) {
        this.game = game;
        assetManager = game.getAssetManager();
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());
        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        nameField = new TextField(prefs.getString(LAN_NAME_KEY, ""), skin);
        nameField.setMaxLength(MAX_NAME);
        nameField.setMessageText("Your name");
        gameNameField = new TextField("", skin);
        gameNameField.setMaxLength(MAX_NAME + 8);
        gameNameField.setMessageText("Game name");
        searchField = new TextField("", skin);
        searchField.setMessageText("Search games...");
        ipField = new TextField("", skin);
        ipField.setMessageText("Host IP (optional)");
        messageLabel = new Label("", skin);

        listener = new LanDiscovery.Listener();
        showBrowse();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        if (pendingError != null) {
            String err = pendingError;
            pendingError = null;
            leaveNetwork();
            showBrowse();
            messageLabel.setText(err);
        }

        NetConnection conn = pendingConnection;
        if (conn != null && state == State.BROWSE) {
            // connection finished after the user cancelled
            pendingConnection = null;
            conn.close();
        } else if (conn != null && state == State.HOSTING) {
            pendingConnection = null;
            stopHosting();
            session = new NetSession(conn, pendingIsHost, currentName());
            conn.send("HELLO " + session.localName);
            state = State.HANDSHAKE;
            handshakeTime = 0;
            showStatus("Connected, starting...", false);
        }

        if (state == State.HANDSHAKE) {
            handshakeTime += delta;
            String line;
            while ((line = session.connection.poll()) != null) {
                if (line.startsWith("HELLO ")) {
                    session.remoteName = line.substring(6).trim();
                    stopListener();
                    game.setScreen(new GameSetupScreen(game, session));
                    return;
                }
            }
            if (handshakeTime > 6 || session.connection.isClosed()) {
                session.close();
                session = null;
                pendingError = "Opponent did not respond.";
            }
        } else if (state == State.BROWSE) {
            refreshTimer -= delta;
            if (refreshTimer <= 0) {
                refreshTimer = 0.4f;
                refreshGameList();
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide() {
        // when the handshake succeeded the connection lives on in the session
        if (session == null) leaveNetwork();
        stopListener();
        stage.dispose();
    }

    // ------------------------------------------------------------------ UI

    private Table baseTable() {
        Table table = new Table();
        table.defaults().pad(8);
        table.setBackground(new TextureRegionDrawable(gameplayAtlas.findRegion(RegionNames.BACKGROUND)));
        table.setFillParent(true);
        return table;
    }

    private void showBrowse() {
        state = State.BROWSE;
        lastListKey = null;
        stage.clear();

        Table table = baseTable();
        table.add(new Label("LAN Game", skin, "title")).colspan(2).padBottom(10).row();

        table.add(new Label("Your name:", skin)).left();
        table.add(nameField).width(420).row();

        table.add(new Label("Host a game:", skin)).left();
        Table hostRow = new Table();
        hostRow.add(gameNameField).width(300).padRight(10);
        TextButton hostButton = new TextButton("Host", skin, "round");
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startHosting();
            }
        });
        hostRow.add(hostButton);
        table.add(hostRow).row();

        table.add(new Label("Find a game:", skin)).left();
        table.add(searchField).width(420).row();

        gamesTable = new Table();
        gamesTable.top();
        ScrollPane scroll = new ScrollPane(gamesTable, skin);
        scroll.setScrollingDisabled(true, false);
        table.add(scroll).colspan(2).width(760).height(260).row();

        table.add(new Label("Or by IP:", skin)).left();
        Table ipRow = new Table();
        ipRow.add(ipField).width(300).padRight(10);
        TextButton joinIpButton = new TextButton("Join", skin, "round");
        joinIpButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String ip = ipField.getText().trim();
                if (ip.isEmpty()) {
                    messageLabel.setText("Enter the host's IP address.");
                } else {
                    joinGame(ip, 0);
                }
            }
        });
        ipRow.add(joinIpButton);
        table.add(ipRow).row();

        table.add(messageLabel).colspan(2).row();

        TextButton backButton = new TextButton("Back", skin, "round");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
        table.add(backButton).colspan(2).padTop(10);

        stage.addActor(table);
        stage.setKeyboardFocus(searchField);
        refreshGameList();
    }

    private void showStatus(String text, boolean cancelable) {
        stage.clear();
        Table table = baseTable();
        table.add(new Label(text, skin, "title")).row();
        if (cancelable) {
            TextButton cancel = new TextButton("Cancel", skin, "round");
            cancel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    leaveNetwork();
                    showBrowse();
                }
            });
            table.add(cancel).padTop(20);
        }
        stage.addActor(table);
    }

    private void refreshGameList() {
        List<LanDiscovery.GameInfo> all = listener.getGames();
        String filter = searchField.getText().trim().toLowerCase();
        StringBuilder key = new StringBuilder(filter).append('#');
        for (LanDiscovery.GameInfo g : all) {
            if (matches(g, filter)) key.append(g.key()).append(g.gameName).append(g.hostName).append(';');
        }
        if (key.toString().equals(lastListKey)) return;
        lastListKey = key.toString();

        gamesTable.clearChildren();
        int shown = 0;
        for (final LanDiscovery.GameInfo g : all) {
            if (!matches(g, filter)) continue;
            shown++;
            TextButton row = new TextButton(g.gameName + "  -  " + g.hostName + "  (" + g.address + ")", skin, "round");
            row.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    joinGame(g.address, g.port);
                }
            });
            gamesTable.add(row).fillX().expandX().pad(4).row();
        }
        if (shown == 0) {
            gamesTable.add(new Label(all.isEmpty() ? "Searching for games on your network..." : "No games match your search.", skin)).pad(20);
        }
    }

    private static boolean matches(LanDiscovery.GameInfo g, String filter) {
        return filter.isEmpty() || g.gameName.toLowerCase().contains(filter) || g.hostName.toLowerCase().contains(filter);
    }

    // ------------------------------------------------------------- networking

    private String currentName() {
        String name = nameField.getText().trim();
        return name.isEmpty() ? "Player" : name;
    }

    private void saveName() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(LAN_NAME_KEY, nameField.getText().trim());
        prefs.flush();
    }

    private void startHosting() {
        saveName();
        String gameName = gameNameField.getText().trim();
        if (gameName.isEmpty()) gameName = currentName() + "'s game";
        try {
            // fixed port so joining by IP works; fall back to any free port (e.g. second host on one PC)
            try {
                serverSocket = new ServerSocket(DEFAULT_PORT);
            } catch (IOException busy) {
                serverSocket = new ServerSocket(0);
            }
        } catch (IOException e) {
            messageLabel.setText("Could not start hosting: " + e.getMessage());
            return;
        }
        final ServerSocket server = serverSocket;
        pendingIsHost = true;
        Thread acceptThread = new Thread(() -> {
            try {
                Socket socket = server.accept();
                pendingConnection = new NetConnection(socket);
            } catch (IOException ignored) {
                // closed because hosting was cancelled
            }
        }, "lan-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        broadcaster = new LanDiscovery.Broadcaster(gameName, currentName(), server.getLocalPort());
        state = State.HOSTING;
        showStatus("Hosting \"" + gameName + "\"\nWaiting for an opponent...", true);
    }

    private void joinGame(final String address, final int knownPort) {
        saveName();
        pendingIsHost = false;
        showStatus("Joining " + address + "...", true);
        state = State.HOSTING; // reuse "cancelable waiting" state until the connection arrives
        Thread t = new Thread(() -> {
            try {
                // manual IP has no discovery info, so fall back to asking the default port
                int port = knownPort > 0 ? knownPort : DEFAULT_PORT;
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(address, port), 4000);
                pendingConnection = new NetConnection(socket);
            } catch (IOException e) {
                pendingError = "Could not join: " + e.getMessage();
            }
        }, "lan-join");
        t.setDaemon(true);
        t.start();
    }

    private void stopHosting() {
        if (broadcaster != null) {
            broadcaster.stop();
            broadcaster = null;
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
    }

    private void stopListener() {
        if (listener != null) {
            listener.stop();
        }
    }

    private void leaveNetwork() {
        stopHosting();
        NetConnection c = pendingConnection;
        pendingConnection = null;
        if (c != null) c.close();
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
