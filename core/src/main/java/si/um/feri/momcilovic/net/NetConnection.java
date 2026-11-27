package si.um.feri.momcilovic.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Line based TCP connection. A background thread reads lines into a queue
 * which the screens poll from render(), so no UI work happens on the network thread.
 */
public class NetConnection {

    public static final String LEAVE = "LEAVE";

    private final Socket socket;
    private final PrintWriter out;
    private final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<>();
    private volatile boolean closed = false;

    public NetConnection(Socket socket) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    inbox.add(line);
                }
            } catch (IOException ignored) {
                // connection dropped
            }
            closed = true;
            inbox.add(LEAVE);
        }, "net-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public synchronized void send(String line) {
        if (!closed) {
            out.println(line);
        }
    }

    /** @return next received line or null if there is none. */
    public String poll() {
        return inbox.poll();
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        if (!closed) {
            send(LEAVE);
        }
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
