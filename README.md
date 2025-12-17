# DvSmart Reorganization API

## 📋 Descripción

API de reorganización masiva de archivos que transfiere millones de archivos desde un servidor SFTP de origen hacia un servidor SFTP de destino, organizándolos automáticamente en una estructura de directorios basada en hash SHA-256 para optimizar el acceso y distribución.

### Características Principales
- **Transferencia Masiva**: Optimizada para millones de archivos con Spring Batch
- **Arquitectura Hexagonal**: Separación clara de responsabilidades con puertos/adaptadores
- **Hash Partitioning**: Organización automática basada en SHA-256
- **Procesamiento Asíncrono**: Paralelización con AsyncItemProcessor y ThreadPool
- **SFTP Streaming**: Transferencia directa sin almacenamiento temporal
- **Auditoría Completa**: Trazabilidad de todos los archivos procesados
- **Configuración Externa**: Propiedades configurables por entorno

## 🏗️ Arquitectura

### Diagrama de Flujo
```
┌─────────────────────────────────────────────────────────────┐
│                  BatchReorganizeController                  │
│                  (REST API /api/batch/reorganize/full)      │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                StartReorganizeFullService                   │
│                (Application Service)                         │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                 BatchReorgFullConfig                        │
│                 (Spring Batch Job)                          │
└──────────────┬──────────────┬──────────────┬────────────────┘
               │              │              │
    ┌──────────▼─────┐  ┌────▼─────────┐  ┌─▼────────────────┐
    │MongoIndexed    │  │Composite     │  │SftpMoveAndAudit │
    │FileItemReader  │  │Processor     │  │ItemWriter       │
    │(MongoDB Cursor)│  │(Hash Calc)   │  │(SFTP Transfer)  │
    └────────────────┘  └──────────────┘  └─────────────────┘
               │              │              │
    ┌──────────▼─────┐  ┌────▼─────────┐  ┌─▼────────────────┐
    │Disorganized    │  │ArchivoLegacy │  │ProcessedArchivo  │
    │FilesIndex      │  │(Domain)      │  │(Audit)           │
    │(MongoDB)       │  │              │  │                  │
    └────────────────┘  └──────────────┘  └─────────────────┘
```

### Componentes Clave
1. **Controller REST**: Expone endpoint para iniciar reorganización
2. **Spring Batch Job**: Orquesta todo el proceso de transferencia
3. **Reader MongoDB**: Lee eficientemente millones de registros con cursor
4. **Processor Hash**: Calcula SHA-256 para estructura de directorios
5. **Writer SFTP**: Transfiere y audita archivos en paralelo

## 📁 Estructura del Proyecto

```
dvsmart_reorganization_api/
├── src/main/java/com/indra/minsait/dvsmart/reorganization/
│   ├── adapter/
│   │   ├── in/web/
│   │   │   └── BatchReorganizeController.java         # REST Endpoint
│   │   ├── out/batch/
│   │   │   ├── config/BatchReorgFullConfig.java       # Configuración Batch
│   │   │   ├── reader/MongoIndexedFileItemReader.java # MongoDB Reader
│   │   │   └── writer/SftpMoveAndAuditItemWriter.java # SFTP Writer
│   │   ├── out/persistence/mongodb/
│   │   │   ├── entity/
│   │   │   │   ├── DisorganizedFilesIndexDocument.java # Índice origen
│   │   │   │   └── OrganizedFilesIndexDocument.java    # Índice destino
│   │   │   └── *RepositoryImpl.java                    # Implementaciones
│   │   └── out/sftp/
│   │       ├── SftpOriginRepositoryImpl.java          # SFTP Origen
│   │       └── SftpDestinationRepositoryImpl.java     # SFTP Destino
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/StartReorganizeFullUseCase.java     # Puerto entrada
│   │   │   └── out/                                   # Puertos salida
│   │   │       ├── DisorganizedFilesIndexRepository.java
│   │   │       ├── OrganizedFilesIndexRepository.java
│   │   │       ├── SftpOriginRepository.java
│   │   │       └── SftpDestinationRepository.java
│   │   └── service/
│   │       └── StartReorganizeFullService.java        # Servicio app
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ArchivoLegacy.java                     # Modelo dominio
│   │   │   └── ProcessedArchivo.java                  # Auditoría
│   │   └── service/
│   │       └── FileReorganizationService.java         # Lógica hash
│   └── infrastructure/
│       ├── config/
│       │   ├── BatchConfigProperties.java             # Props Batch
│       │   ├── MongoConfigProperties.java             # Props MongoDB
│       │   └── SftpConfigProperties.java              # Props SFTP
│       ├── exception/GlobalExceptionHandler.java      # Manejo errores
│       ├── sftp/SftpSessionFactoryConfig.java         # Config SFTP
│       └── ServiceApplication.java                    # Main class
├── src/main/resources/
│   ├── application.properties                         # Configuración
│   └── license-header.txt                            # Copyright header
├── pom.xml                                            # Dependencias Maven
└── README.md                                         # Esta documentación
```

