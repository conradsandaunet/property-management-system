# Todo

- [ ] Sette opp `JWT_SECRET` (og evt. `BOARD_SEED_PASSWORD`) som ekte miljøvariabler i stedet for default-verdien i `resident-service/application.yml`, når/hvis `resident-service` containeriseres (egen `Dockerfile` + `environment:`-blokk i `docker-compose.yml`).
- [ ] Vurdere å gjøre `apartment_id` nullable på `Resident` hvis dere trenger en board/manager-konto som ikke bor i bygget (egen migrasjon + fjerne `nullable = false` på `@JoinColumn`).
