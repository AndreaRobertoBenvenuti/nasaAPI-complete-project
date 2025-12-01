# Riflessioni Personali e Implementazioni Future - NASA Dashboard

---

## 1. Riflessione Personale

Prima di iniziare questo test tecnico, la mia esperienza con sistemi full-stack era principalmente accademica. Avevo costruito piccoli progetti personali, come si può vedere dalle mie repository su GitHub, ma mai qualcosa di così strutturato e completo come questo progetto.

---

### 1.1 Lezioni Apprese

#### Lezione 1: Data Quality È Più Importante Della Quantità

I dati provenienti da API esterne sono **spesso incompleti o inconsistenti**.
- Molti campi `null` 
- Formati di date diversi tra API diverse

Senza una gestione robusta degli errori, il codice si rompe nel modo peggiore: **silenziosamente**. Magari salvi dati corrotti nel database e te ne accorgi solo dopo.  
Il controllo del tipo di dato atteso è fondamentale. Risultato: grande perdita di tempo per un errore banale che poteva essere intercettato con una validazione iniziale.

---

#### Lezione 2: Performance Optimization È User Experience

Un sistema lento equivale a un sistema che non funziona.  
Per come ho implementato il caricamento dati, ho un server backend che:
1. Salva i dati in PostgreSQL
2. Espone API REST
3. Il frontend fa richieste per ogni schermata
 
Più la pagina è ricca di informazioni, più grande è la richiesta, più tempo richiede. 
L'**Analysis Screen** faceva 8 richieste simultanee e andava in timeout dopo 30 secondi.

**Soluzione adottata:**
- **Cache (Caffeine)**: Le correlazioni cambiano poco, non ha senso ricalcolarle ogni volta
- **Lazy loading**: Carico i dati solo quando l'utente apre il tab specifico, non tutto all'inizio


La performance non è un "nice-to-have" — è una **funzionalità fondamentale**. Un sistema che non risponde entro 5 secondi viene abbandonata.

---

#### Lezione 3: La Documentazione è anche per Me Stesso

Il codice dice cosa fa, i commenti dicono perché l'ho fatto in quel modo. Utile per chi guarderà il codice in futuro.

---

#### Lezione 4: I Dati Reali Sono Sempre Sorprendenti

Mi aspettavo che il campo `linkedEvents` della NASA avrà tutte le correlazioni già pronte, mentre
pochi dei CME aveva riferimenti a geomagnetic storms nel campo `linkedEvents`. Mi aspettavo almeno il 70-80%.

I `linkedEvents` probabilmente vengono popolati manualmente o semi-automaticamente dal team NASA. Molti eventi non vengono collegati perché:
- La detection di IPS (Interplanetary Shock) è difficile
- Non tutti i CME vengono monitorati fino alla Terra
- L'associazione richiede conferma che spesso non c'è

Quindi ho deciso di implementare un doppio approccio:

1. **NASA Verified**: Usa `linkedEvents` — 100% accurato ma bassa coverage
2. **Manual Temporal**: Usa finestre temporali basate sulle informazioni fisiche da me ottenute — ~70% accurato ma alta coverage