## ⚙️ Configuración

### Requisitos Previos
- **Java 21** JDK
- **Maven 3.6+**
- **MongoDB 4.4+** (para índices de archivos)
- **Servidores SFTP** (origen y destino accesibles)
- **8GB RAM mínimo** (recomendado para procesamiento masivo)

### Configuración de Propiedades (`application.properties`)

```properties
# ============================================================================
# CONFIGURACIÓN GENERAL
# ============================================================================
spring.application.name=dvsmart-reorganization-api
server.port=8080

# ============================================================================
# MONGODB - Índices de Archivos
# ============================================================================
spring.mongodb.uri=mongodb://usuario:contraseña@host:27017/dvsmart_reorganization
mongo.disorganized-files-index=disorganized-files-index  # Colección origen

# ============================================================================
# SPRING BATCH - Configuración Procesamiento
# ============================================================================
spring.batch.job.enabled=false  # Deshabilitar auto-inicio

# Tamaño de chunk (registros por transacción)
batch.chunk-size=100

# Pool de threads para procesamiento paralelo
batch.thread-pool-size=20

# Capacidad de cola para tareas pendientes
batch.queue-capacity=1000

# ============================================================================
# SFTP ORIGEN - Archivos Desorganizados
# ============================================================================
sftp.origin.host=sftp-origen.tudominio.com
sftp.origin.port=22
sftp.origin.user=usuario_origen
sftp.origin.password=contraseña_origen
sftp.origin.base-dir=/ruta/origen/archivos
sftp.origin.timeout=30000
sftp.origin.pool.size=10  # Conexiones simultáneas

# ============================================================================
# SFTP DESTINO - Archivos Organizados
# ============================================================================
sftp.dest.host=sftp-destino.tudominio.com
sftp.dest.port=22
sftp.dest.user=usuario_destino
sftp.dest.password=contraseña_destino
sftp.dest.base-dir=/data/reorganized  # Base para hash partitioning
sftp.dest.timeout=30000
sftp.dest.pool.size=10

# ============================================================================
# LOGGING - Monitoreo y Debug
# ============================================================================
logging.level.com.indra.minsait.dvsmart.reorganization=INFO
logging.level.org.springframework.batch=INFO
logging.level.org.springframework.integration.sftp=WARN
logging.file.name=logs/reorganization.log
logging.file.max-size=10MB
logging.file.max-history=30
```

### 🔧 Configuración del Hash Partitioning

#### Algoritmo de Organización
Los archivos se organizan automáticamente usando SHA-256:

```java
// Ejemplo: Archivo "/data/legacy/files/documento.pdf"
String input = "/data/legacy/files/documento.pdf" + "documento.pdf";
String hash = sha256(input); // Ej: "a1b2c3d4e5f6..."

// Estructura resultante (3 niveles, 2 caracteres cada uno):
// /data/reorganized/a1/b2/c3/documento.pdf
```

#### Parámetros Configurables
En `FileReorganizationService.java`:

```java
private static final int PARTITION_DEPTH = 3;      // Niveles de directorios
private static final int CHARS_PER_LEVEL = 2;      // Caracteres por nivel
```

#### Cálculo de Distribución
- **256^6 posibilidades** (16^6 = 16.7M combinaciones)
- **Distribución uniforme** gracias a SHA-256
- **Profundidad ajustable** según necesidades

#### Ejemplos de Rutas Generadas
| Archivo Origen | Hash SHA-256 (primeros 6 chars) | Ruta Destino |
|----------------|----------------------------------|--------------|
| `/data/file1.txt` | `a1b2c3d4e5...` | `/data/reorganized/a1/b2/c3/file1.txt` |
| `/docs/report.pdf` | `f6e5d4c3b2...` | `/data/reorganized/f6/e5/d4/report.pdf` |
| `/images/photo.jpg` | `1a2b3c4d5e...` | `/data/reorganized/1a/2b/3c/photo.jpg` |

