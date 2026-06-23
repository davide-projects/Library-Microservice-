# 📚 Apulia Library — Microservices Project

Ecosistema di microservizi per la gestione di una biblioteca digitale, sviluppato con **Java 21** e **Spring Boot 3.4.1**.

---

## Architettura

Il progetto e' composto da tre microservizi di business indipendenti piu' un API Gateway d'ingresso, organizzati in un progetto Maven multi-modulo:

| Servizio | Descrizione | Porta interna | Accessibile dall'esterno |
|---|---|---|---|---|
| API Gateway | Gateway d'ingresso e autenticazione Bearer Token | `8080` | **Si'** |
| Config Server | Configurazione centralizzata (Spring Cloud Config) | `8888` | Solo health check |
| Book Service | Gestione del catalogo libri | `8081` | No (solo via gateway) |
| Member Service | Gestione degli utenti iscritti | `8082` | No (solo via gateway) |
| Loan Service | Gestione dei prestiti | `8083` | No (solo via gateway) |
| MySQL | Database relazionale | `3306` | No (solo rete interna Docker) |

> **Nota:** L'unico punto di ingresso e' il **API Gateway sulla porta `8080`**. I microservizi non sono direttamente raggiungibili dall'esterno.

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
- `ddl-auto: update` per Hibernate
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
- [ ] Autenticazione JWT / OAuth2 (Keycloak)


---

## Licenza

Vedi il file `LICENSE` nella repository.

---

## Autore

**Davide** — Apulia Library Project
