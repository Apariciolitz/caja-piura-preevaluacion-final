# Caja Piura · Pre-evaluación Digital de Créditos (v2.0)

Plataforma web que permite a un cliente potencial de **Caja Piura** completar
un formulario de pre-evaluación crediticia y recibir, en segundos, un
resultado preliminar (*Pre-calificado* o *Requiere revisión*), simular su
cronograma de pagos, adjuntar documentos y agendar una cita presencial.
Incluye un panel privado para que el equipo de asesores de negocio gestione,
de forma colaborativa, el seguimiento de cada solicitud.

Proyecto académico del curso **Herramientas de Desarrollo**. Reescrito con
**Spring Boot** a partir de la versión inicial en HTML/CSS/JS estático,
y ampliado en esta versión para cubrir los procesos, herramientas de
colaboración y gestión de actividades exigidos en el segundo avance.

## Stack técnico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.3 (Java 21), Spring MVC, Spring Data JPA, Spring Security |
| Vistas | Thymeleaf + CSS propio (sin frameworks de UI) |
| Base de datos | H2 (archivo local, incluida — no requiere instalación) |
| Reportes | PDF generado en el servidor con OpenPDF |
| Gráficos | Chart.js (dashboard de analítica) |
| Empaquetado | Maven, Docker |
| CI | GitHub Actions (compila y prueba en cada push) |
| Despliegue | Render (gratuito, mediante Docker) |

## Regla de negocio central

```
ratio = montoSolicitado / ingresoMensual

ratio <= 4  → PRECALIFICADO       (agenda cita en agencia)
ratio  > 4  → REQUIERE_REVISION   (un asesor contacta con alternativas)
```

Implementada en `SolicitudCredito.evaluar()` y cubierta por pruebas unitarias
en `src/test/java/.../SolicitudCreditoTest.java`.

## Funcionalidades (mapa a la rúbrica del proyecto)

**II.6 — Descripción de los procesos principales / II.7 — Secuencia de los
procesos principales**
Documentados en la página pública `/acerca-del-proyecto`: explica el proceso
de pre-evaluación, asignación automática, agendamiento, seguimiento
colaborativo y evaluación formal, con una línea de tiempo del flujo completo.

**II.8 — Herramientas de colaboración de equipo**
- Varios asesores con cuentas propias (Spring Security + BCrypt).
- Asignación automática de cada solicitud al asesor con menor carga activa
  (`PreEvaluacionService.asesorConMenosCargaActual()`).
- Reasignación manual entre asesores desde el detalle de la solicitud.
- Comentarios internos con autor y fecha, visibles para todo el equipo.
- Historial/línea de tiempo de cada cambio de estado, con autor.

**II.9 — Herramientas de gestión de actividades**
- Tablero Kanban (`/asesor/kanban`) con arrastrar y soltar entre columnas de
  estado (Pendiente, Cita agendada, En revisión, Contactado, Cerrado),
  actualizado por AJAX sin recargar la página.
- Panel de solicitudes con búsqueda y filtros por nombre/DNI, estado,
  resultado y asesor asignado.
- Dashboard de analítica (`/asesor/analitica`) con Chart.js: tendencia de
  solicitudes de los últimos 14 días, distribución por destino de crédito,
  tasa de pre-calificación y monto promedio solicitado.

**II.10 — Avance de interfaces y código del proyecto**
Todo el código fuente de esta versión (backend, vistas y configuración de
despliegue) está en este repositorio, organizado según la estructura de
Spring Boot descrita abajo.

### Funcionalidades adicionales para el cliente

- **Simulador de cuotas** (`/simulador`, página pública): calcula la cuota
  mensual con el sistema de cuota fija (amortización francesa) y muestra el
  cronograma completo mes a mes, antes de que el cliente decida solicitar.
- **Carga de documentos**: el cliente puede adjuntar su DNI y su sustento de
  ingresos al llenar el formulario; el asesor los descarga desde el detalle
  de la solicitud.
- **Comprobante en PDF**: tanto el cliente como el asesor pueden descargar un
  PDF con el resultado de la pre-evaluación, generado en el servidor.

## Estructura del proyecto

