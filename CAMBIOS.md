# Correcciones del seguimiento de incidentes

| ID | Severidad | Capa | Qué se cambió |
|---|---|---|---|
| INC-01 | Crítica | Seguridad y config. | Se quitaron las contraseñas del README, de la pantalla de login y del código; ahora salen de `SEED_ADMIN_PASSWORD` / `SEED_ASESOR_PASSWORD` (o se generan al azar). Consola H2 desactivada por defecto (`H2_CONSOLE`). `render.yaml` genera las variables. |
| INC-02 | Alta | Controlador | Resultado, comprobante PDF y cita se abren con un `codigoPublico` (UUID) en vez del ID secuencial. |
| INC-03 | Alta | Datos | **No corregido**: requiere crear una base Postgres en Render y cambiar el driver en `pom.xml`. |
| INC-04 | Media | Servicio | `SolicitudCredito.evaluar()` decide con el ratio exacto; 4.004 ya es REQUIERE_REVISION. |
| INC-05 | Media | Controlador | Cita con fecha inválida o pasada ya no da HTTP 500: vuelve al formulario con aviso. |
| INC-06 | Media | Servicio | Reasignar valida asesor activo con rol ASESOR y deja registro en el historial. |
| INC-07 | Media | Servicio | `countByAsesorAsignadoAndEstadoNot` reemplaza `findAll()` dentro del comparador. |
| INC-08 | Media | Seguridad y config. | `ValidadorArchivos`: solo PDF/JPG/PNG de hasta 5 MB, nombre saneado, cabecera de descarga con `ContentDisposition`. |
| INC-09 | Media | Infra y proceso | Pruebas nuevas en `SolicitudCreditoTest`, `SimuladorCreditoServiceTest` y `ValidadorArchivosTest`. |
| INC-10 | Baja | Servicio | `evaluar()` lanza `IllegalStateException` si faltan datos. |
| INC-11 | Baja | Servicio | `simular()` valida monto, plazo y tasa. |
| INC-12 | Baja | Datos | Comentarios vacíos o de más de 1000 caracteres no se guardan. (La unicidad del DNI no se cambió.) |

Nota: al agregar `codigoPublico`, Hibernate (`ddl-auto=update`) crea la columna; con H2 efímero en Render no hay filas antiguas.

