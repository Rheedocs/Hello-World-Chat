package protocol;

/**
 * Abstraktion for en klient eller server-side handler, der kan sende serverbeskeder tilbage til brugeren.
 */
public interface MessageSender {
    /**
     * Returnerer den registrerede brugernavnstilknytning for den aktuelle klient.
     */
    String getUsername();

    /**
     * Sender en serverbesked med det relevante format tilbage til modtageren.
     */
    void sendServerMessage(String type, String sender, String target, String payload);
}