```
src/main/java/com/cajapiura/preevaluacion/
├── PreevaluacionApplication.java   # clase principal
├── model/        # SolicitudCredito, Usuario, ComentarioSolicitud,
│                 # HistorialEstado, DocumentoAdjunto y enums
├── repository/   # interfaces Spring Data JPA
├── service/      # PreEvaluacionService, SimuladorCreditoService,
│                 # FileStorageService, AnaliticaService, PdfService,
│                 # UsuarioDetailsService
├── controller/   # landing, pre-evaluación, resultado, cita, simulador,
│                 # acerca-del-proyecto, panel del asesor, analítica, documentos
└── config/       # seguridad (Spring Security + BCrypt), locale, datos de ejemplo

src/main/resources/
├── templates/                 # vistas Thymeleaf (sitio público + panel del asesor)
│   ├── fragments/             # head, nav, footer, sidebar del panel
│   └── asesor/                # tablero, detalle, kanban, analítica, login
├── static/css/main.css        # estilos
├── static/js/                 # kanban.js (drag & drop), analitica.js (Chart.js)
└── application.properties
```

## Ejecutar en local

Requiere JDK 21 y Maven.

```bash
mvn spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

- Sitio público: `http://localhost:8080/`
- Formulario de pre-evaluación: `http://localhost:8080/pre-evaluacion`
- Simulador de cuotas: `http://localhost:8080/simulador`
- Acerca del proyecto: `http://localhost:8080/acerca-del-proyecto`
- Panel del asesor: `http://localhost:8080/asesor`

### Cuentas de demostración (panel del asesor)

| Usuario | Contraseña | Rol |
|---|---|---|
| `rpena` | `asesor123` | Asesor de negocios |
| `jtimana` | `asesor123` | Asesor de negocios |
| `admin` | `cajapiura2026` | Administrador (incluye acceso a `/h2-console`) |

## Ejecutar con Docker

```bash
docker build -t caja-piura-preevaluacion .
docker run -p 8080:8080 caja-piura-preevaluacion
```

## Despliegue en línea (Render, gratuito)

El proyecto incluye `Dockerfile` y `render.yaml`, listos para desplegarse
sin configuración adicional:

1. Sube el repositorio a GitHub.
2. Entra a [render.com](https://render.com) e inicia sesión con GitHub.
3. **New +** → **Blueprint** → selecciona este repositorio.
4. Render detecta `render.yaml` y crea el servicio web automáticamente
   (usa el `Dockerfile` incluido). El primer build tarda unos minutos.
5. Al terminar, Render entrega una URL pública tipo
   `https://caja-piura-preevaluacion.onrender.com`.

> **Nota sobre persistencia:** el plan gratuito de Render usa disco efímero,
> así que la base de datos H2 y los documentos subidos se reinician en cada
> nuevo despliegue. Es aceptable para una demo académica; `render.yaml` deja
> comentada la opción de usar una base de datos Postgres administrada
> (también gratuita en Render) si se necesita persistencia real.

### Alternativas equivalentes

Cualquier plataforma que soporte contenedores Docker sirve igual: **Railway**,
**Fly.io** o **Koyeb** siguen el mismo flujo. GitHub Pages **no** es
compatible, porque solo sirve archivos estáticos y esta aplicación necesita
un servidor Java en ejecución.

## Flujo de trabajo en Git (para la exposición)

Esta versión se integra como una nueva rama de funcionalidad sobre el
historial ya existente:

```bash
git checkout develop
git checkout -b feature/funcionalidades-avanzadas

# ... copiar/reemplazar los archivos de esta versión ...
git add .
git commit -m "Agrega simulador, colaboración de equipo, Kanban, analítica y PDF"

git checkout develop
git merge feature/funcionalidades-avanzadas
git checkout main
git merge develop
git tag v2.0
git push -u origin main --tags
git push origin develop
```

## Panel del asesor

Ruta protegida con Spring Security (`/asesor/**`). Incluye:

- Indicadores generales y filtros (búsqueda, estado, resultado, asesor).
- Tablero Kanban con arrastrar y soltar.
- Detalle de cada solicitud: cambio de estado, reasignación de asesor,
  comentarios del equipo, documentos adjuntos, línea de tiempo y comprobante
  en PDF.
- Dashboard de analítica con gráficos.

## Pruebas

```bash
mvn test
```

- `SolicitudCreditoTest`: regla de negocio de pre-calificación.
- `SimuladorCreditoServiceTest`: cálculo de cuotas y cronograma de
  amortización.

## Licencia / uso

Proyecto con fines académicos para el curso de Herramientas de Desarrollo.
probando CD