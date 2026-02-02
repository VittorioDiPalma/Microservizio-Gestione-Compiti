# Documentazione

## Panoramica

Il servizio **Gestione Compiti** gestisce la creazione, assegnazione e consegna dei compiti nella piattaforma universitaria. Consente ai docenti di creare compiti per i corsi, agli studenti di gestire le loro consegne, e gestisce gli allegati associati a compiti e consegne.

## Funzionalità

- Creazione e gestione compiti (Assignment) da parte dei docenti
- Consegna compiti (Submission) da parte degli studenti
- Upload e download di file allegati
- Visualizzazione compiti per corso
- Statistiche sulle consegne
- Gestione stati delle consegne (SUBMITTED, GRADED, LATE, PENDING)
- Integrazione con microservizi esterni (Gestione Corsi, Gestione Iscrizioni, Gestione Valutazioni, Gestione Utenti e Ruoli)

## Porta di Default

```
Server: http://localhost:8080
```

## Documentazione API

```
Swagger UI: http://localhost:8080/swagger-ui.html
```

## Architettura del Database (PostgreSQL)

### Tabella: Assignments

| Campo       | Tipo         | Note                              |
|-------------|--------------|-----------------------------------|
| id          | String       | UUID generato automaticamente     |
| title       | String       | Titolo del compito                |
| description | String       | Descrizione  |
| dueDate     | LocalDateTime| Data di scadenza                  |
| courseId    | String       | ID del corso                      |
| teacherId   | String       | ID del docente creatore           |
| createdAt   | LocalDateTime| Timestamp di creazione            |
| updatedAt   | LocalDateTime| Timestamp ultimo aggiornamento    |

### Tabella: Submissions

| Campo        | Tipo         | Note                                    |
|--------------|--------------|-----------------------------------------|
| id           | String       | UUID generato automaticamente           |
| assignmentId | String       | FK verso Assignment                     |
| studentId    | String       | ID dello studente                       |
| content      | String       | Contenuto testuale |
| submittedAt  | LocalDateTime| Timestamp di consegna                   |
| status       | Enum         | SUBMITTED, GRADED, LATE, PENDING        |

### Tabella: FileAttachments

| Campo            | Tipo         | Note                                      |
|------------------|--------------|-------------------------------------------|
| id               | String       | UUID generato automaticamente             |
| originalFilename | String       | Nome originale del file                   |
| storedFilename   | String       | Nome del file sul filesystem              |
| filePath         | String       | Percorso completo del file                |
| mimeType         | String       | Tipo MIME del file                        |
| fileSize         | Long         | Dimensione in bytes                       |
| entityId         | String       | ID dell'entità associata (Assignment/Submission) |
| entityType       | Enum         | ASSIGNMENT o SUBMISSION                   |
| uploadedBy       | String       | ID utente che ha caricato                 |
| uploadedAt       | LocalDateTime| Timestamp di upload                       |

### Stati delle Consegne

| Stato     | Descrizione                                    |
|-----------|------------------------------------------------|
| SUBMITTED | Consegna effettuata in tempo                   |
| LATE      | Consegna effettuata in ritardo                 |
| GRADED    | Consegna valutata (non più modificabile)       |
| PENDING   | Consegna in attesa di valutazione              |

## Comunicazione

Il servizio utilizza **RabbitMQ** come sistema di messaggistica asincrono.

### Eventi Pubblicati

Il servizio pubblica eventi tramite l'exchange `newunimol.events` (Topic):

```
Exchange: newunimol.events (Topic)
├── assignment.created   → Notifica creazione compito
├── assignment.updated   → Notifica aggiornamento compito
├── assignment.deleted   → Notifica eliminazione compito
└── submission.created   → Notifica consegna studente
```

### Eventi Consumati

Il servizio consuma eventi dagli altri microservizi:

```
├── gestione-compiti.course.deleted      → Notifica eliminazione corso
└── gestione-compiti.assessment.created  → Notifica creazione valutazione
```

### Esempi di Messaggi

#### ASSIGNMENT_CREATED
```json
{
  "eventType": "ASSIGNMENT_CREATED",
  "assignmentId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Esercitazione Java",
  "courseId": "CORSO123",
  "teacherId": "123456",
  "dueDate": "2026-02-15T23:59:59",
  "timestamp": 1716470423000
}
```

