# Chat-miniprojekt

## Gruppe

Gruppe 4, Hello World
Navne: Nicki, Goncalo, Mattias

## Sådan starter du server og klient

```bash
mvn clean compile
java -cp target/classes ChatServer
java -cp target/classes ChatClient
```

## Protokollen

Klient til server, tekstlinjer adskilt med `|`:

```
TYPE|TARGET|PAYLOAD
```

Eksempler:
```
LOGIN||bob
JOIN_ROOM|room42|
TEXT|room42|Hej alle
PRIVATE|alice|Hej Alice
QUIT||
```

Server til klient:

```
TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD
```

Eksempler:
```
2026-09-25 12:00:00|TEXT|bob|room42|Hej alle
2026-09-25 12:01:00|PRIVATE|alice|bob|Hej Bob
2026-09-25 12:02:00|ERROR|server|bob|Brugernavnet er optaget
```

[Beskriv her de øvrige beskedtyper I selv beslutter jer for, ud over LOGIN, JOIN_ROOM, TEXT, PRIVATE, QUIT]

## Klassediagram

[Indsættes når klassestrukturen er endeligt besluttet og implementeret, som Mermaid/PlantUML]

## Trådmodel og delte ressourcer

Serveren bruger en `ExecutorService` med en fast trådpulje (3 tråde) til at håndtere flere samtidige klienter. Hver forbundet klient får sin egen `ClientHandler`-instans, som kører som en opgave i trådpuljen og håndterer al kommunikation med netop den klient, uafhængigt af de andre.

To samlinger deles mellem alle disse tråde samtidig:

- `ClientRegistry` holder styr på hvilke brugernavne der er logget ind, og hvilken `ClientHandler` der hører til hvert navn. Bruger `ConcurrentHashMap`, og registrering sker atomisk med `putIfAbsent`, så to klienter der forsøger at logge ind med samme brugernavn i præcis samme øjeblik ikke begge kan lykkes (en simpel `containsKey` + `put` ville have været en race condition).
- `ChatRoomManager` holder styr på hvilke brugere der er i hvilket rum. Bruger også `ConcurrentHashMap`, med trådsikre sets som værdier.

**Klientens trådmodel** følger et lignende mønster. `ChatClient` bruger tre tråde:

- Hovedtråden orkestrerer og sender beskeder
- En separat input-tråd læser brugerens tastatur-input og lægger linjerne i en `BlockingQueue`, så hovedtråden ikke behøver blokere uendeligt på brugerinput
- `ServerListener` kører i sin egen tråd og læser løbende beskeder fra serveren

Hovedtråden henter fra input-køen med en kort timeout (500ms) i stedet for at blokere permanent. Det gør det muligt at tjekke en delt `AtomicBoolean` (`connectionLost`), som `ServerListener` sætter hvis forbindelsen til serveren tabes. Dermed opdager klienten en død server proaktivt, indenfor cirka et sekund, selv hvis brugeren ikke skriver noget.

Vi stødte undervejs på en konkret race condition, hovedtråden forsøgte oprindeligt selv at læse et svar fra serveren synkront efter at have sendt QUIT, samtidig med at `ServerListener` allerede læste fra den samme stream i baggrunden. To tråde der læser fra samme socket samtidig gav uforudsigelige resultater. Løsningen var at lade `ServerListener` alene stå for al læsning fra serveren, hovedtråden sender kun beskeder, den læser aldrig selv fra streamen.

## Valgt udvidelse

[Hvilken udvidelse valgte I, og hvordan er den integreret i løsningen]

## AI-dokumentation

| Opgave | AI-værktøj | AI's forslag | Vores vurdering og ændringer | Kontrol og test |
|---|---|---|---|---|
| | | | | |
| | | | | |
| | | | | |

## Test

| Scenarie | Forventet resultat | Resultat |
|---|---|---|
| Tre klienter forbindes samtidig | Alle kan sende og modtage beskeder | |
| To brugere vælger samme brugernavn | Den anden bruger afvises | |
| En bruger sender en besked i et rum | Kun brugere i rummet modtager den | |
| En bruger sender en privat besked | Kun modtageren ser den | |
| En klient sender en fejlformateret besked | Serveren sender en fejl og fortsætter | |
| En klient lukker uventet | Brugeren fjernes fra serverens samlinger | |
| Den valgte udvidelse anvendes | Udvidelsen fungerer som beskrevet | |

## Sekvensdiagram

[Indsættes inden aflevering, viser enten den valgte udvidelse eller et fejlsætningsforløb, som Mermaid/PlantUML]