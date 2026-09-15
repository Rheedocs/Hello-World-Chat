package domain;

/**
 * Immutable value object, der repræsenterer en parsed protokolbesked.
 * Objektet holder typen, målet og payloaden, så serveren og klienten kan håndtere meddelelsen ens.
 */
public class Message {
    private final String type;
    private final String target;
    private final String payload;

    /**
     * Opretter en ny besked med den konkrete type, destination og data.
     */
    public Message(String type, String target, String payload) {
        this.type = type;
        this.target = target;
        this.payload = payload;
    }

    /**
     * Returnerer beskedens protokolltype, for eksempel LOGIN eller TEXT.
     */
    public String getType() {
        return type;
    }

    /**
     * Returnerer den målrettede modtager for beskeden.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Returnerer selve indholdet i beskeden.
     */
    public String getPayload() {
        return payload;
    }
}
