# Custom-comands per Fabric 26.2

Port dei comandi Discord, annunci e scambi di oggetti tramite menu condiviso.

## Installazione

Richiede **Minecraft Java 26.2**, **Java 25**, **Fabric Loader 0.19.5** e **Fabric API 0.161.0+26.2**. La mod si installa solo sul server; i giocatori possono usare il client vanilla 26.2.

1. Compila con `cd fabric` e `./gradlew build` (su Windows: `gradlew.bat build`).
2. Copia il JAR da `fabric/build/libs/` nella cartella `mods/` del server, insieme a Fabric API per 26.2. Usa il JAR senza `-sources` nel nome.
3. Avvia il server Fabric. I file della mod sono in `config/`.

Le versioni della toolchain sono fissate nel progetto; il wrapper Gradle verifica lo SHA-256 della distribuzione.

## Comandi e scambi

- `/discord`: mostra il collegamento Discord originale.
- `/annuncio <messaggio>`: annuncio globale. Accessibile a console/operatori di livello almeno 2; nodo Fabric Permission API `customcomands:annuncio`.
- `/trade <giocatore>`: richiesta di scambio; i giocatori devono essere nello stesso mondo e a distanza massima di 10 blocchi.
- `/trade accept` e `/trade deny`: accetta o rifiuta. La richiesta scade dopo 60 secondi.

Ogni giocatore modifica solo la propria riga di offerta. Entrambi devono confermare; ogni modifica annulla le conferme. Shift-click, trascinamento, scambio con hotbar e raccolta multipla sono disabilitati nel menu di scambio. La chiusura del menu, disconnessione, morte, allontanamento o arresto del server annullano lo scambio e restituiscono gli oggetti. Con inventario pieno, gli oggetti rimanenti vengono lasciati a terra.

Le richieste e gli scambi sono temporanei; non richiedono migrazione di file Paper. I nodi Bukkit dei plugin di permessi devono essere configurati separatamente sul server Fabric.

## Verifica

Due test automatici controllano tutti i tipi di operazione del menu e la separazione delle offerte. La prova sul server 26.2 verifica richiesta/accettazione, accesso agli slot, consegna reciproca e restituzione dopo annullamento.
