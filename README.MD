# 📚 Apulia Library — Microservices Project

Ecosistema di microservizi per la gestione di una biblioteca digitale, sviluppato con **Java 21** e **Spring Boot 3.4.1**.

---

## Architettura

Il progetto e' composto da tre microservizi di business indipendenti piu' un API Gateway d'ingresso, organizzati in un progetto Maven multi-modulo:

| Servizio | Descrizione | Porta interna | Health check | Accessibile dall'esterno |
|---|---|---|---|---|---|
| API Gateway | Gateway d'ingresso e autenticazione Bearer Token | `8080` | `/actuator/health` (richiede token) | **Si'** |
| Config Server | Configurazione centralizzata (Spring Cloud Config) | `8888` | `/actuator/health` | Solo health check |
| Book Service | Gestione del catalogo libri | `8081` | `/actuator/health` | No (solo via gateway) |
| Member Service | Gestione degli utenti iscritti | `8082` | `/actuator/health` | No (solo via gateway) |
| Loan Service | Gestione dei prestiti | `8083` | `/actuator/health` | No (solo via gateway) |
| MySQL | Database relazionale | `3306` | — | No (solo rete interna Docker) |

> **Nota:** L'unico punto di ingresso e' il **API Gateway sulla porta `8080`**. I microservizi non sono direttamente raggiungibili dall'esterno.

## Health Check

Ogni microservizio espone l'endpoint `/actuator/health` di Spring Boot Actuator (verifica database, disco, ping). Il Config Server e' accessibile sulla porta `8888`, mentre per i microservizi interni l'health check e' visibile solo dalla rete Docker interna (es. da config-server). L'API Gateway richiede il token di autenticazione anche per l'health check.

---

## Tecnologie utilizzate

- Java 21
- Spring Boot 4.1.0 / 3.4.1 (API Gateway)
- Spring Cloud Gateway (API Gateway)
- Spring Boot Starter WebFlux (API Gateway)
- Spring Web (Microservizi)
- Spring Data JPA + Hibernate
- Spring Security (tutti i microservizi: CORS ristretto, CSRF disabilitato)
- Spring Cloud Config (Config Server + Client)
- Spring Retry (config client con fail-fast)
- Spring Cloud Gateway (API Gateway routing + Bearer Token filter)
- Spring Boot Actuator (health check su tutti i microservizi)
- Spring Validation
- Springdoc OpenAPI 2.8.9
- MySQL 8 (containerizzato)
- Flyway (migrazioni database)
- Bucket4j (rate limiting sul gateway)
- Lombok
- Maven (struttura multi-modulo)
- Docker & Docker Compose

---

## API Gateway (Porta `8080`)

L'**API Gateway** agisce come punto di ingresso unico per tutte le richieste esterne ed esegue il routing dinamico verso i microservizi interni. Inoltre, implementa un filtro di sicurezza Bearer Token.

### Autenticazione
Tutte le chiamate instradate tramite il Gateway sulla porta `8080` richiedono l'autenticazione tramite un header HTTP `Authorization`:
```http
Authorization: Bearer my-secret-token-123
```
In caso di token mancante o errato, il gateway restituira' `401 Unauthorized` con header `WWW-Authenticate: Bearer`.