## Rediseno visual (17/11/2026)
- Paleta verde (#051F20, #0B2B26, #163832, #235347, #8EB69B, #DAF1DE) con dorado de acento; variables en `static/css/main.css` (:root). Cambiar ahi cambia todo el sitio.
- Animaciones: aparicion al hacer scroll, contadores, hero con zoom lento, tarjetas flotantes, cinta de valores. Respetan `prefers-reduced-motion`. Logica en `static/js/ui.js`.
- Imagenes en la nube (Unsplash) en `templates/landing.html`; si una URL falla, se muestra un degradado verde.
- Fuentes: Manrope (titulos) e Inter (texto). Sin emojis: iconos SVG en linea.

## Correccion del Kanban (17/11/2026)
- Error 500 en /asesor/kanban: la plantilla usaba `solicitudes.?[estado == e]` y SpEL no veia la variable `e` dentro del filtro.
  Ahora el controlador calcula `conteos` por estado y la plantilla usa `conteos.get(e)`. El error ya existia desde el Avance 2.

## Consola de administracion y nuevo logotipo (17/11/2026)
Rutas `/admin/**` (solo ROLE_ADMIN). Al iniciar sesion, el admin entra a `/admin` y el asesor a `/asesor`.
- **Resumen y KPIs** (`/admin`): totales, tasa de pre-calificacion, pendientes, estancadas (>3 dias), sin asesor, y KPIs por asesor (asignadas, activas, cerradas, tasa de cierre, monto gestionado). Logica en `KpiCalculator` (pura, con pruebas).
- **Supervision global** (`/admin/supervision`): todas las solicitudes con filtros, reasignacion masiva y exportacion CSV.
- **Usuarios y roles** (`/admin/usuarios`): crear, activar/desactivar, cambiar rol, restablecer contrasena (temporal, se muestra una sola vez). Protecciones: no desactivarse a si mismo ni dejar el sistema sin administrador activo.
- **Parametros** (`/admin/parametros`): ratio limite de la pre-evaluacion y limites del simulador, validados y guardados en la tabla `parametros_sistema`. Aplican a solicitudes nuevas.
- **Auditoria** (`/admin/auditoria`): tabla `auditoria` con inicios de sesion (exitosos y fallidos), solicitudes creadas, cambios de estado, reasignaciones, cambios de usuarios/parametros, exportaciones y consultas a la BD.
- **Base de datos** (`/admin/base-datos`): explorador de SOLO LECTURA (tablas, estructura, filas paginadas, CSV). El nombre de tabla se valida contra el catalogo; las columnas de contrasenas se ocultan; cada consulta queda auditada. La consola SQL de H2 sigue apagada salvo `H2_CONSOLE=true`.
- **Logotipo**: marca propia (C con moneda) + nombre + descriptor, en `templates/fragments/logo.html` y `static/img/logo*.svg` / `favicon.svg`. Para usar el logotipo oficial, reemplazar esos archivos.
- Cambios de codigo existentes: `SolicitudCredito.evaluar(BigDecimal limite)` (el `evaluar()` sin argumentos sigue usando 4), `PreEvaluacionService` (auditoria y limite configurable), `SimuladorController` (limites configurables), `SecurityConfig` (rutas /admin, redireccion por rol, auditoria de login), `DataSeeder` (crea parametros por defecto).
- Pruebas nuevas: `AdminModuloTest` (14 pruebas sin base de datos).
- Limitaciones conocidas: la contrasena temporal no obliga a cambiarla en el primer ingreso; la auditoria muestra los ultimos 500 eventos; con H2 efimero en Render, usuarios creados, parametros y auditoria se pierden al redesplegar.

## Portal del cliente y Docker Compose (18/11/2026)
- **Dos portales separados.** Clientes: `/cliente/registro`, `/cliente/login`, `/cliente` (panel). Colaboradores: `/asesor/login` (alias `/colaboradores`), con apariencia distinta y rotulado "Uso interno". Dos cadenas de Spring Security con administradores de autenticación independientes (`SecurityConfig`): las cuentas de clientes (`Cliente`, tabla `clientes`) y las del personal (`Usuario`) no se mezclan.
- **Registro de clientes** con correo y contraseña. Validaciones en `ClienteValidador`: correo válido, nombre, contraseña de 8 a 72 caracteres con mayúscula, minúscula y número, sin contraseñas comunes ni derivadas del correo, confirmación. Contraseña cifrada con BCrypt.
- **Consentimiento** de tratamiento de datos (Ley N.° 29733) y términos: casillas obligatorias; se guarda la fecha y la versión aceptada. Página `/privacidad` (texto referencial, a revisar por el área legal).
- **Bloqueo por intentos fallidos** (5 intentos en 15 minutos por correo), en memoria (`IntentosLogin`). Inicios de sesión y registros de clientes quedan en la auditoría.
- **Simulador y pre-evaluación ahora exigen cuenta de cliente.** La pre-evaluación precarga nombre y correo y vincula la solicitud al cliente (`SolicitudCredito.cliente`). El panel del cliente lista sus solicitudes. Se restringen por formulario los campos que calcula el sistema (`@InitBinder`).
- **Docker Compose**: `docker-compose.yml`, `.env.example` y `.dockerignore`; volumen `caja-piura-data` para la base y los documentos.
- Pruebas nuevas: `ClientePortalTest` (7 pruebas).
- Limitaciones conocidas: no hay verificación del correo por enlace (requiere un servicio de envío de correo); no hay "olvidé mi contraseña"; no hay eliminación de cuenta desde el portal (derecho de cancelación se atiende por solicitud); con H2 efímero en Render, **las cuentas de clientes se pierden en cada redespliegue** (la solución es Postgres, INC-03); el bloqueo por intentos se reinicia al reiniciar la app.
