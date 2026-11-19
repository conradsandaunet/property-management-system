# Eksamenssjekkliste (PG3402)

Status: DONE = ferdig, PARTIAL = delvis eller må verifiseres, TODO = ikke gjort.

Sist oppdatert etter lastbalansering via Consul, sentral konfigurasjon (Consul KV), demodata, frontend for billing og reservation og varsling fra Kafka-eventet. Hold denne oppdatert når noe endres.

## Mikrotjeneste-krav

### Kreves for E

- [x] **1. Flere tjenester med ulik funksjonalitet som kommuniserer** (DONE)
  resident, billing, maintenance og reservation, pluss gateway og frontend.
- [x] **2. Minst to tjenester med synkron kommunikasjon** (DONE)
  billing -> resident (`ResidentServiceClient`, REST med service-token).
  Beskriv i rapporten: hvorfor billing trenger beboerdata, og hvordan tokenet lages.
- [x] **3. Minst to tjenester med asynkron kommunikasjon (event-drevet)** (DONE)
  Kafka (KRaft-broker i compose). maintenance publiserer `MaintenanceCompleted` på topicen
  `maintenance.completed` (etter at databaseendringen er lagret), og resident-service konsumerer den.
  - [x] Effekten er synlig: resident-service lagrer en varsling, og beboeren ser den i frontend
  - [x] Hendelsesflyten er dokumentert i `docs/architecture.md` (sekvensdiagram, topic, felt, produsent og konsument)
  - [ ] Nevne i refleksjonen: hendelsen kan gå tapt hvis Kafka er nede akkurat ved publisering (ingen outbox/retry)

### Kreves for D

- [ ] **4. Hver tjeneste har tydelig struktur og funksjonalitet** (PARTIAL)
  Resident har lagdelt struktur (controller -> service -> repository, DTO-er, tester).
  Sjekk at billing, maintenance og reservation følger samme mønster, og at ansvaret til hver er tydelig.
  Tjenestene har lite tester utenom resident (maintenance har statustester, de andre bare `contextLoads`).
- [ ] **5. Arkitekturen stemmer med dokumentasjonen** (PARTIAL)
  `docs/architecture.md` beskriver event-flyten (Kafka). `README.md` har tjenestetabell og gateway-seksjon.
  Det mangler fortsatt en samlet oversikt (alle tjenester, ansvar, struktur, hvem som kaller hvem,
  sync/async, porter, databaser, Consul).
  - [ ] Arkitekturdiagram (nettleser -> nginx -> gateway -> tjenester -> databaser, + Kafka og Consul)
  - [ ] Beskrive hver tjeneste: ansvar, endepunkter, database, avhengigheter
  - [ ] Gå gjennom at README/docs stemmer med koden når alt er ferdig
- [x] **6. Kan deployes som Docker-containere** (DONE)
  Én `Dockerfile` for alle Spring-moduler, nginx-image for frontend, `docker compose up --build`.

### Kreves for C

- [x] **7. Én inngang (gateway) som ruter kall** (DONE)
  Spring Cloud Gateway (Web MVC) på :8080. Ruting, CORS, JWT-validering, rate limiting på login.
- [x] **8. Gateway som i tillegg gjør lastbalansering** (DONE)
  Alle fire rutene bruker `lb://<tjenestenavn>` (Spring Cloud LoadBalancer + Consul), og gatewayen
  har ingen faste tjeneste-URL-er. Helsesjekkene i Consul brukes (`query-passing`), og cachen er satt
  til 5 s, så en død instans slutter å få trafikk etter noen sekunder. Testet med
  `docker compose up -d --scale reservation-service=2`: to instanser i Consul, trafikken fordeles.
  - [x] Demoen er beskrevet i README (kommando, Consul-UI, `docker compose logs -f reservation-service`, stoppe en instans)

### Kreves for B

- [x] **9. Sentral kontroll av tjenestenes helse** (DONE)
  Alle tjenester og gatewayen har `/actuator/health` (inkl. databasestatus). Consul kjører helsesjekk
  mot hver tjeneste, og UI-et på `http://localhost:8500` viser dem på ett sted. Compose bruker
  det samme til oppstartsrekkefølge.
  - [ ] Nevne i README og arkitektur hvor man ser helsen (Consul-UI)
