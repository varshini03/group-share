import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.io.IOException;

@ServerEndpoint(value = "/chat")
public class ChatServer {
    private static final String UPLOAD_DIR = "F:\\uploads"; // Directory for storing files
    private String currentFileName;
    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<>());
    private static Map<Session, String> usernames = Collections.synchronizedMap(new HashMap<>());

    @OnOpen
    public void onOpen(Session session) {
        clients.add(session);
        System.out.println("New connection: " + session.getId());
        ensureUploadDirExists();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            // Handle different message types (e.g., sign-in, chat, sign-out)
            if (message.startsWith("SIGN_IN:")) {
                String username = message.substring("SIGN_IN:".length()).trim();
                usernames.put(session, username);
                broadcast("USER_LIST:" + String.join(",", usernames.values()));
                broadcast(username + " has joined the chat.");
            } else if (message.startsWith("CHAT:")) {
                String chatMessage = usernames.get(session) + ": " + message.substring("CHAT:".length()).trim();
                broadcast(chatMessage);
            } else if (message.startsWith("FILE_NAME:")) {
                currentFileName = message.substring("FILE_NAME:".length());
            }else if (message.equals("SIGN_OUT")) {
                String username = usernames.get(session);
                usernames.remove(session);
                clients.remove(session);
                broadcast(username + " has left the chat.");
                broadcast("USER_LIST:" + String.join(",", usernames.values()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onBinaryMessage(Session session, ByteBuffer byteBuffer) {
        if (currentFileName != null) {
            try {
                File file = new File(UPLOAD_DIR, currentFileName);
                try (FileOutputStream fos = new FileOutputStream(file, true)) {
                    fos.write(byteBuffer.array());
                }
                broadcast("FILE:" + currentFileName); // Notify clients about the new file
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        String username = usernames.get(session);
        if (username != null) {
            clients.remove(session);
            usernames.remove(session);
            broadcast(username + " has left the chat.");
            broadcast("USER_LIST:" + String.join(",", usernames.values()));
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error in session " + session.getId() + ": " + throwable.getMessage());
    }

    private void broadcast(String message) {
        synchronized (clients) {
            for (Session client : clients) {
                try {
                    client.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void ensureUploadDirExists() {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }
    }
}
