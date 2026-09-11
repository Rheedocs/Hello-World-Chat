public interface MessageSender {
    String getUsername();
    void sendServerMessage(String type, String sender, String target, String payload);
}