#### Beneficios del Hash Partitioning
1. **Distribución Uniforme**: Evita directorios con millones de archivos
2. **Búsqueda Eficiente**: Puede calcularse la ruta sin consultar DB
3. **Escalabilidad**: Fácil de expandir con más niveles
4. **Consistencia**: Mismo archivo → misma ubicación siempre

### Perfiles Maven
- **dev** (activo por defecto): Desarrollo local
- **prod**: Configuración producción

```bash
# Desarrollo
mvn spring-boot:run -Pdev

# Producción
mvn spring-boot:run -Pprod
```

## 🚀 Compilación y Ejecución

### 1. Compilar el Proyecto
```bash
mvn clean package
```

### 2. Ejecutar la Aplicación
```bash
# Modo desarrollo
java -jar target/dvsmart_reorganization_api.jar

# Modo producción con propiedades
java -jar target/dvsmart_reorganization_api.jar \
  --spring.profiles.active=prod \
  --sftp.origin.host=sftp.miservidor.com \
  --sftp.dest.host=sftp.destino.com

# Con Maven
mvn spring-boot:run
```

### 3. Verificar Estado
```bash
curl http://localhost:8080/actuator/health
```
Respuesta esperada:
```json
{
  "status": "UP",
  "components": {
    "mongo": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

## 📊 Endpoints API

### 1. Iniciar Reorganización Completa
```http
POST /api/batch/reorganize/full
Accept: application/json

Response 202 (Accepted):
{
  "message": "Batch job started successfully",
  "jobExecutionId": 12345,
  "status": "ACCEPTED"
}
```

### 2. Monitoreo con Spring Actuator
```bash
# Health Check
GET /actuator/health

# Información aplicación
GET /actuator/info

# Métricas (rendimiento, memoria, batch)
GET /actuator/metrics

# Jobs Spring Batch
GET /actuator/batch/jobs
GET /actuator/batch/jobs/{jobId}/executions
```

### 3. Consultar Estado de Job
```bash
# Verificar job específico
GET /actuator/batch/jobs/BATCH-REORG-FULL/executions
```

## 🗄️ Base de Datos MongoDB

### Colecciones

#### 1. `disorganized-files-index` (Origen)
Índice de archivos desorganizados (pre-existente).

```javascript
{
  "_id": ObjectId("..."),
  "idUnico": "sha256_hash_unique",
  "rutaOrigen": "/data/legacy/files/subdir/document.pdf",
  "nombre": "document.pdf",
  "mtime": ISODate("2025-12-15T10:30:00Z"),
  "tamanio": NumberLong(2048576),
  "extension": "pdf",
  "indexadoEn": ISODate("2025-12-15T11:00:00Z")
}
```

#### 2. `organized-files-index` (Destino/Auditoría)
Registro de archivos transferidos y organizados.

```javascript
{
  "_id": ObjectId("..."),
  "idUnico": "sha256_hash_unique",
  "rutaOrigen": "/data/legacy/files/document.pdf",
  "rutaDestino": "/data/reorganized/a1/b2/c3/document.pdf",
  "nombre": "document.pdf",
  "status": "SUCCESS",  // o "FAILED"
  "processedAt": ISODate("2025-12-17T14:25:30Z"),
  "errorMessage": null  // Solo si FAILED
}
```

### Índices Recomendados
```javascript
// Índice para búsquedas por estado
db.getCollection('organized-files-index').createIndex({ 
  "status": 1, 
  "processedAt": -1 
})

// Índice para estadísticas
db.getCollection('organized-files-index').createIndex({ 
  "rutaDestino": 1 
})
```

## ⚡ Rendimiento y Optimización

### Parámetros Ajustables

| Parámetro | Valor Default | Rango Recomendado | Impacto |
|-----------|--------------|-------------------|---------|
| `batch.chunk-size` | 100 | 50-500 | Memoria vs Throughput |
| `batch.thread-pool-size` | 20 | CPU cores × 2-4 | Paralelismo |
| `sftp.origin.pool.size` | 10 | 10-50 | Lectura simultánea |
| `sftp.dest.pool.size` | 10 | 10-50 | Escritura simultánea |
| `batch.queue-capacity` | 1000 | 1000-10000 | Buffer picos |

### Estimación de Rendimiento
| Escenario | Throughput Estimado | Factores Limitantes |
|-----------|---------------------|---------------------|
| Archivos pequeños (<1MB) | 500-2000 files/sec | Red, I/O Disco SFTP |
| Archivos medianos (1-10MB) | 100-500 files/sec | Ancho de banda red |
| Archivos grandes (>10MB) | 10-50 files/sec | Latencia red |

### Monitoreo durante Ejecución
```bash
# Ver logs en tiempo real
tail -f logs/reorganization.log | grep -E "(Successfully transferred|Failed to process)"

