
# DVSmart Reorganization API

> Servicio de **reorganización de archivos** entre SFTP Origen y SFTP Destino, implementado con **Spring Boot / Spring Batch**, **MongoDB** y **PostgreSQL** para metadatos; expone **APIs REST** para disparo y auditoría, y provee **observabilidad** vía Actuator.

---

## Índice

- [Descripción y Stack tecnológico](#descripción-y-stack-tecnologico)
- [Criterios de diseño del servicio (HLD)](#criterios-de-diseño-del-servicio-hld)
  - [Adapters](#adapters)
  - [Application](#application)
  - [Domain](#domain)
  - [Batch](#batch)
  - [Infra](#infra)
  - [Cross-cutting](#cross-cutting)
- [Arquitectura](#arquitectura)
  - [Diagrama de arquitectura](#diagrama-de-arquitectura)
  - [Diagrama de componentes](#diagrama-de-componentes)
  - [Diagrama de secuencia](#diagrama-de-secuencia)
  - [Diagrama de clases](#diagrama-de-clases)
  - [Técnicas de procesamiento](#técnicas-de-procesamiento)
- [Guía Completa de Configuración (application.properties)](#guía-completa-de-configuración-applicationproperties)
- [Despliegue y Escalabilidad](#despliegue-y-escalabilidad)
- [Uso y API (por controller y ruta)](#uso-y-api-por-controller-y-ruta)
- [Monitorización, Logs y Troubleshooting](#monitorización-logs-y-troubleshooting)
- [Riesgos y mitigaciones](#riesgos-y-mitigaciones)
- [Soporte y contacto](#soporte-y-contacto)
- [Referencias](#referencias)

---

## Descripción y Stack tecnologico

**Propósito**: Reorganizar archivos provenientes de un SFTP de origen hacia un SFTP de destino bajo un esquema de **particionado por hash**, con **procesamiento por chunks** y **paralelismo asíncrono**, manteniendo un **catálogo** en MongoDB y la **auditoría de ejecuciones**.

**Tecnologías clave**:
- **Java 21** y **Maven**; artifact: `dvsmart_reorganization_api` 1.0.1-SNAPSHOT.
- **Spring Boot** 4.x: **Web**, **Validation**, **Actuator**, **Spring Data MongoDB**.
- **Spring Batch** (persistencia en **PostgreSQL**).
- **Spring Integration SFTP** con *session pooling*.
- **MongoDB**: colecciones `files_index` y `job_executions_audit`.

---

## Criterios de diseño del servicio (HLD)

### Adapters
- **BatchReorganizeController**: expone endpoints para iniciar el job de reorganización completo.
- **JobAuditController**: endpoints de consulta de auditorías (historial, estado, detalle, rango, estadísticas y últimos runs).
- **Actuator**: endpoints de salud y métricas para readiness/liveness.

### Application
- **StartReorganizeFullUseCase**: entrada del caso de uso.
- **StartReorganizeFullService**: orquesta el **Job** y sus **Steps**, maneja parámetros y validaciones previas (evita ejecuciones simultáneas, genera etiquetas `runLabel`).

### Domain
- **FileReorganizationService**: cálculo de **ruta destino** mediante **hash partitioning** configurable.
- **CleanupPolicy**: reglas de **elegibilidad** para borrado en origen.
- **Modelos**: `ArchivoLegacy`, `CleanupCandidate`, `CleanupResult`, `JobExecutionAudit`.

### Batch
- **BatchReorgFullConfig**: define el **Job BATCH-REORG-FULL** con dos steps:
  - **Step 1 Reorganization**: `MongoIndexedDisorganizedFileItemReader` → `CompositeItemProcessor` → `AsyncItemProcessor` → `AsyncItemWriter` → `SftpMoveAndIndexItemWriter`.
  - **Step 2 Cleanup**: `CleanupCandidateReader` → `CleanupValidator` → `OriginFileDeleteWriter`.
- **JobExecutionAuditListener**: registra métricas de cada step y del job.

### Infra
- **SftpOriginRepositoryImpl** y **SftpDestinationRepositoryImpl**: gateways SFTP.
- **SftpSessionFactoryConfig**: *session pooling* y templates SFTP.
- **DisorganizedFilesIndexRepositoryImpl**: acceso a `files_index` en Mongo.
- **JobExecutionAuditRepository**: acceso a `job_executions_audit` en Mongo.
- **PostgreSQL**: metadatos de Spring Batch.

### Cross-cutting
- **Propiedades**: `SftpConfigProperties`, `BatchConfigProperties`, `MongoConfigProperties`.
- **Logging**: niveles específicos para paquetes y patrones en consola.
- **Observabilidad**: Actuator (`health`, `info`, `metrics`, `batch`).

---

## Arquitectura

### Diagrama de arquitectura

```mermaid
flowchart LR
  subgraph Origin[SFTP Origen]
    OFS[(Archivo en /disorganized_data)]
  end

  subgraph Catalog[MongoDB]
    IDX[(files_index)]
    AUD[(job_executions_audit)]
  end

  subgraph Service[DVSmart Reorganization API]
    API[(REST Controller)]
    JB[Spring Batch Job\nBATCH-REORG-FULL]
    STEP1[Step 1 Reorganization\nReader + Async Processor + Async Writer]
    STEP2[Step 2 Cleanup\nReader + Validator + Writer]
    AUDL[JobExecutionAuditListener]
  end

  subgraph Dest[SFTP Destino]
    DFS[(/organized_data/<hash>/filename)]
  end

  API --> JB
  JB --> STEP1
  STEP1 --> DFS
  STEP1 --> IDX
  JB --> STEP2
  STEP2 --> OFS
  STEP2 --> IDX

  AUDL --> AUD
  IDX <-.streaming.-> STEP1
  IDX <-.query cleanup.-> STEP2
```

### Diagrama de componentes

```mermaid
flowchart TB
  subgraph Adapters
    BRC[BatchReorganizeController\nREST: /api/batch/reorganize/*]
    JAC[JobAuditController\nREST: /api/monitoring/audit/*]
    ACT[Spring Boot Actuator\n/actuator/*]
  end

  subgraph Application
    SRFU[StartReorganizeFullUseCase]
    SRFS[StartReorganizeFullService\nOrquesta jobs y steps]
  end

  subgraph Domain
    FRS[FileReorganizationService\nCalculo de rutas y hash partitioning]
    VALC[CleanupPolicy\nReglas de elegibilidad de borrado]
    MODELS[Modelos\nArchivoLegacy\nCleanupCandidate\nCleanupResult\nJobExecutionAudit]
  end

  subgraph Batch
    BRFC[BatchReorgFullConfig\nJob BATCH-REORG-FULL]
    S1[Step 1 Reorganization]
    R1[MongoIndexedDisorganizedFileItemReader\nStreaming cursor]
    P1[CompositeItemProcessor\nMap + Enrich + Validate]
    AP[AsyncItemProcessor]
    AW[AsyncItemWriter]
    W1[SftpMoveAndIndexItemWriter\nMove a SFTP Dest y update Mongo]
    S2[Step 2 Cleanup]
    R2[CleanupCandidateReader\nQuery completados]
    P2[CleanupValidator\nAplica CleanupPolicy]
    W2[OriginFileDeleteWriter\nBorrado en SFTP Origen paralelo]
    AUDL[JobExecutionAuditListener]
  end

  subgraph Infra
    SFTP_OR[SftpOriginRepositoryImpl\nLee y borra en origen]
    SFTP_DS[SftpDestinationRepositoryImpl\nEscribe en destino]
    SFTP_POOL[SftpSessionFactoryConfig\nPools y templates SFTP]
    MONGO_REPO[DisorganizedFilesIndexRepositoryImpl\nMongoDB files_index]
    AUD_REPO[JobExecutionAuditRepository\nMongoDB audit]
    BATCH_META[PostgreSQL\nSpring Batch metadata]
  end

  subgraph CrossCutting
    SFTP_PROPS[SftpConfigProperties\nOrigin Dest BaseDir Timeout Pool]
    BATCH_PROPS[BatchConfigProperties\nChunk Threads Queue Skip Retry]
    MONGO_PROPS[MongoConfigProperties\nURI DB Colecciones]
    LOGCONF[Logging Configuration\nNiveles por paquete y patrones]
    OBS[Observability\nActuator Health Info Metrics Batch]
  end

  BRC --> SRFU
  JAC --> AUD_REPO
  ACT --> OBS

  SRFU --> SRFS
  SRFS --> BRFC
  BRFC --> S1
  BRFC --> S2
  BRFC --> AUDL

  S1 --> R1 --> P1 --> AP --> AW --> W1
  S2 --> R2 --> P2 --> W2

  P1 --> FRS
  P2 --> VALC
  FRS --> MODELS
  VALC --> MODELS

  R1 --> MONGO_REPO
  W1 --> SFTP_DS
  W1 --> MONGO_REPO
  R2 --> MONGO_REPO
  W2 --> SFTP_OR

  AUDL --> AUD_REPO
  BRFC --> BATCH_META

  SFTP_DS --> SFTP_POOL
  SFTP_OR --> SFTP_POOL

  BRFC --> BATCH_PROPS
  SFTP_POOL --> SFTP_PROPS
  MONGO_REPO --> MONGO_PROPS
  AUD_REPO --> MONGO_PROPS
  LOGCONF -. niveles .-> Adapters
  LOGCONF -. niveles .-> Batch
  LOGCONF -. niveles .-> Infra
```

### Diagrama de secuencia

```mermaid
sequenceDiagram
  autonumber
  participant C as Cliente
  participant API as BatchReorganizeController
  participant UC as StartReorganizeFullService
  participant JOB as Spring Batch BATCH-REORG-FULL
  participant R as Reader Mongo cursor
  participant P as Processor Async
  participant W as Writer SFTP Dest + Mongo
  participant CLN as Cleanup Step
  participant AUD as Audit Listener Service
  participant M as MongoDB
  participant S0 as SFTP Origen
  participant S1 as SFTP Destino

  C->>API: POST /api/batch/reorganize/full {jobName, parameters}
  API->>UC: execute(jobName, params)
  UC->>JOB: start(...)
  JOB->>AUD: beforeJob()
  JOB->>R: stream files_index where reorg_status=PENDING
  R-->>JOB: doc DisorganizedFilesIndex
  JOB->>P: map to ArchivoLegacy + calcular destino hash
  P-->>JOB: ArchivoLegacy enriquecido
  JOB->>W: transferir a SFTP Dest + update Mongo
  W->>S1: write y mkdir recursivo
  W->>M: update reorg_status=SUCCESS
  JOB->>CLN: ejecutar cleanup
  CLN->>M: query COMPLETED y deleted_from_source=false
  CLN->>S0: rm path en paralelo
  CLN->>M: bulk update deleted_from_source=true
  JOB->>AUD: afterJob actualizar métricas
```

### Diagrama de clases

```mermaid
classDiagram
  class BatchReorganizeController {
    +POST /api/batch/reorganize/full
  }
  class StartReorganizeFullService {
    +execute(jobName, params) : Long
  }
  class BatchReorgFullConfig {
    +Step reorganizeStep()
    +Step cleanupOriginStep()
    +Job batchReorgFullJob()
  }
  class MongoIndexedDisorganizedFileItemReader {+createReader()}
  class SftpMoveAndIndexItemWriter {+write(Chunk<ArchivoLegacy>)}
  class OriginFileDeleteWriter {+write(Chunk<CleanupResult>)}
  class CleanupCandidateReader {+read()}
  class CleanupValidator {+process(CleanupCandidate): CleanupResult}
  class FileReorganizationService {+calculateDestinationPath(ArchivoLegacy, baseDir): String}
  class JobAuditService {+createAuditRecord(JobExecution); +updateAuditRecord(JobExecution)}
  class JobExecutionAuditListener {+beforeJob(); +afterJob()}

  BatchReorganizeController --> StartReorganizeFullService
  StartReorganizeFullService --> BatchReorgFullConfig
  BatchReorgFullConfig --> MongoIndexedDisorganizedFileItemReader
  BatchReorgFullConfig --> SftpMoveAndIndexItemWriter
  BatchReorgFullConfig --> OriginFileDeleteWriter
  BatchReorgFullConfig --> CleanupCandidateReader
  BatchReorgFullConfig --> CleanupValidator
  SftpMoveAndIndexItemWriter --> FileReorganizationService
  JobExecutionAuditListener --> JobAuditService
```

### Técnicas de procesamiento

- **Chunk-oriented processing**: lotes de tamaño `batch.chunk-size` para reducir overhead y mejorar throughput.
- **Paralelismo asíncrono**: `AsyncItemProcessor` + `AsyncItemWriter` con `ThreadPoolTaskExecutor` (`batch.thread-pool-size`, `batch.queue-capacity`).
- **Hash partitioning**: genera rutas destino tipo `/aa/bb/cc/archivo.ext` según partición (`partition-depth`, `chars-per-level`).
- **Streaming desde Mongo**: cursor (`find` + `sort` por `_id`) evita `skip` costosos y soporta altos volúmenes.
- **Idempotencia**: campos `reorg_*` y `idUnico` previenen duplicados; contador `reorg_attempts`.
- **Tolerancia a fallos**: `skip-limit` y `retry-limit` para reintentos en fallos transitorios de red.
- **Cleanup paralelo**: borrado en origen con pool configurable, actualizando `deleted_from_source` en bulk.
- **Auditoría y métricas**: listener captura tiempos, procesados, errores por step.

---

## Guía Completa de Configuración (application.properties)

```properties
# Core
spring.application.name=dvsmart-reorganization-api
server.servlet.context-path=/dvsmart-reorganization-api
server.port=8080
server.shutdown=graceful

# MongoDB
spring.mongodb.uri=mongodb://dvsmart_user:***@localhost:30000/dvsmart-ms?authSource=dvsmart-ms
mongo.files-index=files_index

# PostgreSQL (Spring Batch)
spring.datasource.url=jdbc:postgresql://localhost:30005/dvsmart
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=dvsmart_ms
spring.datasource.password=***
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# Spring Batch
spring.batch.job.enabled=false
spring.batch.jdbc.initialize-schema=always

# Batch (custom)
batch.chunk-size=1000
batch.thread-pool-size=20
batch.queue-capacity=500
batch.skip-limit=5
batch.retry-limit=3

# SFTP Origen
sftp.origin.host=localhost
sftp.origin.port=30002
sftp.origin.user=sftpsourceuser
sftp.origin.password=securepass
sftp.origin.base-dir=/disorganized_data
sftp.origin.timeout=30000
sftp.origin.pool.size=25

# SFTP Destino
sftp.dest.host=localhost
sftp.dest.port=22
sftp.dest.user=sftpdestinationuser
sftp.dest.password=securepass
sftp.dest.base-dir=/organized_data
sftp.dest.timeout=30000
sftp.dest.pool.size=25

# Hash Partitioning
sftp.hash-partitioning.partition-depth=3
sftp.hash-partitioning.chars-per-level=2

# Logging
logging.level.root=INFO
logging.level.com.indra.minsait.dvsmart.reorganization=DEBUG
logging.level.org.springframework.batch=INFO
logging.level.org.springframework.integration.sftp=DEBUG
logging.level.org.springframework.data.mongodb=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,batch
management.endpoint.health.show-details=always
management.metrics.enable.jvm=true
management.metrics.enable.process=true
management.metrics.enable.system=true
```

---

## Despliegue y Escalabilidad

### Producción (contenedor / K8s)
- **Config/Secrets**: externalizar `spring.mongodb.uri`, `spring.datasource.*`, `sftp.*` en `ConfigMap`/`Secret`.
- **Readiness/Liveness**: `/dvsmart-reorganization-api/actuator/health`.
- **Recursos**: ajustar `requests/limits` de CPU/Memory según throughput objetivo.
- **Persistencia Batch**: PostgreSQL gestionado, respaldos y monitoreo de pool.

### Alto rendimiento
- **CPU-bound vs IO-bound**: escalar `batch.thread-pool-size` con pruebas de saturación de SFTP/Mongo.
- **Chunking**: tamaños grandes (`1000`) reducen trips a red, validar memoria.
- **SFTP pool**: `sftp.*.pool.size` y `timeout` según latencia; evitar agotamiento de sesiones.
- **Mongo**: índices adecuados en `files_index` para filtros por estado y rango.
- **Logs**: reducir nivel en producción para evitar I/O excesivo.

### Build & Run
```bash
mvn -q -DskipTests package
java -jar target/dvsmart_reorganization_api-1.0.1-SNAPSHOT.jar --spring.profiles.active=default
```

---

## Uso y API (por controller y ruta)

> **Context path**: `/dvsmart-reorganization-api`

### BatchReorganizeController

**POST** `/api/batch/reorganize/full`
- **Body**
```json
{
  "jobName": "BATCH-REORG-FULL",
  "parameters": { "runLabel": "manual-YYYY-MM-DD" }
}
```
- **curl**
```bash
curl -s -X POST \
  http://localhost:8080/dvsmart-reorganization-api/api/batch/reorganize/full \
  -H 'Content-Type: application/json' \
  -d '{"jobName":"BATCH-REORG-FULL","parameters":{"runLabel":"manual-2025-12-29"}}'
```
- **Respuesta (202)**
```json
{ "message": "Batch job started successfully", "jobExecutionId": 12345, "status": "ACCEPTED" }
```

### JobAuditController

**GET** `/api/monitoring/audit/jobs/{jobName}`
```bash
curl -s http://localhost:8080/dvsmart-reorganization-api/api/monitoring/audit/jobs/BATCH-REORG-FULL | jq
```

**GET** `/api/monitoring/audit/status/{status}`
```bash
curl -s http://localhost:8080/dvsmart-reorganization-api/api/monitoring/audit/status/COMPLETED | jq
```

**GET** `/api/monitoring/audit/execution/{jobExecutionId}`
```bash
curl -s http://localhost:8080/dvsmart-reorganization-api/api/monitoring/audit/execution/12345 | jq
```

**GET** `/api/monitoring/audit/range?start=ISO&end=ISO`
```bash
curl -s "http://localhost:8080/dvsmart-reorganization-api/api/monitoring/audit/range?start=2025-12-01T00:00:00Z&end=2025-12-31T23:59:59Z" | jq
```

**GET** `/api/monitoring/audit/stats`
```bash
curl -s http://localhost:8080/dvsmart-reorganization-api/api/monitoring/audit/stats | jq
```

**GET** `/api/monitoring/audit/latest`
```bash
curl -s http://localhost:8080/dvsmart-reorganization-api/api/monitoring/audit/latest | jq
```

### Actuator

**GET** `/actuator/health`
```bash
curl -s http://localhost:8080/dvsmart-reorganization-api/actuator/health | jq
```

**GET** `/actuator/info`, `/actuator/metrics`, `/actuator/batch`
```bash
curl -s http://localhost:8080/dvsmart-reorganization-api/actuator/info | jq
curl -s http://localhost:8080/dvsmart-reorganization-api/actuator/metrics | jq
```

---

## Monitorización, Logs y Troubleshooting

### Monitorización
- **Salud**: `/actuator/health` con detalles (`show-details=always`).
- **Métricas**: JVM, proceso y sistema habilitadas; integrar con Prometheus/Grafana.
- **Auditoría**: usar endpoints de `JobAuditController` para seguimiento operacional.

### Logs
- **Paquetes**: `com.indra.minsait.dvsmart.reorganization=DEBUG` durante pruebas; `INFO` en producción.
- **Formatos**: patrón de consola con timestamp y logger.

### Troubleshooting

| Síntoma | Posible causa | Verificación | Acción |
|---|---|---|---|
| 409 CONFLICT al iniciar job | Job en ejecución | `/audit/status/STARTED`, Actuator `batch` | Esperar fin o detener ejecución |
| SocketTimeoutException en SFTP | Latencia/red | `sftp.*.timeout`, pruebas de red | Incrementar timeouts, `retry-limit` |
| Directorios destino no creados | Permisos insuficientes | Logs en SFTP destino | Conceder permisos, `mkdir` recursivo |
| Fuga de sesiones SFTP | Streams no cerrados | Revisar uso de input streams | Asegurar `close()`; pool devuelve sesión |
| Lento al leer Mongo | Falta de índices | `explain()` y listado de índices | Crear índices según script |
| Campos `reorg_*` no cambian | Fallo writer Mongo | Logs del writer | Revisar `updateFirst(...)` y excepciones |

---

## Riesgos y mitigaciones

- **Red SFTP inestable**: *Mitigación*: aumentar `timeout`, habilitar `retry`, circuit breakers a nivel de repositorio.
- **Sobrecarga de directorios** (hot dirs): *Mitigación*: **hash partitioning** con mayor profundidad/segmentos.
- **Lecturas Mongo voluminosas**: *Mitigación*: índices dedicados, `batch.chunk-size` optimizado, ventanas por rango.
- **Contención de hilos**: *Mitigación*: `thread-pool-size` acorde CPU, monitorizar cola, back-pressure.
- **Agotamiento de sesiones SFTP**: *Mitigación*: dimensionar `pool.size` y asegurar cierre correcto de streams.
- **Duplicidad de archivos**: *Mitigación*: `idUnico` y actualizaciones idempotentes en `files_index`.
- **Pérdida de trazabilidad**: *Mitigación*: auditoría por ejecución y persistencia de metadatos Batch.

---

## Soporte y contacto

- Responsable técnico: **hahuaranga@indracompany.com**

---

## Referencias

- Estructura de código: controllers, batch config, readers/writers, servicios de dominio, repositorios Mongo/SFTP, `application.properties`.
- Colecciones Mongo: `files_index`, `job_executions_audit`.
- Notas: validar compatibilidad de dependencias de Spring Boot con Java 21 antes de producción.

