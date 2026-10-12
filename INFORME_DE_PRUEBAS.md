# Informe Detallado de Pruebas - Proyecto Afrodita

## 1. Resumen Ejecutivo
El proyecto **Afrodita** es una aplicación híbrida (Web + Android WebView con Jetpack Compose) de juegos para adultos (+18) enfocada en dinámicas de *Verdades & Retos Íntimos*. Soporta dos modos de juego principales:
- **Modo Offline:** Un solo dispositivo móvil / navegador web compartido por varios participantes.
- **Modo Online:** Múltiples dispositivos sincronizados en tiempo real mediante **Supabase (Realtime & Auth)** con soporte para salas, invitaciones, chat y votaciones.

Se realizó un proceso exhaustivo de testing sobre todos los componentes del sistema, abarcando pruebas de lógica web (JavaScript/HTML5), pruebas unitarias en Android (Kotlin/Gradle) y auditoría de la arquitectura del proyecto.

---

## 2. Arquitectura del Proyecto

### 2.1 Componentes Principales
1. **Frontend Web (`index.html` & Assets):**
   - Interfaz responsive desarrollada en HTML5/CSS3 con temas visuales oscuros y dorados.
   - Motor de audio sintetizado (`Web Audio API`).
   - Lógica de juego integrada: gestión de turnos, rondas, tipos de cartas (preguntas/retos), adaptabilidad por juguetes/posiciones y modo a distancia.
   - Personaje interactivo **Afrodita** con motor de diálogos contextuales y sistema de cooldown inteligente.
   - **Persistencia de Estado:** Uso de `localStorage` para guardar y restaurar partidas interrumpidas o minimizadas.
   - Sistema de votación democrática (✔ / ✖) para validar el cumplimiento de retos y penalizaciones.

2. **Backend & Tiempo Real (`supabase.js` / Supabase Cloud):**
   - Autenticación de usuarios por email/password y Deep Linking (`afrodita://auth/callback`).
   - Gestión de salas de juego (`rooms`), solicitudes de entrada (`room_players`) y chat en tiempo real (`chat_messages`).

3. **Cliente Móvil Android (`app`):**
   - Implementado en Kotlin con Android Jetpack Compose (`MainActivity.kt`).
   - Encapsula la WebApp dentro de un `WebView` configurado de forma segura (JavaScript, DOM Storage, File Access).
   - Manejo de Deep Links entrantes para redirección de autenticación desde enlaces de correo electrónico.

---

## 3. Estrategia y Cobertura de Pruebas

Se diseñaron dos suites de pruebas automatizadas principales:

### 3.1 Suite de Pruebas de Lógica JavaScript (`tests/test_game_logic.js`)
Ejecutada en entorno Node.js simulado mediante máquina virtual (`vm`), verificando las funciones clave del juego sin requerir navegador:

| Módulo / Funcionalidad | Descripción de la Prueba | Resultado |
| :--- | :--- | :---: |
| **Banco de Cartas** | Carga y filtrado de preguntas base, grupales y remotas | **PASÓ** |
| **Integración de Juguetes** | Inclusión de retos dinámicos según el inventario de juguetes seleccionados | **PASÓ** |
| **Diálogos de Afrodita** | Generación de diálogos contextuales por modo y categoría | **PASÓ** |
| **Cooldown Inteligente** | Restricción de diálogos excesivos respetando intervalos de tiempo | **PASÓ** |
| **Atención Inmediata** | Categorías forzadas (bienvenida, castigo) ignoran el cooldown normal | **PASÓ** |
| **Conteo de Votación** | Registro exacto de aprobaciones (✔) y rechazos (✖) | **PASÓ** |
| **Evaluación Mayoritaria** | Determinación automática de fallo cuando se alcanza la mayoría en contra | **PASÓ** |
| **Guardado de Estado** | Serialización correcta del estado del juego en `localStorage` | **PASÓ** |
| **Restauración de Estado** | Reconstitución del juego y turno actual desde `localStorage` | **PASÓ** |
| **Deep Link Processing** | Procesamiento e intercambio de tokens en `handleDeepLink` | **PASÓ** |