# Métricas de batch
curl -s http://localhost:8080/actuator/metrics/spring.batch.job | jq .

# Uso memoria
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq .
```

## 🔧 Mantenimiento y Operación

### Limpieza de Datos
```javascript
// Eliminar registros antiguos (ejemplo > 90 días)
db.getCollection('organized-files-index').deleteMany({
  "processedAt": { 
    "$lt": new Date(Date.now() - 90 * 24 * 60 * 60 * 1000) 
  },
  "status": "SUCCESS"
});

// Mantener solo fallos recientes
db.getCollection('organized-files-index').deleteMany({
  "status": "FAILED",
  "processedAt": { 
    "$lt": new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) 
  }
});
```

### Backup y Recovery
```bash
# Backup índices organizados
mongodump --uri="mongodb://localhost:27017/dvsmart_reorganization" \
  --collection="organized-files-index" \
  --gzip \
  --out=/backup/mongodb_$(date +%Y%m%d)

# Restaurar en caso necesario
mongorestore --uri="mongodb://localhost:27017/dvsmart_reorganization" \
  --collection="organized-files-index" \
  --gzip \
  /backup/mongodb_20251217/dvsmart_reorganization/organized-files-index.bson.gz
```

### Rotación de Logs
Configuración automática en `application.properties`:
- 10MB máximo por archivo
- 30 archivos de historial
- 500MB capacidad total

## 🐛 Solución de Problemas

### Problemas Comunes

#### 1. Conexión SFTP Falla
**Síntomas**:
- `Connection refused` o `Timeout exceeded`
- Errores en `SftpOriginRepositoryImpl` o `SftpDestinationRepositoryImpl`

**Solución**:
```properties
# Aumentar timeout
sftp.origin.timeout=60000
sftp.dest.timeout=60000

# Verificar credenciales
sftp.origin.user=usuario_correcto
sftp.origin.password=contraseña_correcta

# Probar conexión manualmente
sftp -P 22 usuario@host.sftp.com
```

#### 2. Rendimiento Lento
**Síntomas**:
- Throughput < 100 archivos/segundo
- Alta CPU en servidor

**Solución**:
```properties
# Aumentar paralelismo
batch.thread-pool-size=40
batch.chunk-size=50  # Reducir para menos memoria

# Aumentar conexiones SFTP
sftp.origin.pool.size=20
sftp.dest.pool.size=20
```

#### 3. MongoDB Saturado
**Síntomas**:
- Timeouts en operaciones
- Alta carga CPU en MongoDB

**Solución**:
```properties
# Reducir chunk size
batch.chunk-size=50

# Considerar índices adecuados
# Verificar conexión directa (no pasar por balanceador)
```

### Comandos de Diagnóstico
```bash
# Ver jobs activos
curl http://localhost:8080/actuator/batch/jobs

# Ver métricas de ejecución
curl http://localhost:8080/actuator/metrics/spring.batch.job | jq '.measurements'

# Ver conexiones SFTP activas
grep -i "session" logs/reorganization.log | tail -20

# Monitorizar throughput
watch -n 5 'grep "Successfully transferred" logs/reorganization.log | wc -l'
```

## 🧪 Pruebas

### Pruebas Unitarias
```bash
# Ejecutar todas las pruebas
mvn test

# Ejecutar pruebas específicas
mvn test -Dtest=FileReorganizationServiceTest
```

### Pruebas de Integración
1. Configurar entorno de prueba:
   - MongoDB local en puerto 27017
   - Servidores SFTP de prueba (puede usar testcontainers)

2. Ejecutar con datos de prueba:
```bash
# Usar directorio pequeño para pruebas
sftp.origin.base-dir=/test/small_dataset
sftp.dest.base-dir=/test/reorganized_output

# Ejecutar reorganización limitada
mvn spring-boot:run -Dtest.mode=true
```

### Validación de Resultados
```javascript
// Verificar integridad después de ejecución
db.getCollection('disorganized-files-index').countDocuments();
db.getCollection('organized-files-index').countDocuments({ status: "SUCCESS" });

