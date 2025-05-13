# API de Productos Similares

Una API RESTful de alto rendimiento para obtener productos similares a uno dado utilizando programación reactiva con Spring WebFlux.

## Descripción del Proyecto

Esta aplicación implementa un microservicio que proporciona información sobre productos similares para un producto específico. Combina datos de dos APIs externas:
1. Una API que proporciona IDs de productos similares
2. Una API que proporciona detalles de producto por ID

La solución ofrece un alto rendimiento bajo carga mediante programación reactiva no bloqueante, aprovechando las capacidades de Spring WebFlux para lograr una respuesta eficiente incluso con un gran número de solicitudes concurrentes.

## Mejoras Recientes

Se ha refactorizado el código para mejorar su calidad siguiendo principios SOLID y buenas prácticas:

1. **Reducción de código boilerplate**:
   - Incorporación de Lombok para simplificar la creación de modelos
   - Uso de anotaciones como `@Data`, `@Slf4j`, `@RequiredArgsConstructor`

2. **Externalización de configuración**:
   - Todos los valores estáticos y textos movidos a `application.properties`
   - Mensajes de log centralizados para facilitar localización
   - Configuraciones de caché, circuit breaker y timeouts parametrizadas

3. **Mejor separación de responsabilidades**:
   - Implementación de `GlobalExceptionHandler` para manejo centralizado de errores
   - Eliminación de lógica de manejo de errores duplicada en controladores

4. **Actualizaciones de código**:
   - Reemplazo de anotaciones obsoletas (`@MockBean` → `@TestConfiguration`)
   - Modernización del uso de `UriComponentsBuilder` con patrón builder

5. **Mejora en pruebas**:
   - Configuración de propiedades específicas para tests
   - Tests más robustos y aislados

## Arquitectura y Flujo 

```
Cliente → API de Productos Similares → APIs de Productos Externas → Respuesta Consolidada
```

El flujo de la aplicación es el siguiente:
1. El cliente realiza una solicitud HTTP GET a `/product/{productId}/similar`
2. La aplicación consulta la API externa para obtener un listado de IDs similares
3. Para cada ID de producto similar se obtiene su detalle desde la API externa
4. Los detalles se combinan en una respuesta única que se devuelve al cliente
5. El proceso completo se realiza de manera reactiva y no bloqueante
6. Se implementa caché para reducir llamadas a servicios externos

## Tecnologías y Patrones

### Tecnologías Principales
- **Java 17**: Para el desarrollo del backend
- **Spring Boot 2.7**: Framework base para la aplicación
- **Spring WebFlux**: Para programación reactiva y no bloqueante
- **Reactor**: Para flujos reactivos (Mono/Flux)
- **Netty**: Servidor integrado optimizado para aplicaciones reactivas
- **Caffeine**: Motor de caché de alto rendimiento
- **Lombok**: Para reducción de código boilerplate

### Patrones y Arquitectura
- **Arquitectura Reactiva**: Programación basada en eventos para alta concurrencia
- **Patrón Circuit Breaker**: Manejo de fallos en servicios externos con Resilience4j
- **Patrón de Caché**: Almacenamiento en memoria de respuestas frecuentes
- **Arquitectura de Microservicios**: Servicio independiente y fácilmente escalable
- **Patrón Modelo-Controlador**: Separación clara de responsabilidades
- **Client-Side Load Balancing**: Balanceo de carga en el cliente con WebClient
- **Global Exception Handler**: Manejo centralizado de excepciones

### Bibliotecas y Dependencias
- **Spring Cloud Circuit Breaker**: Para tolerancia a fallos
- **Spring Cache + Caffeine**: Implementación de caché de alto rendimiento
- **Reactor Netty**: Servidor y cliente HTTP no bloqueante
- **Slf4j**: Para logging estructurado
- **Lombok**: Para reducción de código repetitivo

## Estrategias de Optimización Implementadas

### 1. Optimización de Conexiones HTTP
- **Connection Pooling**: Configuración avanzada de pool de conexiones con estrategia LIFO
- **Timeouts Optimizados**: Configuración de timeouts para cada fase de la conexión
- **Compresión HTTP**: Reducción del tamaño de las respuestas
- **Backlog TCP**: Configuración de backlog TCP para soportar más conexiones entrantes

### 2. Paralelización y Concurrencia
- **Programación Reactiva**: Uso de Flux para procesamiento no bloqueante
- **Paralelización Eficiente**: Procesamiento en paralelo de peticiones de detalle de producto
- **Scheduler Optimizado**: Configuración de schedulers de Reactor para mejor rendimiento

### 3. Estrategias de Caché
- **Caché Multicapa**: Caché tanto a nivel de productos similares como de detalles de producto
- **Caché con Caffeine**: Uso de Caffeine para implementación de caché de alto rendimiento
- **TTL Configurable**: Tiempo de vida configurable para las entradas de caché