### Rate Limiting
Il gateway applica un rate limit di **10 richieste ogni 60 secondi** per IP client. Al superamento, restituisce `429 Too Many Requests` con body JSON contenente messaggio esplicativo, `retryAfterSeconds` e `resetTime` (ISO-8601):

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please wait 60 seconds before retrying.",
  "retryAfterSeconds": 60,
  "resetTime": "2026-06-24T08:07:08.997289330Z"
}
```

### Regole di Routing

| Path Sorgente (Gateway) | Destinazione |
|---|---|
| `/book` e `/book/**` | `http://book-service:8081` |
| `/members` e `/members/**` | `http://member-service:8082` |
| `/loans` e `/loans/**` | `http://loan-service:8083` |

---

## Docker & Docker Compose

L'intero ecosistema puo' essere compilato e avviato tramite Docker. Il database MySQL e' incluso nei servizi Docker.

### Prerequisiti

Prima di avviare, creare il file `.env` nella root del progetto (una copia di `.env.example`):

```bash
cp .env.example .env
```

Il file `.env` contiene le credenziali sensibili (MySQL, token API Gateway) ed e' escluso da git.

### Build delle immagini e compilazione jar

Da root del progetto, compila i progetti con Maven e builda i container:

```bash
# Compilazione dei jar
.\mvnw.cmd package -DskipTests

# Build delle immagini docker
docker-compose build
```

### Avvio dei microservizi
Per avviare l'intera architettura:

```bash
docker-compose up -d
```

### Verifica stato dei container

```bash
docker-compose ps
```

Per vedere i log di un servizio (ad esempio l'api-gateway):

```bash
docker-compose logs api-gateway
```

### Configurazione centralizzata

Il servizio **Config Server** (porta `8888`) centralizza la configurazione di tutti i microservizi. I file YAML si trovano in `config-repo/` e sono montati come volume nel container config-server per aggiornamenti senza rebuild.

Ogni microservizio carica la configurazione all'avvio tramite `spring.config.import=configserver:http://config-server:8888` con retry automatico (fail-fast, max 10 tentativi).

La configurazione condivisa (`config-repo/application.yml`) include:
- `ddl-auto: validate` per Hibernate
- `validate-on-migrate: false` per Flyway (aggiornamenti checksum manuali)
- logging `WARN` e Swagger disabilitato
- Parametri SSL MySQL (useSSL=false, allowPublicKeyRetrieval=true) via HikariCP properties

### Stop & Cleanup

```bash
docker-compose down
# Per rimuovere anche i volumi (database compreso):
docker-compose down -v
```

---

## Book Service (Porta interna `8081` / Gateway `8080`)

Microservizio dedicato alla gestione del catalogo libri.

### Endpoints (tramite Gateway `8080` con Auth Header)

| Metodo | Endpoint | Descrizione |
|---|---|---|
| GET | `/book` | Lista libri |
| GET | `/book/{id}` | Libro per ID |
| GET | `/book/search` | Ricerca per titolo e/o autore (es. `?title=1984`) |
| POST | `/book` | Crea libro |
| PUT | `/book/{id}` | Aggiorna libro |
| PATCH | `/book/{id}` | Aggiornamento parziale (con `BookPatchDTO` validato) |
| DELETE | `/book/{id}` | Elimina libro |

---

## Member Service (Porta interna `8082` / Gateway `8080`)

Microservizio dedicato alla gestione degli utenti iscritti alla biblioteca.

### Endpoints (tramite Gateway `8080` con Auth Header)

| Metodo | Endpoint | Descrizione |
|---|---|---|
| GET | `/members` | Lista membri |
| GET | `/members/{id}` | Membro per ID |
| POST | `/members` | Crea membro (firstName, lastName, city, phone) |
| PUT | `/members/{id}` | Aggiorna membro |
| DELETE | `/members/{id}` | Elimina membro |

---

## Loan Service (Porta interna `8083` / Gateway `8080`)

Microservizio dedicato alla gestione dei prestiti. Interagisce internamente via REST con Book Service e Member Service per validare e arricchire i dati dei prestiti.

### Endpoints (tramite Gateway `8080` con Auth Header)

| Metodo | Endpoint | Descrizione |
|---|---|---|
| GET | `/loans` | Lista prestiti |
| GET | `/loans/{id}` | Prestito per ID |
| GET | `/loans/details` | Lista prestiti arricchiti con dettagli libro e membro |
| GET | `/loans/details/{id}` | Dettaglio singolo prestito arricchito |
| POST | `/loans` | Crea prestiti (multipli: `memberId` + `bookIds`) |
| PATCH | `/loans/{id}/return` | Restituisce libro (imposta returnDate) |
| DELETE | `/loans/{id}` | Elimina prestito |

---

> **Nota:** Il branch `backup` funge da backup snapshot allineato a `prod` e `dev`.

---

## Struttura progetto

```
Library-Microservice-Architecture/
├── pom.xml                    (parent multi-modulo)
├── docker-compose.yml         (configurazione orchestratore docker)
├── docker-compose.school.yml  (variante per ambiente scolastico)
├── .env.example               (template variabili d'ambiente)
├── mysql/
│   └── init.sql               (script inizializzazione database)
├── config-repo/               (configurazione centralizzata YAML)
│   ├── application.yml        (condivisa tra tutti i servizi)
│   ├── api-gateway.yml
│   ├── book-service.yml
│   ├── member-service.yml
│   └── loan-service.yml
├── config-server/             (Config Server)
│   ├── Dockerfile
│   └── src/main/
├── api-gateway/               (API Gateway del sistema)
│   ├── Dockerfile
│   └── src/main/
├── book-service/              (Catalogo libri)
│   ├── Dockerfile
│   └── src/main/
├── member-service/            (Gestione iscritti)
│   ├── Dockerfile
│   └── src/main/
└── loan-service/              (Gestione prestiti)
    ├── Dockerfile
    └── src/main/
```

---

## TO-DO

- [x] Aggiungere API Gateway con Bearer Token Authentication
- [x] Containerizzare MySQL nel docker-compose
- [x] Bloccare accesso diretto ai microservizi (solo gateway esposto)
- [x] Centralizzare la configurazione (Spring Cloud Config)
- [x] Credenziali in `.env` (escluse da git)
- [x] Spring Security su tutti i microservizi (CORS ristretto)
- [x] Token Gateway passato via variabile d'ambiente
- [x] Sanificazione error handlers (400 per input malformati/non numerici, no leak dettagli interni)
- [x] Fix sicurezza: NPE in TokenProperties, WWW-Authenticate su 401, SSL MySQL via HikariCP
- [x] Validazione input (PATCH con DTO, phone uniqueness)
- [x] Migrazioni database con Flyway
- [x] Rate limiting sul gateway (Bucket4j)
- [x] Circuit Breaker + Retry + TimeLimiter (Resilience4j) su chiamate REST loan→book/member
- [x] Gestione errori `ResourceAccessException` con fallback a `503 Service Unavailable`
- [x] Bulkhead + Rate Limiter (Resilience4j) su book-service e member-service
- [ ] Autenticazione JWT / OAuth2 (Keycloak)

---

## Resilienza (Resilience4j)

Il progetto implementa pattern di resilienza tramite **Resilience4j** per gestire guasti e sovraccarico.

### Loan Service — Circuit Breaker + Retry (chiamate outgoing)

Il loan-service chiama via REST book-service e member-service. Su queste chiamate sono applicati:

| Pattern | Configurazione | Effetto |
|---|---|---|
| **Circuit Breaker** | `sliding-window-size=10`, `minimum-number-of-calls=5`, `failure-rate-threshold=50%`, `wait-duration-in-open-state=30s`, `permitted-number-of-calls-in-half-open-state=3` | Dopo il 50% di errori in una finestra di 10 chiamate (minimo 5), il circuito si apre per 30 secondi. In half-open state, 3 chiamate di prova decidono se chiudere o riaprire |
| **Retry** | `max-attempts=3`, `wait-duration=500ms` | Ritenta fino a 3 volte le chiamate che falliscono con `5xx` o `ResourceAccessException` |
| **Time Limiter** | `timeout-duration=4s` | Timeout massimo di 4 secondi per ogni chiamata |

**Ignora 404**: Gli errori `404 Not Found` sono trattati come errori funzionali (`ValidationException`) e NON attivano il circuit breaker.

**Fallback**: In caso di circuito aperto o errori dopo i retry, il servizio restituisce `503 Service Unavailable`.

### Book Service & Member Service — Bulkhead + Rate Limiter (protezione inbound)

I servizi leaf (book-service e member-service) non effettuano chiamate HTTP outgoing. Sono protetti da sovraccarico inbound tramite:

| Pattern | Configurazione | Effetto |
|---|---|---|
| **Bulkhead** (semaforo) | `max-concurrent-calls=10`, `max-wait-duration=500ms` | Limita a 10 le chiamate contemporanee al service layer. L'undicesima chiamata simultanea attende fino a 500ms, poi riceve `503 Bulkhead Full` |
| **Rate Limiter** | `limit-for-period=100`, `limit-refresh-period=1s` | Massimo 100 richieste al secondo. La 101esima riceve `429 Too Many Requests` senza attendere |

### Test sui pattern di resilienza

```bash
# Esegue i test di resilienza per tutti i moduli
.\mvnw.cmd test

# Singoli moduli
.\mvnw.cmd test -pl loan-service
.\mvnw.cmd test -pl book-service
.\mvnw.cmd test -pl member-service
```

---

## Test con Postman (Resilienza)

> **Prerequisito**: I servizi devono essere avviati (tramite `docker-compose up -d`). Tutte le richieste devono passare dal **Gateway sulla porta `8080`** con header:
> ```
> Authorization: Bearer my-secret-token-123
> ```

### Rate Limiter (book-service)

La configurazione di produzione limita a **100 richieste/secondo**. Per testare con Postman:

1. **Riduci temporaneamente il limite** modificando `config-repo/book-service.yml`:
   ```yaml
   ratelimiter:
     instances:
       bookService:
         limit-for-period: 3
   ```
2. **Riavvia book-service**: `docker-compose restart book-service`
3. Invia **5 richieste GET rapide** a `http://localhost:8080/book`
   - Le prime 3 ≈ `200 OK`
   - La 4ª e 5ª ≈ `429 Too Many Requests`
     ```json
     {
       "status": 429,
       "error": "Too Many Requests",
       "message": "Book service is receiving too many requests. Please try again later."
     }
     ```
4. **Ripristina** la configurazione originale e riavvia.

> **Nota**: Con il valore di default `limit-for-period=100` è difficile raggiungere il limite con richieste manuali. Abbassalo per il test.

### Bulkhead (book-service)

Il bulkhead limita le **chiamate concorrenti** (non la frequenza). Con Postman è complesso perché servono richieste **veramente simultanee**.

**Opzione 1 — Usa `curl` (PowerShell/cmd)**:

1. Modifica `config-repo/book-service.yml` per valori piú stretti:
   ```yaml
   bulkhead:
     instances:
       bookService:
         max-concurrent-calls: 2
         max-wait-duration: 0
   ```
2. Riavvia: `docker-compose restart book-service`
3. Esegui in PowerShell:
   ```powershell
   $url = "http://localhost:8080/book"
   $headers = @{ Authorization = "Bearer my-secret-token-123" }
   1..15 | ForEach-Object -Parallel {
       try {
           $r = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
           Write-Output "OK $($r.StatusCode)"
       } catch {
           Write-Output "ERR $($_.Exception.Response.StatusCode.value__)"
       }
   } -ThrottleLimit 15
   ```
   Dovresti vedere circa 2 `200 OK` e 13 `503` (Bulkhead Full).

**Opzione 2 — Apache Bench**:

```bash
ab -n 15 -c 15 -H "Authorization: Bearer my-secret-token-123" http://localhost:8080/book
```

Con `max-concurrent-calls=2`, circa 13 richieste su 15 riceveranno `503`.

### Circuit Breaker (loan-service)

Il loan-service chiama book-service e member-service per arricchire i dettagli dei prestiti.
Se un servizio downstream non risponde, dopo i retry il circuit breaker si apre e risponde con `503 Service Unavailable`.

**Per testare con Postman o PowerShell**:

1. Assicurati che tutti i servizi siano avviati
2. Fai una GET a `{{baseUrl}}/loans/details` per verificare che funzioni (dovresti ricevere `200` con la lista dei prestiti arricchiti)
3. Ferma book-service:
   ```bash
   docker compose stop book-service
   ```
4. Fai una GET a `{{baseUrl}}/loans/details`:
   - Dopo alcuni tentativi (retry + apertura CB) → `503 Service Unavailable`
   ```json
   {
     "status": 503,
     "error": "SERVICE_UNAVAILABLE",
     "message": "Book service is currently unavailable. Please try again later."
   }
   ```
5. Riavvia book-service:
   ```bash
   docker compose start book-service
   ```
6. Dopo 30 secondi (wait-duration-in-open-state), il CB torna a half-open e poi closed → `200 OK`

> **Nota**: Il circuito si apre dopo 5 fallimenti (minimum-number-of-calls) nel ritentare book-service. Ogni chiamata viene ritentata 3 volte (Retry) prima di considerarla fallita.

---

## Licenza

Vedi il file `LICENSE` nella repository.

---

## Autore

**Davide** — Apulia Library Project
