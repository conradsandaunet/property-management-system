# Eksamenssjekkliste (PG3402)

Status: DONE = ferdig, PARTIAL = delvis eller må verifiseres, TODO = ikke gjort.

Sist oppdatert etter Kafka-event, arbeider-flyten, Consul-registrering. Hold denne oppdatert når noe endres.

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
  - [ ] Gjør effekten synlig: consumeren logger i dag bare "Would notify". Lagre en varsling i
    resident-service og vis den for beboeren i frontend, så sensor ser hendelsen virke
  - [ ] Dokumentere hendelsesflyten i arkitekturen (topic, hendelsens felt, hvem som produserer og konsumerer)
  - [ ] Nevne i refleksjonen: hendelsen kan gå tapt hvis Kafka er nede akkurat ved publisering (ingen outbox/retry)

### Kreves for D

- [ ] **4. Hver tjeneste har tydelig struktur og funksjonalitet** (PARTIAL)
  Resident har lagdelt struktur (controller -> service -> repository, DTO-er, tester).
  Sjekk at billing, maintenance og reservation følger samme mønster, og at ansvaret til hver er tydelig.
  Tjenestene har lite tester utenom resident (maintenance har statustester, de andre bare `contextLoads`).
- [ ] **5. Arkitekturen stemmer med dokumentasjonen** (PARTIAL)
  `README.md` har tjenestetabell og gateway-seksjon, men det mangler et arkitekturdokument
  (alle tjenester, ansvar, struktur, hvem som kaller hvem, sync/async, porter, databaser, Consul, Kafka).
  - [ ] Arkitekturdiagram (nettleser -> nginx -> gateway -> tjenester -> databaser, + Kafka og Consul)
  - [ ] Beskrive hver tjeneste: ansvar, endepunkter, database, avhengigheter
  - [ ] Gå gjennom at README/docs stemmer med koden når alt er ferdig
- [x] **6. Kan deployes som Docker-containere** (DONE)
  Én `Dockerfile` for alle Spring-moduler, nginx-image for frontend, `docker compose up --build`.

### Kreves for C

- [x] **7. Én inngang (gateway) som ruter kall** (DONE)
  Spring Cloud Gateway (Web MVC) på :8080. Ruting, CORS, JWT-validering, rate limiting på login.
- [ ] **8. Gateway som i tillegg gjør lastbalansering** (TODO)
  Grunnlaget er lagt: alle tjenester og gatewayen registrerer seg i Consul. Gatewayen bruker
  fortsatt faste `*_URL`-variabler.
  - [ ] Få gatewayen til å rute via Consul (`lb://<tjenestenavn>`). Støtten i Web MVC-varianten er
    ikke bekreftet, så prøv med én rute først (reservation)
  - [ ] Kjøre flere instanser av en tjeneste (`docker compose up --scale ...` / `replicas`)
  - [ ] Vise det i en demo (kall flere ganger, se ulike instanser svare)
  - Se punkt 12 for hindringene.

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

- [ ] **11. Sentral konfigurasjon for tjenestene** (TODO)
  I dag: miljøvariabler per tjeneste via compose og rot-`.env`. Consul brukes foreløpig bare til
  registrering og helse, ikke til konfigurasjon.
  - [ ] Legge inn felles konfig i Consul KV (importer-container som laster YAML ved oppstart, slik læreren gjør)
  - [ ] Få tjenestene til å lese den (`spring-cloud-starter-consul-config`, `spring.config.import=optional:consul:`)
  - [ ] Dokumentere hva som ligger i Consul og hva som fortsatt er miljøvariabler (hemmeligheter bør ikke ligge i KV i klartekst)
- [ ] **12. Kan deployes med flere instanser av relevante tjenester** (TODO)
  Ikke testet. Kjente hindringer:
  - [ ] Seeding ved oppstart (board-bruker, demo-arbeidere, demo-faktura) er ikke trygg med flere kopier som starter samtidig
  - [ ] Rate limiting på login er in-memory per gateway-instans (hold gatewayen på én instans)
  - [ ] Velge en tjeneste å demonstrere med (reservation er enklest), fjerne `container_name`/port-konflikter, vise at to instanser deler last

## Oppgavespesifikke krav

Funksjonaliteten vurderes mot arbeidskravet. Brukerhistoriene står i [`requirements.md`](requirements.md).
Prioritet i parentes: must-have, should-have, nice-to-have.

### Status per tjeneste

- [x] **Resident:** alle must-have/should-have-historier ferdig, inkl. leiligheter (legge til, endre, fjerne) og å legge til/flytte beboere fra leilighetssiden
- [ ] **Maintenance:** beboer oppretter og følger forespørsler, board tildeler arbeider, arbeider logger inn, ser egne oppdrag og endrer status (frontend finnes), `MaintenanceCompleted`-event publiseres
  - [ ] Merk av arbeider-historiene og event-historien i `requirements.md` (de er laget men står som ikke krysset av)
  - [ ] Should-have: kommentarer (beboer og arbeider), board overstyrer prioritet/status
- [ ] **Billing:** beboer ser og betaler faktura og ser historikk, faktura-PDF finnes. Backend har generering, betalingsoversikt og "marker betalt" for board (`/generate`, `/overview`, `/mark-paid`)
  - [ ] Verifiser at de tre board-historiene fungerer og kryss dem av i `requirements.md`
  - [ ] Frontend mangler helt for billing (ingen sider for fakturaer)
- [ ] **Reservation:** backend har tilgjengelighet, booking, avbestilling og blokkering av ressurs (`/block`)
  - [ ] Verifiser "blokker ressurs" og kryss av i `requirements.md`
  - [ ] Frontend mangler helt for reservation

### Frontend-dekning (sensor skal kunne se funksjonaliteten)

- [x] Resident: innlogging, husstand, kontaktinfo, oversikt, beboere, leiligheter
- [x] Maintenance: beboer, board og arbeider
- [ ] Billing (beboer og board)
- [ ] Reservation (beboer og board)
- [ ] Beslutte hva som kuttes (nice-to-have: bilder av leiligheter, flere bygninger, fullførte oppdrag for arbeider) og begrunne i refleksjonsdokumentet

### Dokumenter som skal leveres

- [ ] **Refleksjonsdokument** som forklarer:
  - [ ] Hvilke brukerhistorier som ble utelatt og hvorfor, og hvordan dere valgte hva som skulle bort
  - [ ] Funksjonalitet lagt til utover arbeidskravet
  - [ ] Arbeidsmengde/prosess (krav, prioritering, aktiviteter rundt prosjektet)
- [ ] **README.md** der sensor kan se all funksjonalitet og kjøre prosjektet:
  - [ ] Hvordan starte alt (`.env`, `docker compose up --build`) og åpne appen
  - [ ] Innloggingsbrukere for demo (board, beboer og arbeider), inkludert seed-data
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

1. **Gatewayen via Consul og flere instanser** (punkt 8 og 12): C og A, bygger på Consul-registreringen som nå er på plass.
2. **Sentral konfigurasjon i Consul** (punkt 11): A, samme integrasjon.
3. **Arkitekturdokumentasjon** (punkt 4-5): D-krav, kan gjøres parallelt.
4. **Frontend for billing og reservation**, og synlig varsling fra Kafka-eventet.
5. **Rydde `requirements.md`**, refleksjonsdokument og README-gjennomgang.
