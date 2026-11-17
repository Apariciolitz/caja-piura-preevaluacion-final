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