#### ASSIGNMENT_UPDATED
```json
{
  "eventType": "ASSIGNMENT_UPDATED",
  "assignmentId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Esercitazione Java - Aggiornata",
  "description": "Nuova descrizione",
  "dueDate": "2026-02-20T23:59:59",
  "timestamp": 1716470523000
}
```

#### ASSIGNMENT_DELETED
```json
{
  "eventType": "ASSIGNMENT_DELETED",
  "assignmentId": "550e8400-e29b-41d4-a716-446655440000",
  "courseId": "CORSO123",
  "timestamp": 1716470623000
}
```

#### SUBMISSION_CREATED
```json
{
  "eventType": "SUBMISSION_CREATED",
  "submissionId": "660e8400-e29b-41d4-a716-446655440001",
  "assignmentId": "550e8400-e29b-41d4-a716-446655440000",
  "studentId": "178001",
  "status": "SUBMITTED",
  "submittedAt": "2026-02-10T14:30:00",
  "timestamp": 1716470723000
}
```

## JWT Authentication

I token JWT vengono utilizzati per autenticare gli utenti e sono emessi dal microservizio **Gestione Utenti e Ruoli**.

### Configurazione

Il servizio **valida** i token JWT utilizzando la chiave pubblica RSA configurata in:
```properties
jwt.public-key=${JWT_PUBLIC_KEY}
```

### Esempio contenuto del Token

```json
{
  "header": {
    "alg": "RS256"
  },
  "payload": {
    "sub": "180051",
    "iat": 1716470423,
    "exp": 1716474023,
    "username": "vittorio.dipalma",
    "role": "admin"
  },
  "signature": "RwJ6KU8X8DQ9ImTD6O4RrmlQ9znzVz1dAQuvG3k9KZ4"
}
```

### Payload del Token

| Campo    | Descrizione                         |
|----------|-------------------------------------|
| sub      | Identificatore utente (matricola)   |
| iat      | Data creazione (Unix timestamp)     |
| exp      | Data scadenza (Unix timestamp)      |
| username | Nome e Cognome utente               |
| role     | Ruolo utente (admin, teach, student)|

### Testing API

Per testare le API:
1. Ottieni un token JWT dal microservizio Gestione Utenti e Ruoli tramite `/auth/login`
2. Includi il token nell'header delle richieste successive:

```
Authorization: Bearer <token JWT>

Esempio:
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxNzg2NjYiLCJpYXQiOjE3NTAxNzU4NDQsImV4cCI6MTc4MTcxNTQ0NCwidXNlcm5hbWUiOiJkb25hdG8iLCJyb2xlIjoiYWRtaW4ifQ.jpOJDG_IgYx_3KwCiIDBQpMceYRl1QQw8ORM...
```

## File Upload

### Configurazione
- Directory di upload: `uploads/` (configurabile via `FILE_UPLOAD_DIR`)
- Dimensione massima file: **10MB**
- Formati supportati: PDF, DOCX, PNG, JPG, ZIP, TXT

### Struttura Directory
```
uploads/
├── assignments/     → File allegati ai compiti
└── submissions/     → File allegati alle consegne
```

## API Endpoints

### Admin

Nessun permesso esclusivo per ADMIN. Gli ADMIN possono utilizzare tutte le API e bypassare i controlli di ownership sui microservizi esterni.

### Docenti

