# Chat-miniprojekt

## Gruppe

Gruppe 4, Hello World
Navne: Nicki, Goncalo, Mattias

## Sådan starter du server og klient

```bash
javac -d out src/**/*.java
java -cp out ChatServer
java -cp out ChatClient
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

[Beskriv her: hvordan ExecutorService bruges, hvordan hver klient får sin egen ClientHandler-tråd, hvilke samlinger der deles mellem tråde (brugerliste, chatrum), og hvorfor jeres valgte løsning (fx ConcurrentHashMap) er trådsikker]

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