- [ ] **10. Hele prosjektet kan startes med `docker compose up`, klart for CI/CD** (PARTIAL)
  `docker compose up --build` starter alt. GitHub Actions kjører tester (backend med Postgres,
  frontend lint/build) og er grønn på `main`. Ruleset `run-tests` på `main` krever at `backend` og
  `frontend` er grønne før merge, og blokkerer force-push og sletting av `main`, så ingenting uten
  grønne tester kommer inn i `main`.
  - [x] Kreve grønn CI for merge (ruleset `run-tests`, aktiv)
  - [ ] Teste `docker compose up` fra et rent oppsett (ny klone, bare `.env`) før levering
  - [ ] Eventuelt et CD-steg (bygge og publisere images) hvis dere vil vise det

### Kreves for A

- [x] **11. Sentral konfigurasjon for tjenestene** (DONE)
  Consul KV. Filene i `consul-config/` (`application.yml` felles, `gateway.yml` for gatewayen) lastes
  inn av `consul-config`-containeren ved oppstart (`config/<navn>/data`), og alle fem tjenestene leser dem via
  `spring.config.import: "optional:consul:"`. I Consul ligger JWT-utløpstid, Kafka-topicnavn, demodata-flagg
  og rate limit på login. Hemmeligheter (`JWT_SECRET`, databasepassord) ligger bevisst i `.env`, ikke i KV.
  Endringer i KV krever restart av tjenesten. Verifisert på gatewayen (verdi fra Consul overstyrte fallback).
  - [x] Dokumentert i README: hva som ligger i Consul, hva som er miljøvariabler og hvorfor
  - [x] Verifisert i hele compose-stakken (alle tjenester `healthy`)
- [x] **12. Kan deployes med flere instanser av relevante tjenester** (DONE)
  De fire tjenestene (reservation, resident, billing, maintenance) kan kjøre som flere instanser:
  `docker compose up -d --scale <tjeneste>=N`. Gatewayen fordeler lasten via Consul, og
  `scripts/scaling-demo.sh` viser fordelingen (header `X-Served-By`, målt 20/20/20 og 30/30).
  Seeding bruker en Postgres advisory lock (testet med ti instanser som starter samtidig),
  dobbeltbooking og dobbel fakturering stoppes av databasen, og Kafka-temaet har tre partisjoner
  så resident-instansene deler hendelsene (ett varsel per hendelse).
  - [x] Seeding i alle fire tjenestene tåler at flere instanser starter samtidig (`SeedLock`, med test)
  - [x] Samtidige bookinger og fakturagenerering er trygge på tvers av instanser
  - [x] Demo og målinger er dokumentert i README (fordeling, hva som skjer når en instans dør: ca. 5 s)
  - [x] Begrensninger er dokumentert: gatewayen kjører som én instans (rate limit i minnet), én konsument per partisjon (3), manuell skalering
  - [ ] Kjør `scripts/scaling-demo.sh` mot Docker-stakken fra en ren klone før levering

## Oppgavespesifikke krav

Funksjonaliteten vurderes mot arbeidskravet. Brukerhistoriene står i [`requirements.md`](requirements.md).
Prioritet i parentes: must-have, should-have, nice-to-have.

### Status per tjeneste

- [x] **Resident:** alle must-have/should-have-historier ferdig, inkl. leiligheter (legge til, endre, fjerne), å legge til/flytte beboere fra leilighetssiden og bilder av leiligheter (opplasting og sletting, lagret i resident-db). og flere bygninger (legge til, endre navn, fjerne, og velge bygning per leilighet)
- [ ] **Maintenance:** beboer oppretter og følger forespørsler, board tildeler arbeider, arbeider logger inn, ser egne oppdrag og endrer status (frontend finnes), `MaintenanceCompleted`-event publiseres
  - [ ] Merk av arbeider-historiene og event-historien i `requirements.md` (de er laget men står som ikke krysset av)
  - [ ] Should-have: kommentarer (beboer og arbeider), board overstyrer prioritet/status