| Metodo | Endpoint | Input | Output | Descrizione |
|--------|----------|-------|--------|-------------|
| POST | `/api/v1/assignments` | **Header:** Token JWT<br>**Body:** `{"title": "string", "description": "string", "dueDate": "2026-02-15T23:59:59", "courseId": "string"}` | AssignmentResponseDto | Crea nuovo compito |
| GET | `/api/v1/assignments/{id}` | **Header:** Token JWT<br>**PathVariable:** id compito | AssignmentResponseDto | Visualizza compito (solo propri) |
| GET | `/api/v1/assignments/course/{courseId}` | **Header:** Token JWT<br>**PathVariable:** id corso | List<AssignmentResponseDto> | Lista compiti del corso |
| PUT | `/api/v1/assignments/{id}` | **Header:** Token JWT<br>**PathVariable:** id compito<br>**Body:** `{"title": "string", "description": "string", "dueDate": "2026-02-20T23:59:59"}` | AssignmentResponseDto | Modifica compito |
| DELETE | `/api/v1/assignments/{id}` | **Header:** Token JWT<br>**PathVariable:** id compito | void | Elimina compito (+ consegne e file) |
| POST | `/api/v1/files/assignment/{assignmentId}/upload` | **Header:** Token JWT<br>**PathVariable:** id compito<br>**FormData:** file (multipart) | FileAttachment | Carica file allegato al compito |
| GET | `/api/v1/submissions/assignment/{assignmentId}` | **Header:** Token JWT<br>**PathVariable:** id compito | List<SubmissionResponseDto> | Lista consegne di un compito |
| GET | `/api/v1/submissions/assignment/{assignmentId}/stats` | **Header:** Token JWT<br>**PathVariable:** id compito | SubmissionStatsDto | Statistiche consegne (totali, in tempo, ritardo, valutate) |
| GET | `/api/v1/submissions/assignment/{assignmentId}/student/{studentId}` | **Header:** Token JWT<br>**PathVariable:** id compito, id studente | SubmissionResponseDto | Visualizza consegna di uno studente specifico |

### Studenti

| Metodo | Endpoint | Input | Output | Descrizione |
|--------|----------|-------|--------|-------------|
| GET | `/api/v1/assignments/{id}` | **Header:** Token JWT<br>**PathVariable:** id compito | AssignmentResponseDto | Visualizza compito (solo corsi iscritti) |
| GET | `/api/v1/assignments/course/{courseId}` | **Header:** Token JWT<br>**PathVariable:** id corso | List<AssignmentResponseDto> | Lista compiti del corso (solo corsi iscritti) |
| POST | `/api/v1/submissions/assignment/{assignmentId}` | **Header:** Token JWT<br>**PathVariable:** id compito<br>**Body:** `{"content": "string"}` | SubmissionResponseDto | Consegna compito |
| GET | `/api/v1/submissions/{submissionId}` | **Header:** Token JWT<br>**PathVariable:** id consegna | SubmissionResponseDto | Visualizza propria consegna |
| PUT | `/api/v1/submissions/{submissionId}` | **Header:** Token JWT<br>**PathVariable:** id consegna<br>**Body:** `{"content": "string"}` | SubmissionResponseDto | Modifica consegna (solo se non GRADED) |
| DELETE | `/api/v1/submissions/{submissionId}` | **Header:** Token JWT<br>**PathVariable:** id consegna | void | Elimina consegna (solo se non GRADED e prima scadenza) |
| POST | `/api/v1/files/submission/{submissionId}/upload` | **Header:** Token JWT<br>**PathVariable:** id consegna<br>**FormData:** file (multipart) | FileAttachment | Carica file allegato alla consegna |

### Permessi Generici (Tutti)

| Metodo | Endpoint | Input | Output | Descrizione |
|--------|----------|-------|--------|-------------|
| GET | `/api/v1/files/download/{fileId}` | **Header:** Token JWT<br>**PathVariable:** id file | Resource (file binario) | Scarica file se autorizzato |
| GET | `/api/v1/files/assignment/{assignmentId}` | **Header:** Token JWT<br>**PathVariable:** id compito | List<FileAttachment> | Lista file del compito |
| GET | `/api/v1/files/submission/{submissionId}` | **Header:** Token JWT<br>**PathVariable:** id consegna | List<FileAttachment> | Lista file della consegna |
| DELETE | `/api/v1/files/{fileId}` | **Header:** Token JWT<br>**PathVariable:** id file | void | Elimina file (solo proprietario) |

### Controlli di Accesso
- **ADMIN**: Accesso completo a tutte le risorse, bypassa verifiche esterne
- **TEACHER**: Accede solo ai propri compiti e ai compiti dei corsi in cui insegna
- **STUDENT**: Accede solo ai compiti dei corsi in cui è iscritto e alle proprie consegne


## Docker Compose

Il servizio include un `docker-compose.yml` per eseguire:
- **PostgreSQL** (porta 5432)
- **RabbitMQ** con management UI (porta 15672)

```bash
docker-compose up -d
```

## Avvio del Microservizio

```bash
# Con Maven
mvn spring-boot:run

# Con JAR compilato
java -jar target/newunimol-1.0.0.jar
```