Il sistema è più robusto e trasparente (l'utente vede entrambi i metodi).

---

### 1.2 Cosa Rifarei Diversamente

#### 1. Database Design: Schema Prima, Codice Dopo

Ho iniziato a scrivere codice subito, "per capire quale fosse la sfida più difficile". Schema database definito progressivamente.

Piu modifiche dello schema. Ogni volta che lo modificavo perdevo tempo a risolvere problemi che si verificavano a cascata.

**Cosa avrei dovuto fare:**  
Dedicare mezza giornata a progettare lo schema completo **prima** di scrivere una riga di codice:
1. Studiare tutte le 5 API NASA (struttura JSON)
2. Identificare entities e relationships
3. Disegnare ER diagram su carta
4. **Poi** iniziare a implementare

**"hours of planning save days of coding"**  
Quando si deve progettare una buona gestione del dato e la sua analisi, è assolutamente vero.

La mia scelta di "imbattermi prima nel codice" non è stata completamente sbagliata (mi ha fatto capire la complessità), ma **nemmeno ottimale** — ho sprecato tempo prezioso.

---

#### 2. DTO Pattern

**Situazione attuale:**  
Non sto usando DTO. Espongo le Entity direttamente al frontend, anche se ho preparato una classe dedicata.

**Problema:**  
Il frontend riceve **tutte** le informazioni, anche quelle strutturali o interne:
- Campi `rawData` (JSON completo, inutile per UI)
- Relazioni `apiSource` (causano circular reference)
- ID interni del database

**Perché è un problema:**
1. **Sicurezza**: Espongo più informazioni del necessario
2. **Performance**: Payload JSON più grande (spreco banda)
3. **Coupling**: Cambio Entity → devo cambiare frontend

**Miglioramento futuro:**  
Implementare DTO layer per mostrare al frontend **solo le informazioni necessarie**. Avrò anche una migliore gestione di permessi e sicurezza.

---

#### 3. Testing: Oltre i Test Accademici Inutili

**La mia esperienza con i test:**  
Molto spesso la cartella `test/` viene riempita di test **inutili**.

Esempio:
```java
@Test
public void testGetName() {
    User user = new User("Mario");
    assertEquals("Mario", user.getName());
}
```

**Obiettivo ufficiale:** Verificare che il codice funzioni.  
**Obiettivo reale:** Gonfiare la test coverage al 90%+ per le statistiche.

**Cosa ho fatto in questo progetto:**  
Testing manuale di tutti i casi possibili:
-  Ogni schermata funziona?
-  Filtri applicano correttamente?
-  API gestiscono errori?

Questo approccio su progetti piu importanti e reali. Con 50+ endpoint e 100+ scenari, il testing manuale diventa impossibile.

Durante il progetto di ING SOFTW abbiamo utilizzato JUnit per verificare i casi di test.

---

#### 4. Git Strategy: 

Durante lo sviluppo del progetto ho committato sempre sullo stesso branch (`master`), diverso dal `main`, senza creare branch separati per ogni feature.
Non avevo necessità di gestire branch multipli.

Sarebbe utile in futuro (non per questo progetto, ma per me come sviluppatore) capire come utilizzare correttamente i version-control. Non utilizzarlo solo come "Salva con nome" che ti permette di tornare indietro.

---

#### 5. La Grafica È Importante, Ma Non È Tutto

Mi sono focalizzato troppo su dettagli grafici, quando avrei potuto investire quel tempo in altro,
come nuove correlazioni o analisi, sicuramente piu utili.

Ho trovato questa "regola" online:
**80/20 rule**: Il primo 80% della qualità grafica richiede il 20% del tempo. L'ultimo 20% richiede l'80% del tempo.

---

### 1.3 Competenze Acquisite

#### Tecnologie Apprese/Migliorate

**Flutter (Dart)**
- Dashboard con 5 screens, state management, charts interattivi

**SQL e Database Design**
- Sto seguendo il corso universitario, conoscenza basilare
- Mi sono aiutato con l'AI per query complesse, ma ho compreso la logica 

**PgAdmin 4**
- Scoperta: Visualizzare e interrogare database graficamente
- Utilizzo: Debugging query, verifica integrità dati

**API Integration**
- Autenticazione con API key
- Gestione rate limits
- Parsing JSON complessi (nested objects)
- Robusta gestione degli errori (timeout, retry)

**Data Visualization**
- Libreria fl_chart per Flutter
- Stacked bar, pie, line charts
- Interattività (touch tooltips)

---

### 1.4 Bilancio Finale

Il sistema ha:
1. ✅ Integrato 5 API NASA diverse
2. ✅ Correlato eventi spaziali con approccio scientifico
3. ✅ Presentato insights in modo comprensibile 
4. ✅ Performance ottimali
5. ✅ Documentazione 
 
Sono abbastanza soddisfatto del risultato.
Backend e frontend comunicano correttamente, il database è solido, l'analisi e le correlazioni sembrano buone.

---

## 2. Implementazioni Future

#### 1. Implementazione Pattern DTO

**Problema attuale:** Entity JPA esposte direttamente, troppe informazioni al frontend.

**Soluzione:**  
Creare DTO layer con solo campi necessari per UI.

---

#### 2. Mappe Geografiche Interattive

**Obiettivo:** Visualizzare eventi su mappa mondiale.

**Features:**
- Posizione eventi con coordinate (lat/lon)
- Filtri temporali: "Tutti gli eventi di questo periodo"
- Filtri geografici: "Tutti gli eventi in questa area"

**Use case:** Vedere tutti i fireballs in Europa negli ultimi 6 mesi.

---

#### 3. Integrazione Analisi dalla Dashboard

**Idea:** Link diretti dalle correlazioni alla dashboard ufficiale NASA.

Esempio:
```
X1.1 Flare → CME-001 → Storm G4
[View on NASA DONKI] ← Click apre browser
```
esempio:
https://ssd.jpl.nasa.gov/tools/sbdb_lookup.html#/?sstr=2003361&view=OPD

Oppure, ancora meglio: dettaglio del fenomeno con tutti i dati necessari e, ad esempio, simulazione dell'orbita dell'asteroide che 

---

#### 4. Nuove Sorgenti Dati API

**NOAA - Space Weather Prediction Center**
- Solar Wind data (real-time)
- Aurora forecasts
- Radiation storm levels

**EONET - Eventi Geologici sulla Terra**
- Vulcani, Terremoti, ...
- Correlazione con eventi spaziali? (ricerca scientifica)

**Visualizzazione su Mappa**  
Overlay di eventi spaziali + geologici per vedere pattern.


---

#### 5. Analisi Predittiva 

**Obiettivo:** "Se X-flare ora → 70% probabilità storm G3+ tra 48h"

**Approccio:**
1. Feature engineering: classe flare, velocità CME, angolo
2. Training: RandomForest su dataset storico
3. Validazione: accuratezza, precisione


---

#### 6. Pattern Stagionali e Algoritmi Predittivi

**Analisi:**
- Attività solare per stagione
- Correlazioni CME speed → Storm intensity
- Hotspot geografici fireballs

**Visualizzazioni:**
- Radar chart per distribuzioni stagionali
- Heat map geografica

---

## Conclusione

Questo progetto è stato una **sfida**, ma anche un'**opportunità di crescita** enorme.

Ho imparato che costruire un sistema reale è diverso da fare esercizi accademici:
- I dati sono sporchi
- Le performance contano
- La documentazione è essenziale
- Le decisioni di design hanno conseguenze

---

**Data:** Dicembre 2025  
**Autore:** Andrea Roberto Benvenuti