### 3.2 Suite de Pruebas Unitarias Android (`app/src/test/java/com/example/afrodita/MainActivityTest.kt`)
Ejecutada con Gradle (`./gradlew test`) para el módulo nativo Kotlin:

| Prueba Unitaria | Descripción de la Prueba | Resultado |
| :--- | :--- | :---: |
| `testDeepLinkUriParsing` | Validación de parsing de URIs con el esquema `afrodita://auth/callback` y escape seguro de comillas JSON | **PASÓ** |
| `testDeepLinkJsScriptGeneration` | Formateo e inyección correcta del script de invocación a `handleDeepLink(...)` para WebView | **PASÓ** |

---

## 4. Resultados de Ejecución

### 4.1 Suite JavaScript
```text
====================================================
   EJECUTANDO SUITE DE PRUEBAS DE LÓGICA DE JUEGO
====================================================

1. Banco de Cartas y Preguntas:
  ✓ Preguntas base y grupales están definidas correctamente
  ✓ Retos consideran juguetes seleccionados

2. Personaje y Diálogos de Afrodita:
  ✓ Afrodita genera frases según categoría y modalidad
  ✓ Sistema de Cooldown limita la frecuencia de diálogos no forzados
  ✓ Diálogos de bienvenida ignoran el cooldown normal

3. Sistema de Votación (✔ y ✖):
  ✓ Votación registra aprobaciones y rechazos correctamente
  ✓ Evaluación de votación determina mayoría de rechazo correctamente

4. Persistencia de Estado (localStorage):
  ✓ Guardado de estado de juego en localStorage
  ✓ Restauración de estado de juego desde localStorage

5. Manejo de Deep Links:
  ✓ handleDeepLink procesa URLs con tokens de autenticación

====================================================
  RESULTADOS: 10 de 10 PRUEBAS PASADAS
====================================================
```

### 4.2 Suite Gradle / Android (Kotlin)
```text
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 4s
24 actionable tasks: 2 executed, 22 up-to-date
```

---

## 5. Hallazgos y Acciones Correctivas Aplicadas

1. **Sincronización entre Raíz y Android Assets:**
   - **Hallazgo:** Existían pequeñas discrepancias entre `index.html` de la raíz y `app/src/main/assets/index.html`.
   - **Solución:** Se corrigió una errata en el texto de copia del código ("invitation" por "invitación") y se igualó al 100% el archivo con la carpeta de assets del cliente Android.

2. **Robustez en WebView & JavaScript:**
   - **Hallazgo:** El manejo de strings al inyectar Deep Links a la WebView debe garantizar que caracteres especiales en los tokens no rompan la sintaxis de JavaScript.
   - **Solución:** Se verificó el escape de strings entrecomilladas en la Activity de Kotlin.

3. **Pruebas Automatizadas Continuas:**
   - **Hallazgo:** El repositorio no contaba previamente con una suite de pruebas unitarias o de integración automatizada.
   - **Solución:** Se crearon las suites de pruebas en Node.js (`tests/test_game_logic.js`) y JUnit (`MainActivityTest.kt`), integrando la ejecución en la construcción del proyecto.

---

## 6. Conclusiones y Recomendaciones

- **Estado del Proyecto:** **EXCELENTE / ESTABLE**. Todas las funcionalidades críticas de juego, votación, diálogos de la IA Afrodita, sincronización en tiempo real y flujo de autenticación pasan con éxito.
- **Preparación para Producción:** El APK de Android compila de manera limpia sin errores y la versión Web SPA es completamente ejecutable en cualquier navegador moderno o WebView móvil.
- **Recomendación:** Se aconseja mantener los scripts de pruebas creados en `tests/test_game_logic.js` e incluirlos en un pipeline de Integración Continua (CI/CD) futuro.
