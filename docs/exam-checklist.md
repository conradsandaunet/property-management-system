# Eksamenssjekkliste (PG3402)

Status: DONE = ferdig, PARTIAL = delvis eller må verifiseres, TODO = ikke gjort.

Sist oppdatert etter apartments-CRUD, gateway, Docker og CI. Hold denne oppdatert når noe endres.

## Mikrotjeneste-krav

### Kreves for E

- [x] **1. Flere tjenester med ulik funksjonalitet som kommuniserer** (DONE)
  resident, billing, maintenance og reservation, pluss gateway og frontend.
- [x] **2. Minst to tjenester med synkron kommunikasjon** (DONE)
  billing -> resident (`ResidentServiceClient`, REST med service-token).
  Beskriv i rapporten: hvorfor billing trenger beboerdata, og hvordan tokenet lages.
- [ ] **3. Minst to tjenester med asynkron kommunikasjon (event-drevet)** (TODO)
  Ikke startet. Plan: Kafka, `MaintenanceCompleted` fra maintenance, consumer i resident-service
  (varsle beboeren) eller egen notification-service.
  - [ ] Avtale event-kontrakten med Conrad (`maintenance.completed`: `requestId`, `residentId`, `apartmentId`, `completedAt`)
  - [ ] Arbeider-flyten i maintenance (se egne oppdrag, Accept -> Start -> Complete) som event-kilden trenger
  - [ ] Kafka-broker (KRaft) i `docker-compose.yml` med healthcheck
  - [ ] Producer i maintenance, consumer i valgt tjeneste
  - [ ] Dokumentere hendelsesflyten i arkitekturen

### Kreves for D

- [ ] **4. Hver tjeneste har tydelig struktur og funksjonalitet** (PARTIAL)
  Resident har lagdelt struktur (controller -> service -> repository, DTO-er, tester).
  Sjekk at billing, maintenance og reservation følger samme mønster, og at ansvaret til hver er tydelig.
- [ ] **5. Arkitekturen stemmer med dokumentasjonen** (PARTIAL)
  `README.md` har tjenestetabell og gateway-seksjon, men det mangler et arkitekturdokument
  (alle tjenester, ansvar, struktur, hvem som kaller hvem, sync/async, porter, databaser).
  - [ ] Arkitekturdiagram (nettleser -> nginx -> gateway -> tjenester -> databaser, + Kafka)
  - [ ] Beskrive hver tjeneste: ansvar, endepunkter, database, avhengigheter
  - [ ] Gå gjennom at README/docs stemmer med koden når alt er ferdig
- [x] **6. Kan deployes som Docker-containere** (DONE)
  Én `Dockerfile` for alle Spring-moduler, nginx-image for frontend, `docker compose up --build`.

### Kreves for C

- [x] **7. Én inngang (gateway) som ruter kall** (DONE)
  Spring Cloud Gateway (Web MVC) på :8080. Ruting, CORS, JWT-validering, rate limiting på login.
- [ ] **8. Gateway som i tillegg gjør lastbalansering** (TODO)
  Gatewayen peker i dag på ett fast URL per tjeneste.
  - [ ] Kjøre flere instanser av en tjeneste (`docker compose up --scale ...` / `replicas`)
  - [ ] Få gatewayen til å fordele mellom dem (f.eks. `lb://`-ruter, eller DNS-basert round-robin via Docker-nettverket)
  - [ ] Vise det i en demo (kall flere ganger, se ulike instanser svare)
  - Se punkt 12 for hindringene.

### Kreves for B

- [ ] **9. Sentral kontroll av tjenestenes helse** (PARTIAL)
  Alle tjenester og gatewayen har `/actuator/health` (inkl. DB-status), og compose bruker det til
  healthchecks og oppstartsrekkefølge. Mangler en sentral oversikt.
  - [ ] Velg og dokumenter hvordan "sentralt" løses (f.eks. gatewayen aggregerer helse fra alle tjenester, eller et enkelt `/api/health`-endepunkt/Spring Boot Admin)
- [ ] **10. Hele prosjektet kan startes med `docker compose up`, klart for CI/CD** (PARTIAL)
  `docker compose up --build` starter alt. GitHub Actions kjører tester (backend med Postgres, frontend lint/build) på push og PR.
  - [ ] Kreve grønn CI for merge (branch protection / ruleset, krever admin på repoet)
  - [ ] Få CI grønn på `main` (Docker-PR-en inneholder rettelsene)
  - [ ] Teste `docker compose up` fra et rent oppsett (ny klone, bare `.env`) før levering
  - [ ] Eventuelt et CD-steg (bygge og publisere images) hvis dere vil vise det

