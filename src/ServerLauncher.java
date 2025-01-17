import org.glassfish.tyrus.server.Server;

public class ServerLauncher {
    public static void main(String[] args) {
        Server server = new Server("localhost", 8080, "/", null, ChatServer.class);
        try {
            server.start();
            System.out.println("WebSocket server started at ws://localhost:8080/chat");
            System.out.println("Press Enter to stop the server...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}
