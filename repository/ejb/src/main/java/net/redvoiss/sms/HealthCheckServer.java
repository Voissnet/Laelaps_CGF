package net.redvoiss.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 *
 * @author Jorge Avila
 */
@Singleton
@Startup
public class HealthCheckServer {

    private static final Logger LOGGER = Logger.getLogger(HealthCheckServer.class.getName());
    protected final static int HEARTBEAT_PORT = Integer.parseInt(System.getProperty("heartbeat.port", "30175"));
    protected final static String BINDING_ADDRESS = System.getProperty("bind.address");
    private static final ServerSocket SERVER_SOCKET = produceServerSocket();
    private Future m_task;

    @Resource(lookup = "concurrent/__defaultManagedExecutorService")
    ManagedExecutorService m_executor;

    protected static ServerSocket produceServerSocket() {
        ServerSocket ret = null;
        try {
            if (BINDING_ADDRESS == null) {
                ret = new ServerSocket(HEARTBEAT_PORT);
                LOGGER.log(INFO, "Heartbeat server created to listen on port {0}", HEARTBEAT_PORT);
            } else {
                ret = new ServerSocket(HEARTBEAT_PORT, 0, InetAddress.getByName(BINDING_ADDRESS));
                LOGGER.log(INFO, "Heartbeat server created to listen at {0}:{1}", new Object[]{BINDING_ADDRESS, HEARTBEAT_PORT});
            }
            ret.setSoTimeout(0); // Socket never closes
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Unexpected IO exception", ioe);
        }
        return ret;
    }

    @PostConstruct
    public void init() {
        if (SERVER_SOCKET == null) {
            LOGGER.severe("Server socket is unnavailable");
        } else {
            m_task = m_executor.submit(() -> {
                try {
                    while (true) {
                        Socket clientSocket = SERVER_SOCKET.accept();
                        clientSocket.setSoTimeout(120000); //Timeout 2 minutes (set to what you want)
                        LOGGER.log(FINEST, "Connection from {0} accepted", clientSocket.getInetAddress().getHostAddress());
                        m_executor.submit(() -> {
                            try (BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream()))) {
                                final String command = in.readLine();
                                if ("quit".equals(command) || command == null) {
                                    LOGGER.log(FINE, "Received expected heartbeat from {0}", clientSocket.getInetAddress().getHostAddress());
                                } else {
                                    LOGGER.log(WARNING, "Received unexpected heartbeat {0} from {1}", new String[]{command, clientSocket.getInetAddress().getHostAddress()});
                                }
                            } catch (IOException ex) {
                                LOGGER.log(Level.SEVERE, "Unexpected IO exception", ex);
                            } catch (Exception ex) {
                                LOGGER.log(SEVERE, "Unexpected exception", ex);
                            } finally {
                                try {
                                    clientSocket.close();
                                } catch (IOException ex) {
                                    LOGGER.log(Level.SEVERE, "Unexpected IO exception", ex);
                                } catch (Exception ex) {
                                    LOGGER.log(SEVERE, "Unexpected exception", ex);
                                }
                            }
                        });
                    }
                } finally {
                    LOGGER.info("Heartbeat server exiting");
                }
            });
            if (m_task.isCancelled() || m_task.isDone()) {//Sanity check
                LOGGER.severe("Heartbeat task failed to start");
            } else {
                LOGGER.fine("Heartbeat task seems to be OK");
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        if (SERVER_SOCKET == null) {
            LOGGER.severe("Server socket was unnavailable");
        } else {
            if (m_task.isDone()) {
                LOGGER.warning("Heartbeat server is done");
            } else if (m_task.isCancelled()) {
                LOGGER.warning("Heartbeat server was cancelled before expected");
            } else {
                if (m_task.cancel(true)) {
                    LOGGER.info("Heartbeat server was cancelled without issues");
                } else {
                    LOGGER.warning("Heartbeat server was cancelled before expected");
                }
            }
        }
    }
}