- [ ] **Billing:** beboer ser og betaler faktura og ser historikk, faktura-PDF finnes. Backend har generering, betalingsoversikt og "marker betalt" for board (`/generate`, `/overview`, `/mark-paid`)
  - [ ] Verifiser at de tre board-historiene fungerer og kryss dem av i `requirements.md`
  - [x] Frontend finnes (`InvoicesPage` for beboer, `BoardInvoicesPage` for board)
- [ ] **Reservation:** backend har tilgjengelighet, booking, avbestilling og blokkering av ressurs (`/block`)
  - [ ] Verifiser "blokker ressurs" og kryss av i `requirements.md`
  - [x] Frontend finnes (`BookPage`, `MyReservationsPage`, `BoardResourcesPage`)
  - [x] Dobbeltbooking sikret i databasen

### Frontend-dekning (sensor skal kunne se funksjonaliteten)

- [x] Resident: innlogging, husstand, kontaktinfo, oversikt, beboere, leiligheter
- [x] Maintenance: beboer, board og arbeider
- [x] Billing (beboer og board)
- [x] Reservation (beboer og board)
- [x] Varsler fra Kafka-eventet (beboer)
- [ ] Gå gjennom at billing- og reservation-sidene fungerer mot seedede data (som board og beboer)
- [ ] Beslutte hva som kuttes (nice-to-have: fullførte oppdrag for arbeider) og begrunne i refleksjonsdokumentet

### Dokumenter som skal leveres

- [ ] **Refleksjonsdokument** som forklarer:
  - [ ] Hvilke brukerhistorier som ble utelatt og hvorfor, og hvordan dere valgte hva som skulle bort
  - [ ] Funksjonalitet lagt til utover arbeidskravet
  - [ ] Arbeidsmengde/prosess (krav, prioritering, aktiviteter rundt prosjektet)
- [ ] **README.md** der sensor kan se all funksjonalitet og kjøre prosjektet:
  - [ ] Hvordan starte alt (`.env`, `docker compose up --build`) og åpne appen
  - [ ] Innloggingsbrukere for demo (board, beboer og arbeider), inkludert seed-data. Demodata finnes nå
    (6 leiligheter, 8 beboere, fakturaer, vedlikeholdssaker, ressurser og bookinger, slås av med `SEED_DEMO_DATA=false` / `app.seed.demo-data`),
    men README beskriver fortsatt bare den gamle demofakturaen. Demobeboere bruker passordet `demo-resident-pw` (`RESIDENT_SEED_PASSWORD`)
  - [ ] Gjennomgang av funksjonaliteten per tjeneste og rolle
  - [ ] Hvordan kjøre testene, og hvor man ser Consul og Kafka
- [ ] Alt som trengs for å bygge og kjøre er med: compose-fil, `.env.example`, seed-data, oppsettskript

### Kjøre-og-levere-sjekk (rett før innlevering)

- [ ] Klone repoet på nytt i en tom mappe og følge README fra start til slutt
- [ ] `docker compose up --build` fra ren tilstand: alle containere `healthy`, alle tjenester synlige i Consul
- [ ] Gå gjennom hver brukerhistorie i appen som board, beboer og arbeider
- [ ] `./mvnw clean test` er grønn lokalt, og CI er grønn på `main`
- [ ] Ingen hemmeligheter committet (`.env` er gitignored, bare `.env.example`)

## Foreslått rekkefølge

1. **Verifisere hele stakken** etter alle endringene: `docker compose up --build` fra ren tilstand, alle containere `healthy`, Consul viser tjenester og konfig.
2. **Arkitekturdokumentasjon** (punkt 4-5): én samlet oversikt over tjenester, kall, Consul og porter i `docs/architecture.md`.
3. **README**: oppstart, demobrukere, hvor man ser Consul/Kafka, `--scale`-demo, hva som ligger i Consul.
4. **Rydde `requirements.md`**: verifiser og kryss av worker-, billing-board- og reservation-blokker-historiene.
5. **Refleksjonsdokument** (utelatte historier, ekstra funksjonalitet, tapt Kafka-event ved nedetid, in-memory rate limit, arbeidsprosess).
6. Kjøre-og-levere-sjekken i en ny klone.
