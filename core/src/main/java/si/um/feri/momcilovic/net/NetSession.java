package si.um.feri.momcilovic.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Everything the setup and game screens share about one LAN match. */
public class NetSession {

    public static class Placement {
        public final String region;
        public final int row;
        public final int col;
        public final boolean horizontal;
        public final int size;

        public Placement(String region, int row, int col, boolean horizontal, int size) {
            this.region = region;
            this.row = row;
            this.col = col;
            this.horizontal = horizontal;
            this.size = size;
        }
    }

    public final NetConnection connection;
    public final boolean isHost;
    public final String localName;
    public String remoteName = "Opponent";

    public final List<Placement> placements = new ArrayList<>();
    private int[][] shipId;
    private int[] shipRemaining;

    public NetSession(NetConnection connection, boolean isHost, String localName) {
        this.connection = connection;
        this.isHost = isHost;
        this.localName = localName;
    }

    /** Index 0 is always the host, so both machines order players identically. */
    public String[] playerNames() {
        return isHost ? new String[]{localName, remoteName} : new String[]{remoteName, localName};
    }

    public int localIndex() {
        return isHost ? 0 : 1;
    }

    /** Call once all ships are placed. */
    public void buildFleet() {
        shipId = new int[10][10];
        for (int[] row : shipId) Arrays.fill(row, -1);
        shipRemaining = new int[placements.size()];
        for (int i = 0; i < placements.size(); i++) {
            Placement p = placements.get(i);
            shipRemaining[i] = p.size;
            for (int k = 0; k < p.size; k++) {
                int r = p.horizontal ? p.row : p.row + k;
                int c = p.horizontal ? p.col + k : p.col;
                shipId[r][c] = i;
            }
        }
    }

    /**
     * Applies an incoming shot to the local fleet (callers must not shoot the same cell twice).
     * @return 0 = miss, 1 = hit, 2 = hit and the ship sank
     */
    public int receiveShot(int r, int c) {
        int id = shipId[r][c];
        if (id < 0) return 0;
        shipId[r][c] = -1;
        shipRemaining[id]--;
        return shipRemaining[id] == 0 ? 2 : 1;
    }

    public void close() {
        connection.close();
    }
}
