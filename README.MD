# 📚 Apulia Library — Microservices Project

Ecosistema di microservizi per la gestione di una biblioteca digitale, sviluppato con **Java 21** e **Spring Boot 4**.

---

## 🧩 Architettura

Il progetto è composto da tre microservizi di business indipendenti più un API Gateway d'ingresso, organizzati in un progetto Maven multi-modulo:

| Servizio | Descrizione | Porta |
|---|---|---|
| 🚪 API Gateway | Gateway d'ingresso e autenticazione Bearer Token | `8080` |
| 📘 Book Service | Gestione del catalogo libri | `8081` |
| 🧑‍🤝‍🧑 Member Service | Gestione degli utenti iscritti | `8082` |
| 📄 Loan Service | Gestione dei prestiti | `8083` |

---

## 🛠️ Tecnologie utilizzate

- Java 21
- Spring Boot 4.1.0 / 3.4.1 (API Gateway)
- Spring Cloud Gateway (API Gateway)
- Spring Boot Starter WebFlux (API Gateway)
- Spring Web (Microservizi)
- Spring Data JPA + Hibernate
- Spring Security (Book Service)
- Spring Validation
- Springdoc OpenAPI 2.8.9
- MySQL 8 (esterno o su host)
- Lombok
- Maven (struttura multi-modulo)
- Docker & Docker Compose

---

## 🚪 API Gateway (Porta `8080`)

L'**API Gateway** agisce come punto di ingresso unico per tutte le richieste esterne ed esegue il routing dinamico verso i microservizi interni. Inoltre, implementa un filtro di sicurezza Bearer Token.

### Autenticazione
Tutte le chiamate instradate tramite il Gateway sulla porta `8080` richiedono l'autenticazione tramite un header HTTP `Authorization`:
```http
Authorization: Bearer my-secret-token-123
```
In caso di token mancante o errato, il gateway restituirà `401 Unauthorized`.

### Regole di Routing

| Path Sorgente (Gateway) | Destinazione |
|---|---|
| `/book` e `/book/**` | `http://book-service:8081` |
| `/members` e `/members/**` | `http://member-service:8082` |
| `/loans` e `/loans/**` | `http://loan-service:8083` |

---

## 🐳 Docker & Docker Compose

L'intero ecosistema può essere compilato e avviato tramite Docker.

### 📦 Build delle immagini e compilazione jar
Da root del progetto, compila i progetti con Maven e builda i container:

```bash
# Compilazione dei jar
.\mvnw.cmd package -DskipTests

# Build delle immagini docker
docker-compose build
```

### ▶️ Avvio dei microservizi
Per avviare l'intera architettura:

```bash
docker-compose up -d
```

### 🧪 Verifica stato dei container

```bash
docker ps
```

Per vedere i log di un servizio (ad esempio l'api-gateway):

```bash
docker logs api-gateway
```

### 🛑 Stop & Cleanup

```bash
docker-compose down
```

---

## 📘 Book Service (Porta `8081` / Gateway `8080`)

Microservizio dedicato alla gestione del catalogo libri.

### Endpoints (tramite Gateway `8080` con Auth Header)

| Metodo | Endpoint | Descrizione |
|---|---|---|
| GET | `/book` | Lista libri |
| GET | `/book/{id}` | Libro per ID |
| GET | `/book/search` | Ricerca per titolo e/o autore (es. `?title=1984`) |
| POST | `/book` | Crea libro |
| PUT | `/book/{id}` | Aggiorna libro |
| PATCH | `/book/{id}` | Aggiornamento parziale |
| DELETE | `/book/{id}` | Elimina libro |

### Documentazione API (Diretta)
```
http://localhost:8081/swagger-ui/index.html
http://localhost:8081/v3/api-docs
```

---

## 🧑‍🤝‍🧑 Member Service (Porta `8082` / Gateway `8080`)

Microservizio dedicato alla gestione degli utenti iscritti alla biblioteca.

### Endpoints (tramite Gateway `8080` con Auth Header)

| Metodo | Endpoint | Descrizione |
|---|---|---|
| GET | `/members` | Lista membri |
| GET | `/members/{id}` | Membro per ID |
| POST | `/members` | Crea membro |
| PUT | `/members/{id}` | Aggiorna membro |
| DELETE | `/members/{id}` | Elimina membro |

### Documentazione API (Diretta)
```
http://localhost:8082/swagger-ui/index.html
http://localhost:8082/v3/api-docs
```

---

## 📄 Loan Service (Porta `8083` / Gateway `8080`)

Microservizio dedicato alla gestione dei prestiti. Interagisce internamente via REST con Book Service e Member Service per validare e arricchire i dati dei prestiti.

### Endpoints (tramite Gateway `8080` con Auth Header)

| Metodo | Endpoint | Descrizione |
|---|---|---|
| GET | `/loans` | Lista prestiti |
| GET | `/loans/{id}` | Prestito per ID |
| GET | `/loans/details` | Lista prestiti arricchiti con dettagli libro e membro |
| GET | `/loans/details/{id}` | Dettaglio singolo prestito arricchito |
| POST | `/loans` | Crea prestiti (multipli) |
| PATCH | `/loans/{id}/return` | Restituisce libro (imposta returnDate) |
| DELETE | `/loans/{id}` | Elimina prestito |

### Documentazione API (Diretta)
```
http://localhost:8083/swagger-ui/index.html
http://localhost:8083/v3/api-docs
```

---

## 🧱 Struttura progetto

```
Library-Microservice-Architecture/
├── pom.xml                  (parent multi-modulo)
├── docker-compose.yml       (configurazione orchestratore docker)
├── api-gateway/             (API Gateway del sistema)
│   ├── Dockerfile
│   └── src/main/
├── book-service/            (Catalogo libri)
│   ├── Dockerfile
│   └── src/main/
├── member-service/          (Gestione iscritti)
│   ├── Dockerfile
│   └── src/main/
└── loan-service/            (Gestione prestiti)
    ├── Dockerfile
    └── src/main/
```

---

## 📝 TO-DO

- [x] Aggiungere API Gateway con Bearer Token Authentication
- [ ] Aggiungere meccanismo di discovery (Eureka)
- [ ] Centralizzare la configurazione (Spring Cloud Config)

---

## 📄 Licenza

Vedi il file `LICENSE` nella repository.

---

## 👨‍💻 Autore

**Davide** — Apulia Library Project