### Kreves for A

- [ ] **11. Sentral konfigurasjon for tjenestene** (TODO)
  I dag: miljøvariabler per tjeneste via compose og rot-`.env`. Ikke en egen config-tjeneste.
  - [ ] Velge løsning (Consul KV, Spring Cloud Config Server, ...) og begrunne valget
  - [ ] Flytte felles konfig (JWT-hemmelighet, URL-er, grenser) dit
  - [ ] Dokumentere
- [ ] **12. Kan deployes med flere instanser av relevante tjenester** (TODO)
  Ikke testet. Kjente hindringer:
  - [ ] Seeding ved oppstart (board-bruker, demo-arbeidere, demo-faktura) er ikke trygg med flere kopier som starter samtidig
  - [ ] Rate limiting på login er in-memory per gateway-instans (egen/delt teller trengs hvis gatewayen skaleres)
  - [ ] Velge en tjeneste å demonstrere med (reservation er enklest), fjerne `container_name`/port-konflikter, vise at to instanser deler last

## Oppgavespesifikke krav

Funksjonaliteten vurderes mot arbeidskravet. Brukerhistoriene står i [`requirements.md`](requirements.md).
Prioritet i parentes: must-have, should-have, nice-to-have.

- [x] Resident-tjenesten: alle must-have/should-have-historier ferdig (inkl. leiligheter: legge til, endre, fjerne, og legge til/flytte beboere derfra)
- [ ] **Billing (board):** generere månedlige fakturaer (must-have), betalingsoversikt for bygget (must-have), manuelt markere betalt (should-have)
- [ ] **Maintenance (arbeider):** egne oppdrag (must-have), endre status Accept -> Start -> Complete (must-have), `MaintenanceCompleted`-event (must-have, knyttet til punkt 3)
- [ ] Maintenance (should-have): kommentarer (beboer og arbeider), board kan overstyre prioritet/status
- [ ] Reservation: "blokker ressurs" (should-have) ser ut til å være laget (PR #24) men er ikke avkrysset i `requirements.md`. Verifiser og kryss av.
- [ ] Beslutte hva som kuttes (nice-to-have: bilder av leiligheter, flere bygninger) og begrunne i refleksjonsdokumentet

### Dokumenter som skal leveres

- [ ] **Refleksjonsdokument** som forklarer:
  - [ ] Hvilke brukerhistorier som ble utelatt og hvorfor, og hvordan dere valgte hva som skulle bort
  - [ ] Funksjonalitet lagt til utover arbeidskravet
  - [ ] Arbeidsmengde/prosess (krav, prioritering, aktiviteter rundt prosjektet)
- [ ] **README.md** der sensor kan se all funksjonalitet og kjøre prosjektet:
  - [ ] Hvordan starte alt (`.env`, `docker compose up --build`) og åpne appen
  - [ ] Innloggingsbrukere for demo (board og beboer), inkludert seed-data
  - [ ] Gjennomgang av funksjonaliteten per tjeneste og rolle
  - [ ] Hvordan kjøre testene
- [ ] Alt som trengs for å bygge og kjøre er med: compose-fil, `.env.example`, seed-data, oppsettskript

### Kjøre-og-levere-sjekk (rett før innlevering)

- [ ] Klone repoet på nytt i en tom mappe og følge README fra start til slutt
- [ ] `docker compose up --build` fra ren tilstand: alle containere `healthy`
- [ ] Gå gjennom hver brukerhistorie i appen som board, beboer og arbeider
- [ ] `./mvnw clean test` er grønn lokalt, og CI er grønn på `main`
- [ ] Ingen hemmeligheter committet (`.env` er gitignored, bare `.env.example`)

## Foreslått rekkefølge

1. **Async med Kafka** (punkt 3): E-krav, og krever arbeider-flyten i maintenance først.
2. **Arkitekturdokumentasjon** (punkt 4-5): D-krav, kan gjøres parallelt.
3. **Lastbalansering og flere instanser** (punkt 8 og 12): C og A, bygger på rydding av seeding.
4. **Sentral helse og konfigurasjon** (punkt 9 og 11).
5. **Resterende brukerhistorier**, refleksjonsdokument og README-gjennomgang.
