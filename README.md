# DVSmart Reorganization API

Sistema de reorganización masiva de archivos desde servidores SFTP origen a destino, utilizando particionado hash (SHA-256) para distribución uniforme.

## 📋 Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Requisitos Previos](#requisitos-previos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Base de Datos MongoDB](#base-de-datos-mongodb)
- [Ejecución](#ejecución)
- [API REST](#api-rest)
- [Monitoreo](#monitoreo)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Arquitectura

### Patrón de Diseño
- **Arquitectura Hexagonal (Ports & Adapters)**
- **Domain-Driven Design (DDD)**
- **Spring Batch Chunk-Oriented Processing**

### Componentes Principales

```
┌─────────────────┐
│   REST API      │ ← Endpoint de inicio de jobs
└────────┬────────┘
         │
┌────────▼────────────────────────────────────────┐
│         Spring Batch Job                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │  Reader  │→ │Processor │→ │  Writer  │     │
│  │ (MongoDB)│  │ (Hash)   │  │ (SFTP)   │     │
│  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────┘
         │                              │
┌────────▼────────┐            ┌───────▼────────┐
│   MongoDB       │            │  SFTP Servers  │
│  - archivo_index│            │  - Origin      │
│  - processed_   │            │  - Destination │
│    files        │            └────────────────┘
└─────────────────┘
```

### Flujo de Procesamiento

1. **Lectura**: Cursor streaming de MongoDB (`archivo_index`)
2. **Procesamiento Asíncrono**:
   - Conversión de documento → modelo de dominio
   - Cálculo de hash SHA-256
   - Generación de path destino (particionado)
3. **Escritura**:
   - Lectura streaming del archivo origen (SFTP)
   - Creación de directorios en destino
   - Escritura streaming en destino (SFTP)
   - Auditoría en MongoDB (`processed_files`)

---

## 🛠️ Stack Tecnológico

| Componente | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 (LTS) | Runtime |
| Spring Boot | 4.0.0 | Framework base |
| Spring Batch | 6.0.0 | Procesamiento batch |
| Spring Integration | 7.0.0 | Integración SFTP |
| MongoDB | 7.0+ | Persistencia |
| Apache MINA SSHD | (via SSHJ 0.38.0) | Cliente SFTP |
| Lombok | 1.18.30 | Reducción de boilerplate |
| Maven | 3.9+ | Build tool |

---

## 📦 Requisitos Previos

### Software Requerido

- **JDK 21** o superior
- **Maven 3.9+**
- **Docker** y **Docker Compose** (para desarrollo local)
- **MongoDB 7.0+**
- Acceso a servidores **SFTP** (origen y destino)

### Puertos Utilizados

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| API REST | 8080 | Endpoint HTTP |
| MongoDB | 27017 | Base de datos |
| SFTP Origin | 2222 | Servidor SFTP origen (dev) |
| SFTP Destination | 2223 | Servidor SFTP destino (dev) |
| Mongo Express | 8081 | UI MongoDB (opcional) |

---

## 💾 Instalación

### 1. Clonar el Repositorio

```bash
git clone https://github.com/tu-org/dvsmart_reorganization_api.git
cd dvsmart_reorganization_api
```

### 2. Compilar el Proyecto

```bash
mvn clean install
```

### 3. Levantar Infraestructura (Desarrollo Local)

```bash
# Levantar MongoDB + SFTP servers
docker-compose up -d

# Verificar que estén corriendo
docker-compose ps
```

### 4. Poblar Datos de Prueba

Ver sección [Datos de Prueba](#datos-de-prueba).

---

## ⚙️ Configuración

### Archivo de Propiedades

El proyecto utiliza perfiles de Spring:

```
src/main/resources/
├── application.properties              # Configuración base
├── application-dev.properties          # Desarrollo local
├── application-prod.properties         # Producción
└── application-test.properties         # Testing
```

### Variables de Entorno (Producción)

```bash
# SFTP Origin
export SFTP_ORIGIN_HOST=sftp-prod-origin.example.com
export SFTP_ORIGIN_PORT=22
export SFTP_ORIGIN_USER=prod_user
export SFTP_ORIGIN_PASSWORD=secure_password
export SFTP_ORIGIN_BASE_DIR=/data/production/legacy

# SFTP Destination
export SFTP_DEST_HOST=sftp-prod-dest.example.com
export SFTP_DEST_PORT=22
export SFTP_DEST_USER=prod_user
export SFTP_DEST_PASSWORD=secure_password
export SFTP_DEST_BASE_DIR=/data/production/reorganized
```

### Parámetros de Configuración

#### Batch Configuration (`batch.*`)

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `batch.chunk-size` | int | 100 | Registros por chunk |
| `batch.concurrency-limit` | int | 10 | Límite de concurrencia (deprecado) |
| `batch.thread-pool-size` | int | 20 | Threads para procesamiento paralelo |
| `batch.queue-capacity` | int | 1000 | Capacidad de cola de tareas |

**Recomendaciones por ambiente:**
- **Dev**: chunk-size=10, thread-pool-size=5
- **Test**: chunk-size=5, thread-pool-size=2
- **Prod**: chunk-size=100, thread-pool-size=30

#### SFTP Origin Configuration (`sftp.origin.*`)

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `sftp.origin.host` | String | - | Hostname del servidor SFTP |
| `sftp.origin.port` | int | 22 | Puerto SSH |
| `sftp.origin.user` | String | - | Usuario SFTP |
| `sftp.origin.password` | String | - | Contraseña SFTP |
| `sftp.origin.base-dir` | String | - | Directorio base origen |
| `sftp.origin.timeout` | int | 30000 | Timeout en ms |
| `sftp.origin.pool.size` | int | 10 | Tamaño del pool de conexiones |

#### SFTP Destination Configuration (`sftp.dest.*`)

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `sftp.dest.host` | String | - | Hostname del servidor SFTP |
| `sftp.dest.port` | int | 22 | Puerto SSH |
| `sftp.dest.user` | String | - | Usuario SFTP |
| `sftp.dest.password` | String | - | Contraseña SFTP |
| `sftp.dest.base-dir` | String | - | Directorio base destino |
| `sftp.dest.timeout` | int | 30000 | Timeout en ms |
| `sftp.dest.pool.size` | int | 10 | Tamaño del pool de conexiones |

---

## 🗄️ Base de Datos MongoDB

### Colecciones

#### 1. `archivo_index` (Índice de Archivos Origen)

Contiene el inventario de archivos a reorganizar.

**Estructura del Documento:**

```javascript
{
    "_id": ObjectId("674c5e1a2b3f4a5e6d7c8b9a"),
    "idUnico": "file-sha256-hash-12345",
    "rutaOrigen": "/home/testuser/upload/origin/dir1/file1.txt",
    "nombre": "file1.txt",
    "mtime": ISODate("2025-12-13T20:30:00.000Z")
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `_id` | ObjectId | Sí | ID MongoDB (auto-generado) |
| `idUnico` | String | Sí | Identificador único del archivo |
| `rutaOrigen` | String | Sí | Path completo en SFTP origen |
| `nombre` | String | Sí | Nombre del archivo |
| `mtime` | Date | Sí | Fecha de última modificación |

**Índices:**

```javascript
// Índice único en idUnico
db.archivo_index.createIndex(
    { "idUnico": 1 }, 
    { unique: true, name: "idx_idUnico_unique" }
)

// Índice por defecto en _id (auto-creado)
db.archivo_index.createIndex(
    { "_id": 1 }
)
```

**Script de Creación:**

```javascript
use dvsmart_reorganization_dev

db.createCollection("archivo_index")

db.archivo_index.createIndex(
    { "idUnico": 1 }, 
    { unique: true, name: "idx_idUnico_unique" }
)
```

**Ejemplo de Inserción:**

```javascript
db.archivo_index.insertMany([
    {
        idUnico: "file1-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/dir1/file1.txt",
        nombre: "file1.txt",
        mtime: new Date()
    },
    {
        idUnico: "file2-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/dir1/file2.pdf",
        nombre: "file2.pdf",
        mtime: new Date()
    },
    {
        idUnico: "file3-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/dir2/file3.jpg",
        nombre: "file3.jpg",
        mtime: new Date()
    }
])
```

---

#### 2. `processed_files` (Auditoría de Archivos Procesados)

Registra el resultado del procesamiento de cada archivo.

**Estructura del Documento:**

```javascript
{
    "_id": ObjectId("674c5f2b3c4d5e6f7a8b9c0d"),
    "idUnico": "file-sha256-hash-12345",
    "rutaOrigen": "/home/testuser/upload/origin/dir1/file1.txt",
    "rutaDestino": "/home/testuser/upload/destination/a1/b2/c3/file1.txt",
    "nombre": "file1.txt",
    "status": "SUCCESS",
    "processedAt": ISODate("2025-12-13T22:35:10.123Z"),
    "errorMessage": null
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `_id` | ObjectId | Sí | ID MongoDB (auto-generado) |
| `idUnico` | String | Sí | Identificador único (mismo que `archivo_index`) |
| `rutaOrigen` | String | Sí | Path original en SFTP origen |
| `rutaDestino` | String | Sí | Path calculado en SFTP destino |
| `nombre` | String | Sí | Nombre del archivo |
| `status` | String | Sí | `SUCCESS` o `FAILED` |
| `processedAt` | Date | Sí | Timestamp de procesamiento |
| `errorMessage` | String | No | Mensaje de error (solo si `FAILED`) |

**Índices:**

```javascript
// Índice único en idUnico
db.processed_files.createIndex(
    { "idUnico": 1 }, 
    { unique: true, name: "idx_idUnico_unique" }
)

// Índice compuesto para consultas por status y fecha
db.processed_files.createIndex(
    { "status": 1, "processedAt": -1 }, 
    { name: "idx_status_processedAt" }
)

// Índice para búsquedas por fecha
db.processed_files.createIndex(
    { "processedAt": -1 }, 
    { name: "idx_processedAt" }
)
```

**Script de Creación:**

```javascript
use dvsmart_reorganization_dev

db.createCollection("processed_files")

db.processed_files.createIndex(
    { "idUnico": 1 }, 
    { unique: true, name: "idx_idUnico_unique" }
)

db.processed_files.createIndex(
    { "status": 1, "processedAt": -1 }, 
    { name: "idx_status_processedAt" }
)

db.processed_files.createIndex(
    { "processedAt": -1 }, 
    { name: "idx_processedAt" }
)

// Verificar índices creados
db.processed_files.getIndexes()
```

**Ejemplos de Documentos:**

**Archivo procesado exitosamente:**
```javascript
{
    "_id": ObjectId("674c5f2b3c4d5e6f7a8b9c0d"),
    "idUnico": "file1-unique-id",
    "rutaOrigen": "/home/testuser/upload/origin/dir1/file1.txt",
    "rutaDestino": "/home/testuser/upload/destination/a1/b2/c3/file1.txt",
    "nombre": "file1.txt",
    "status": "SUCCESS",
    "processedAt": ISODate("2025-12-13T22:35:10.123Z"),
    "errorMessage": null
}
```

**Archivo con error:**
```javascript
{
    "_id": ObjectId("674c5f2b3c4d5e6f7a8b9c0e"),
    "idUnico": "file2-unique-id",
    "rutaOrigen": "/home/testuser/upload/origin/dir1/file2.pdf",
    "rutaDestino": "/home/testuser/upload/destination/d4/e5/f6/file2.pdf",
    "nombre": "file2.pdf",
    "status": "FAILED",
    "processedAt": ISODate("2025-12-13T22:35:15.456Z"),
    "errorMessage": "Failed to read file from origin SFTP: Permission denied"
}
```

---

### Datos de Prueba

#### Script Completo de Inicialización

```bash
#!/bin/bash
# scripts/init-mongodb.sh

echo "Inicializando base de datos MongoDB..."

mongosh mongodb://localhost:27017/dvsmart_reorganization_dev <<EOF

// Eliminar colecciones existentes (opcional)
db.archivo_index.drop();
db.processed_files.drop();

// Crear colección archivo_index
db.createCollection("archivo_index");
db.archivo_index.createIndex({ "idUnico": 1 }, { unique: true });

// Insertar archivos de prueba
db.archivo_index.insertMany([
    {
        idUnico: "file1-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/dir1/file1.txt",
        nombre: "file1.txt",
        mtime: new Date()
    },
    {
        idUnico: "file2-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/dir1/file2.pdf",
        nombre: "file2.pdf",
        mtime: new Date()
    },
    {
        idUnico: "file3-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/dir2/file3.jpg",
        nombre: "file3.jpg",
        mtime: new Date()
    },
    {
        idUnico: "file4-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/dir3/file4.doc",
        nombre: "file4.doc",
        mtime: new Date()
    },
    {
        idUnico: "file5-unique-id",
        rutaOrigen: "/home/testuser/upload/origin/file5.txt",
        nombre: "file5.txt",
        mtime: new Date()
    }
]);

// Crear colección processed_files
db.createCollection("processed_files");
db.processed_files.createIndex({ "idUnico": 1 }, { unique: true });
db.processed_files.createIndex({ "status": 1, "processedAt": -1 });
db.processed_files.createIndex({ "processedAt": -1 });

print("✓ Base de datos inicializada correctamente");
print("✓ Archivos insertados: " + db.archivo_index.countDocuments());

EOF

echo "✓ MongoDB inicializado"
```

**Ejecutar script:**
```bash
chmod +x scripts/init-mongodb.sh
./scripts/init-mongodb.sh
```

---

### Consultas Útiles

#### Contar archivos por status
```javascript
db.processed_files.aggregate([
    {
        $group: {
            _id: "$status",
            count: { $sum: 1 }
        }
    }
])
```

#### Archivos fallidos en las últimas 24 horas
```javascript
db.processed_files.find({
    status: "FAILED",
    processedAt: { 
        $gte: new ISODate(new Date().getTime() - 24*60*60*1000) 
    }
}).sort({ processedAt: -1 })
```

#### Archivos pendientes de procesar
```javascript
db.archivo_index.aggregate([
    {
        $lookup: {
            from: "processed_files",
            localField: "idUnico",
            foreignField: "idUnico",
            as: "processed"
        }
    },
    {
        $match: {
            processed: { $eq: [] }
        }
    },
    {
        $project: {
            processed: 0
        }
    }
])
```

#### Estadísticas de procesamiento
```javascript
db.processed_files.aggregate([
    {
        $group: {
            _id: null,
            total: { $sum: 1 },
            exitosos: {
                $sum: { $cond: [{ $eq: ["$status", "SUCCESS"] }, 1, 0] }
            },
            fallidos: {
                $sum: { $cond: [{ $eq: ["$status", "FAILED"] }, 1, 0] }
            },
            ultimoProcesado: { $max: "$processedAt" }
        }
    }
])
```

---

## 🚀 Ejecución

### Desarrollo Local

```bash
# Con Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Con JAR
java -jar target/dvsmart_reorganization_api.jar --spring.profiles.active=dev
```

### Producción

```bash
# Configurar variables de entorno (ver sección Configuración)
export SFTP_ORIGIN_HOST=...
export SFTP_ORIGIN_USER=...
# ... resto de variables

# Ejecutar con perfil prod
java -jar target/dvsmart_reorganization_api.jar --spring.profiles.active=prod
```

### Con Docker (Opcional)

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/dvsmart_reorganization_api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build
docker build -t dvsmart-reorganization-api:1.0.0 .

# Run
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SFTP_ORIGIN_HOST=... \
  --name dvsmart-api \
  dvsmart-reorganization-api:1.0.0
```

---

## 📡 API REST

### Base URL
```
http://localhost:8080/api
```

### Endpoints

#### 1. Iniciar Reorganización Completa

Inicia un job batch para reorganizar todos los archivos indexados.

**Request:**
```http
POST /api/batch/reorganize/full
Content-Type: application/json
```

**Response (202 Accepted):**
```json
{
    "message": "Batch job started successfully",
    "jobExecutionId": 1,
    "status": "ACCEPTED"
}
```

**Códigos de Estado:**
- `202 Accepted` - Job iniciado correctamente
- `500 Internal Server Error` - Error al iniciar el job

**Ejemplo con cURL:**
```bash
curl -X POST http://localhost:8080/api/batch/reorganize/full
```

**Ejemplo con HTTPie:**
```bash
http POST http://localhost:8080/api/batch/reorganize/full
```

**Ejemplo con Postman:**
```
Method: POST
URL: http://localhost:8080/api/batch/reorganize/full
Headers: (ninguno necesario)
Body: (vacío)
```

---

### Actuator Endpoints

Spring Boot Actuator proporciona endpoints de monitoreo.

#### Health Check
```http
GET /actuator/health
```

**Response:**
```json
{
    "status": "UP",
    "components": {
        "db": {
            "status": "UP",
            "details": {
                "database": "MongoDB",
                "validationQuery": "ismaster()"
            }
        },
        "diskSpace": {
            "status": "UP",
            "details": {
                "total": 500000000000,
                "free": 250000000000,
                "threshold": 10485760
            }
        }
    }
}
```

#### Batch Jobs Info
```http
GET /actuator/batch
```

#### Métricas
```http
GET /actuator/metrics
```

---

## 📊 Monitoreo

### Logs

Los logs se escriben en:
- **Consola**: Para desarrollo
- **Archivo**: `logs/reorganization.log` (rotación automática)

**Niveles de log por componente:**

| Componente | Nivel | Descripción |
|------------|-------|-------------|
| `com.indra.minsait.dvsmart.reorganization` | DEBUG | Logs de aplicación |
| `org.springframework.batch` | INFO | Logs de Spring Batch |
| `org.springframework.integration.sftp` | DEBUG | Logs de SFTP |
| `org.springframework.data.mongodb` | INFO | Logs de MongoDB |

**Ejemplo de logs exitosos:**
```
2025-12-13 22:30:15 - BatchReorganizeController - Received request to start full reorganization
2025-12-13 22:30:15 - StartReorganizeFullService - Job launched successfully. JobExecutionId: 1
2025-12-13 22:30:16 - SftpMoveAndAuditItemWriter - Successfully transferred: /origin/file1.txt -> /dest/a1/b2/c3/file1.txt
```

### Métricas JVM

Disponibles en `/actuator/metrics`:

- `jvm.memory.used`
- `jvm.threads.live`
- `jvm.gc.pause`
- `process.cpu.usage`

### Monitoreo de Jobs

**Consultar estado del job en MongoDB:**

Spring Batch almacena metadatos en colecciones:
- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_STEP_EXECUTION`

```javascript
// Ver últimas ejecuciones
db.BATCH_JOB_EXECUTION.find().sort({ START_TIME: -1 }).limit(10)

// Ver estadísticas del último job
db.BATCH_STEP_EXECUTION.find({ 
    JOB_EXECUTION_ID: 1 
})
```

---

## 🎯 Algoritmo de Particionado Hash

### Descripción

Los archivos se distribuyen en una estructura de directorios basada en el hash SHA-256 de su ruta + nombre.

### Configuración

```java
private static final int PARTITION_DEPTH = 3;      // Niveles de directorios
private static final int CHARS_PER_LEVEL = 2;      // Caracteres por nivel
```

### Ejemplo

**Archivo origen:**
```
/home/testuser/upload/origin/documents/report.pdf
```

**Cálculo:**
1. Input: `"/home/testuser/upload/origin/documents/report.pdf" + "report.pdf"`
2. SHA-256: `a1b2c3d4e5f6...` (64 caracteres hex)
3. Particionado: Tomar 2 caracteres × 3 niveles = `a1/b2/c3`
4. Path destino: `/data/reorganized/a1/b2/c3/report.pdf`

**Resultado:**
```
/data/reorganized/
├── a1/
│   └── b2/
│       └── c3/
│           └── report.pdf
├── d4/
│   └── e5/
│       └── f6/
│           └── invoice.pdf
└── ...
```

### Ventajas

- **Distribución uniforme**: Cada directorio tiene ~256 subdirectorios (16²)
- **Escalabilidad**: Soporta millones de archivos sin degradación
- **Performance**: Búsquedas rápidas en filesystems con índices de directorios
- **Determinismo**: El mismo archivo siempre va al mismo path

---

## 🔧 Troubleshooting

### Error: "Job not found in registry: BATCH-REORG-FULL"

**Causa**: El `JobRegistry` no encuentra el job.

**Solución**: Verificar que existe el bean `jobRegistry()` en `BatchReorgFullConfig`:
```java
@Bean
public JobRegistry jobRegistry() {
    return new MapJobRegistry();
}
```

---

### Error: "Failed to read file from origin SFTP: Permission denied"

**Causa**: El usuario SFTP no tiene permisos de lectura.

**Solución**:
1. Verificar credenciales en `application.properties`
2. Verificar permisos del archivo en SFTP origen:
   ```bash
   sftp -P 2222 testuser@localhost
   ls -la /upload/origin/dir1/
   ```

---

### Error: "Failed to transfer file to destination SFTP: Connection timeout"

**Causa**: Timeout de conexión SFTP.

**Solución**:
1. Aumentar timeout en configuración:
   ```properties
   sftp.dest.timeout=60000
   ```
2. Verificar conectividad de red
3. Verificar firewall/security groups

---

### Error: "Duplicate key error collection: processed_files"

**Causa**: Intento de procesar el mismo archivo dos veces.

**Solución**: Este es un comportamiento esperado. El sistema evita reprocesar archivos. Si necesitas reprocesar:
```javascript
// Eliminar registro de auditoría
db.processed_files.deleteOne({ idUnico: "file1-unique-id" })
```

---

### Performance: Procesamiento muy lento

**Diagnóstico**:
```javascript
// Ver archivos procesados por minuto
db.processed_files.aggregate([
    {
        $group: {
            _id: {
                $dateToString: {
                    format: "%Y-%m-%d %H:%M",
                    date: "$processedAt"
                }
            },
            count: { $sum: 1 }
        }
    },
    { $sort: { _id: -1 } },
    { $limit: 10 }
])
```

**Soluciones**:
1. Aumentar `thread-pool-size`:
   ```properties
   batch.thread-pool-size=30
   ```
2. Aumentar `chunk-size`:
   ```properties
   batch.chunk-size=200
   ```
3. Aumentar pool de conexiones SFTP:
   ```properties
   sftp.origin.pool.size=20
   sftp.dest.pool.size=20
   ```

---

### MongoDB: Out of Memory

**Causa**: Dataset muy grande sin streaming.

**Verificación**: El código ya usa `MongoCursorItemReader` con streaming. Si persiste:

```properties
# Reducir batch size interno del cursor
# (modificar en MongoIndexedFileItemReader.java)
.batchSize(50)  // Reducir de 100 a 50
```

---

## 📚 Documentación Adicional

### Spring Batch
- [Documentación oficial](https://spring.io/projects/spring-batch)
- [Guía de referencia 6.0](https://docs.spring.io/spring-batch/docs/current/reference/html/)

### Spring Integration SFTP
- [Documentación oficial](https://docs.spring.io/spring-integration/docs/current/reference/html/sftp.html)

### MongoDB
- [Manual de MongoDB](https://docs.mongodb.com/manual/)
- [Índices en MongoDB](https://docs.mongodb.com/manual/indexes/)

---

## 📄 Licencia

```
Copyright (c) 2025 Indra Sistemas, S.A. All Rights Reserved.
http://www.indracompany.com/

The contents of this file are owned by Indra Sistemas, S.A. copyright holder.
This file can only be copied, distributed and used all or in part with the
written permission of Indra Sistemas, S.A, or in accordance with the terms and
conditions laid down in the agreement / contract under which supplied.
```

---

## 👥 Contacto

**Autor**: hahuaranga@indracompany.com  
**Fecha Creación**: 11-12-2025  
**Versión**: 1.0.1-SNAPSHOT