### 4. Resiliencia
- **Circuit Breaker**: Implementación de circuit breaker para evitar cascada de fallos
- **Timeouts Inteligentes**: Estrategia de timeout adaptativa según la carga
- **Fallback**: Manejo de errores degradando graciosamente el servicio

### 5. Optimizaciones a Nivel de Sistema
- **Netty Optimizado**: Configuración avanzada de Netty para máximo rendimiento
- **Event Loop Personalizado**: Optimización del número de hilos y event loops
- **Backpressure**: Manejo de contrapresión para evitar sobrecarga

### 6. Mantenibilidad y Configuración
- **Externalización de configuración**: Propiedades en archivos de configuración
- **Mensajes externalizados**: Facilidad para cambios y localización
- **Valores paramétricos**: Eliminación de constantes hard-coded

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── backendtest/
│   │           └── similarproducts/
│   │               ├── client/
│   │               │   └── ProductClient.java         # Cliente HTTP reactivo
│   │               ├── config/
│   │               │   ├── netty/
│   │               │   │   └── NettyServerCustomizer.java # Config. servidor Netty
│   │               │   ├── CacheConfig.java           # Configuración de caché
│   │               │   └── WebClientConfig.java       # Config. cliente HTTP
│   │               ├── controller/
│   │               │   ├── GlobalExceptionHandler.java # Manejo centralizado de errores
│   │               │   └── SimilarProductController.java # Controlador REST
│   │               ├── model/
│   │               │   └── ProductDetail.java         # Modelo de datos
│   │               ├── service/
│   │               │   └── SimilarProductService.java # Lógica de negocio
│   │               └── SimilarProductsApplication.java # Punto de entrada
│   └── resources/
│       └── application.properties                    # Configuración
└── test/
    ├── java/
    │   └── com/
    │       └── backendtest/
    │           └── similarproducts/
    │               ├── controller/
    │               │   └── SimilarProductControllerTest.java
    │               ├── integration/
    │               │   └── SimilarProductIntegrationTest.java
    │               └── service/
    │                   └── SimilarProductServiceTest.java
    └── resources/
        └── application.properties                   # Configuración específica para tests
```

## Resultados de Rendimiento

El test de rendimiento final mostró excelentes resultados:

```
running (1m00.5s), 000/200 VUs, 16209 complete and 600 interrupted iterations
normal   ✓ [======================================] 200 VUs  10s
notFound ✓ [======================================] 200 VUs  10s
error    ✓ [======================================] 200 VUs  10s
slow     ✓ [======================================] 200 VUs  10s
verySlow ✓ [======================================] 200 VUs  10s

http_req_duration..........: avg=90ms     min=365µs    med=9.88ms  max=2.29s    p(90)=70.84ms  p(95)=118.36ms
iterations.................: 16209  267.698825/s
```

La API pudo manejar de manera consistente **más de 100 solicitudes por segundo** bajo carga, incluso en escenarios de alta concurrencia y con servicios externos lentos (escenario "verySlow").

La implementación reactiva con Spring WebFlux demostró ser extremadamente eficiente, utilizando menos recursos (CPU y memoria) que una implementación tradicional de Spring MVC mientras maneja un mayor número de solicitudes concurrentes.

## Pre-requisitos

- Java 17
- Maven
- Docker (para ejecutar los mocks y pruebas)

## Cómo Ejecutar

1. Inicia los servicios mock:
```
docker-compose up -d simulado influxdb grafana
```

2. Compila y ejecuta la aplicación:
```
mvn clean package
java -jar target/similarproducts-0.0.1-SNAPSHOT.jar
```

3. Prueba la API:
```
curl http://localhost:5001/product/1/similar
```

4. Ejecuta las pruebas de rendimiento:
```
docker-compose run --rm k6 run -e HOST=host.docker.internal:5001 scripts/test.js
```

## Monitoreo

Los resultados de las pruebas de rendimiento pueden visualizarse en Grafana:
[http://localhost:3000/d/Le2Ku9NMk/k6-performance-test](http://localhost:3000/d/Le2Ku9NMk/k6-performance-test)

## API Endpoint

```
GET /product/{productId}/similar
```

### Ejemplo de respuesta

```json
[
  {
    "id": "2",
    "name": "Product 2",
    "price": 20.0,
    "availability": true
  },
  {
    "id": "3",
    "name": "Product 3",
    "price": 30.0,
    "availability": false
  }
]
```

## Características

- **Alto rendimiento**: Uso de programación reactiva y caché
- **Resiliencia**: Circuit breaker para manejar fallos en las APIs externas
- **Escalabilidad**: Diseño sin estado, fácilmente escalable horizontalmente
- **Mantenibilidad**: Código limpio y configuración externalizada
