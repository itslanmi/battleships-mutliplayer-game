package si.um.feri.momcilovic.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** UDP broadcast based discovery of hosted games on the local network. */
public class LanDiscovery {

    public static final int DISCOVERY_PORT = 45454;
    private static final String MAGIC = "BSHIP1";
    private static final long EXPIRE_MS = 4000;

    public static class GameInfo {
        public final String gameName;
        public final String hostName;
        public final String address;
        public final int port;
        long lastSeen;

        GameInfo(String gameName, String hostName, String address, int port) {
            this.gameName = gameName;
            this.hostName = hostName;
            this.address = address;
            this.port = port;
        }

        public String key() {
            return hostName + "|" + gameName + "|" + port;
        }
    }

    // ---------------------------------------------------------------- host side

    public static class Broadcaster {
        private volatile boolean running = true;
        private final Thread thread;

        public Broadcaster(String gameName, String hostName, int tcpPort) {
            final byte[] data = (MAGIC + "|" + clean(gameName) + "|" + clean(hostName) + "|" + tcpPort)
                    .getBytes(StandardCharsets.UTF_8);
            thread = new Thread(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setBroadcast(true);
                    while (running) {
                        for (InetAddress target : broadcastTargets()) {
                            try {
                                socket.send(new DatagramPacket(data, data.length, target, DISCOVERY_PORT));
                            } catch (IOException ignored) {
                            }
                        }
                        Thread.sleep(1000);
                    }
                } catch (Exception ignored) {
                }
            }, "lan-broadcast");
            thread.setDaemon(true);
            thread.start();
        }

        public void stop() {
            running = false;
            thread.interrupt();
        }
    }

    private static List<InetAddress> broadcastTargets() {
        List<InetAddress> targets = new ArrayList<>();
        try {
            targets.add(InetAddress.getByName("255.255.255.255"));
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    if (ia.getBroadcast() != null) targets.add(ia.getBroadcast());
                }
            }
        } catch (IOException ignored) {
        }
        return targets;
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('|', ' ').trim();
    }

    // ------------------------------------------------------------- client side

    public static class Listener {
        private final Map<String, GameInfo> games = new ConcurrentHashMap<>();
        private volatile boolean running = true;
        private volatile DatagramSocket socket;

        public Listener() {
            Thread thread = new Thread(() -> {
                try {
                    DatagramSocket s = new DatagramSocket(null);
                    s.setReuseAddress(true);
                    s.bind(new java.net.InetSocketAddress(DISCOVERY_PORT));
                    s.setSoTimeout(500);
                    socket = s;
                    byte[] buf = new byte[512];
                    while (running) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        try {
                            s.receive(p);
                        } catch (SocketTimeoutException e) {
                            continue;
                        }
                        String[] parts = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8).split("\\|");
                        if (parts.length != 4 || !MAGIC.equals(parts[0])) continue;
                        try {
                            GameInfo info = new GameInfo(parts[1], parts[2], p.getAddress().getHostAddress(), Integer.parseInt(parts[3]));
                            info.lastSeen = System.currentTimeMillis();
                            // one entry per game even if it is announced from several network cards
                            GameInfo old = games.get(info.key());
                            if (old != null) {
                                old.lastSeen = info.lastSeen;
                            } else {
                                games.put(info.key(), info);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    s.close();
                } catch (IOException ignored) {
                    // port unavailable: list stays empty, manual IP still works
                }
            }, "lan-listener");
            thread.setDaemon(true);
            thread.start();
        }

        /** Currently visible games, newest-first order is not guaranteed; sorted by name. */
        public List<GameInfo> getGames() {
            long now = System.currentTimeMillis();
            List<GameInfo> result = new ArrayList<>();
            for (GameInfo g : games.values()) {
                if (now - g.lastSeen > EXPIRE_MS) {
                    games.remove(g.key());
                } else {
                    result.add(g);
                }
            }
            Collections.sort(result, (a, b) -> a.gameName.compareToIgnoreCase(b.gameName));
            return result;
        }

        public void stop() {
            running = false;
            DatagramSocket s = socket;
            if (s != null) s.close();
        }
    }
}
