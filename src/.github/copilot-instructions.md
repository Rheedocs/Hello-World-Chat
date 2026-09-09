# Copilot Instructions

Chat-miniprojekt, obligatorisk studieaktivitet. Mål er ikke kun at få det til at virke, alle gruppemedlemmer skal kunne forklare protokollen, trådmodellen, de delte ressourcer og håndteringen af klientafbrydelser.

## Opgaven

Et chatprogram i Java hvor flere klienter kommunikerer gennem én fælles server. Serveren skal håndtere mindst tre samtidige klienter via en trådpulje (ExecutorService), ikke sekventielt som sidste opgaves TCP-filoverførsel.

## Build og kør

```bash
javac -d out src/**/*.java
java -cp out ChatServer
java -cp out ChatClient
```

## Arkitektur

Foreslået klassestruktur, følg medmindre I har en god grund til at afvige:

- `ChatServer`, starter serveren, accepterer forbindelser, bruger `ExecutorService`
- `ClientHandler`, håndterer kommunikationen med én klient, kører i egen tråd
- `ChatClient`, forbinder klienten, sender brugerens beskeder
- `ServerListener`, separat tråd på klientsiden der modtager beskeder fra serveren, mens brugeren skriver
- `Message`, repræsenterer en besked
- `MessageParser`, opbygger og parser protokolbeskeder
- `ClientRegistry`, holder styr på tilsluttede brugere, skal være trådsikker
- `ChatRoomManager`, holder styr på chatrum og medlemmer, skal være trådsikker

## Protokol

Klient til server, tekstlinjer adskilt med `|`:
```
TYPE|TARGET|PAYLOAD
```
Eksempler: `LOGIN||bob`, `JOIN_ROOM|room42|`, `TEXT|room42|Hej alle`, `PRIVATE|alice|Hej Alice`, `QUIT||`

Server parser med `message.split("\\|", 3)`.

Server til klient:
```
TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD
```
Klient parser med `message.split("\\|", 5)`.

Serveren fastsætter altid afsender og tidspunkt selv, stoler ikke på brugernavn sendt i beskeden efter login, brugernavnet er allerede knyttet til ClientHandler.

## Trådsikkerhed

Delte samlinger (brugerliste, chatrum-medlemskab) tilgås af flere ClientHandler-tråde samtidig. Brug ConcurrentHashMap eller trådsikre sets, ikke almindelig HashMap/ArrayList uden synkronisering. I skal kunne forklare hvorfor jeres valgte løsning er sikker.

## Fejlhåndtering

Fejlformaterede beskeder må aldrig crashe serveren for andre klienter, kun sende en fejl tilbage til den ene klient. Klienter der lukker forbindelsen (normalt eller uventet) skal fjernes korrekt fra alle samlinger (brugerliste, chatrum).

## Kodestil

- Kommentarer og fejlbeskeder på dansk, identifiers på engelsk
- Enkel og letforståelig kode, alle skal kunne forklare det til demonstrationen
- KISS, ingen unødvendig kompleksitet
- Commit-stil: kort, lowercase, conventional commits (feat/fix/test)

## Test

JUnit på MessageParser og central beskedhåndtering er krav, ikke valgfrit. Test med flere samtidige klienter må gerne dokumenteres som manuel integrationstest.

## Arbejdsproces med agenten

Byg i små trin, test hvert trin før næste:
1. Én klient forbinder, sender én besked
2. ExecutorService tilføjes, mindst tre klienter forbindes
3. Unikke brugernavne, broadcast
4. Chatrum, private beskeder
5. Fejlhåndtering, korrekt afbrydelse
6. Obligatoriske tests
7. Valgt udvidelse

Bed ikke om hele løsningen på én gang. Efter hvert trin, kør, test, forklar.