// Verificar que todos los archivos origen tienen su contraparte destino
db.getCollection('disorganized-files-index').aggregate([
  {
    $lookup: {
      from: "organized-files-index",
      localField: "idUnico",
      foreignField: "idUnico",
      as: "organized"
    }
  },
  {
    $match: {
      organized: { $size: 0 }
    }
  },
  { $count: "missing_files" }
]);
```

## 🚢 Despliegue en Producción

### Requisitos Hardware
| Componente | Mínimo | Recomendado |
|------------|--------|-------------|
| CPU | 4 cores | 8+ cores |
| RAM | 8GB | 16-32GB |
| Disco | 50GB | 200GB+ |
| Red | 100Mbps | 1Gbps+ |

### Configuración Producción
```properties
# application-prod.properties
spring.profiles.active=prod

# MongoDB Cluster
spring.mongodb.uri=mongodb://user:pass@mongodb1:27017,mongodb2:27017/dvsmart_reorganization?replicaSet=rs0

# SFTP con conexiones seguras
sftp.origin.host=prod-sftp-origin.company.com
sftp.dest.host=prod-sftp-dest.company.com

# Optimización producción
batch.thread-pool-size=40
sftp.origin.pool.size=30
sftp.dest.pool.size=30
batch.queue-capacity=5000

# Logging producción
logging.level.root=WARN
logging.level.com.indra.minsait.dvsmart.reorganization=INFO
```

### Consideraciones de Seguridad
1. **Credenciales**: Usar Vault o Secrets Manager
2. **Conexiones**: SFTP sobre VPN o conexiones privadas
3. **Firewall**: Restringir puertos necesarios (8080, 27017)
4. **SSL/TLS**: Para MongoDB y SFTP si es posible
5. **Auditoría**: Mantener logs de todas las operaciones

## 🔄 CI/CD (Opcional)

### Pipeline Ejemplo (.gitlab-ci.yml)
```yaml
stages:
  - build
  - test
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

build:
  stage: build
  image: maven:3.8.4-openjdk-21
  script:
    - mvn clean compile
  artifacts:
    paths:
      - target/

test:
  stage: test
  image: maven:3.8.4-openjdk-21
  services:
    - mongo:4.4
  script:
    - mvn test

deploy-prod:
  stage: deploy
  image: alpine:latest
  script:
    - apk add --no-cache openssh-client
    - scp target/dvsmart_reorganization_api.jar user@prod-server:/opt/app/
    - ssh user@prod-server "systemctl restart reorganization-api"
  only:
    - master
```

## 📝 Licencia y Copyright

Todos los archivos `.java` incluyen automáticamente headers de copyright usando `license-maven-plugin`.

Para aplicar/actualizar headers:
```bash
mvn license:format
```

Archivo de configuración: `src/main/resources/license-header.txt`

## 🔮 Roadmap y Mejoras Futuras

### Próximas Versiones
1. **Reorganización Parcial**: Solo archivos modificados desde última ejecución
2. **Dashboard Web**: Interfaz gráfica para monitoreo en tiempo real
3. **Multi-origen**: Soporte para múltiples servidores SFTP origen
4. **Validación Post-transferencia**: Checksum comparativo
5. **Estimación Tiempo**: Cálculo dinámico de tiempo restante
6. **Pausa/Reanudación**: Control granular de ejecución
7. **Exportación Reportes**: CSV/PDF de estadísticas

### Optimizaciones Planeadas
- Compresión durante transferencia para archivos grandes
- Cache local de directorios ya creados en SFTP destino
- Balanceo dinámico de threads según throughput
- Reintentos inteligentes con backoff exponencial

## 📚 Recursos y Referencias

- [Spring Batch Documentation](https://docs.spring.io/spring-batch/reference/)
- [Spring Integration SFTP](https://docs.spring.io/spring-integration/reference/sftp.html)
- [MongoDB Java Driver](https://mongodb.github.io/mongo-java-driver/)
- [SSHJ Library](https://github.com/hierynomus/sshj)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

## 🤝 Soporte y Contacto

**Equipo de Mantenimiento**: DvSmart Reorganization Team  
**Contacto**: hahuaranga@indracompany.com  
**Repositorio**: [Enlace interno al repositorio]  
**Documentación Técnica**: [Enlace a documentación detallada]

### Reporte de Issues
Al encontrar un problema, incluir:
1. Versión de la aplicación
2. Configuración relevante (sin credenciales)
3. Logs de error completos
4. Pasos para reproducir
5. Impacto en producción

---
**Última Actualización**: Diciembre 2025  
**Versión Actual**: 1.0.1-SNAPSHOT  
**Estado**: Activo en Desarrollo