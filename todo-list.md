# Todo

- [ ] Sette opp `JWT_SECRET` (og evt. `BOARD_SEED_PASSWORD`) som ekte miljøvariabler i stedet for default-verdien i `resident-service/application.yml`, når/hvis `resident-service` containeriseres (egen `Dockerfile` + `environment:`-blokk i `docker-compose.yml`).
- [ ] Vurdere å gjøre `apartment_id` nullable på `Resident` hvis vi trenger en board/manager-konto som ikke bor i bygget (egen migrasjon + fjerne `nullable = false` på `@JoinColumn`).
- [ ] Legge til `@RestControllerAdvice` i `resident-service` for å håndtere uventede exceptions ordentlig (f.eks. `DataIntegrityViolationException` ved duplikat e-post → `409` i stedet for rå `500`).
- [ ] Legge til Bean Validation (`@Valid` + `@NotBlank`/`@Email` osv.) på `LoginRequest` og `ResidentCreateRequest` — nullfelter i requesten gir i dag NPE/500 i stedet for en ren `400`.
- [ ] Logge feilslåtte token-valideringer i `JwtAuthFilter` (i dag svelges `JwtException`/`IllegalArgumentException` helt stille — ingen synlighet på ugyldige/utløpte tokens, i motsetning til feilet login som logges).
- [ ] Vurdere å trekke ut delt JWT-valideringskode (`JwtAuthFilter`/`SecurityConfig`-mønsteret) til en egen Maven-modul (`security-common`) før det kopieres inn i maintenance-/billing-/reservation-service, for å unngå å duplisere det tre ganger.
