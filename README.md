# Post-contenido — Unidad 6: Antipatrones de Diseño

**Estudiante:** Jesús Pérez  
**Asignatura:** Patrones de Diseño de Software — Sexto Semestre  
**Institución:** Universidad de Santander (UDES)  
**Repositorio:** [https://github.com/Yizuz13/perez-post1-u6.git](https://github.com/Yizuz13/perez-post1-u6.git)

---

## 1. Descripción General del Proyecto

Este proyecto aborda el diagnóstico, análisis crítico y refactorización orientada a patrones de diseño de un sistema de comercio electrónico desarrollado en Spring Boot (`pedidos-service`). La actividad consta de dos fases secuenciales:

1. **Parte 1:** Diagnóstico de antipatrones combinados en una clase monolítica (`GestorPedidos`) que concentraba la validación de stock, verificación de clientes, cálculo financiero, acceso a base de datos JDBC y notificación por correo.
2. **Parte 2:** Detección y corrección de un segundo antipatrón surgido en un ciclo de crecimiento posterior, donde se reutilizó erróneamente el patrón de la Parte 1 para aplicar tres nuevas campañas promocionales, desvirtuando el diseño original.

---

## 2. Decisiones de Diseño y Diagnóstico

### Parte 1 — GestorPedidos: Antipatrones God Object y Spaghetti Code

#### Diagnóstico con Evidencia del Código Base
Al analizar el método original `GestorPedidos.procesarPedido(PedidoRequest request)` (líneas 20 a 115) dentro de una clase de 340 líneas, se evidencian dos antipatrones fuertemente acoplados:

* **God Object (Objeto Todopoderoso):** La clase asume 6 responsabilidades distintas que violan de forma directa el Principio de Responsabilidad Única (SRP):
  1. *Validación de stock:* Consultas directas a la tabla `inventario` (líneas 27-35).
  2. *Validación de estado crediticio y cliente:* Consultas directas a las tablas `clientes` y `facturas` (líneas 37-54).
  3. *Cálculo de subtotal de producto:* Iteración y consultas SQL embebidas en `productos` (líneas 56-62).
  4. *Cálculo de descuentos:* Reglas de negocio anidadas (líneas 65-80).
  5. *Persistencia relacional:* Inserciones SQL directas vía `jdbcTemplate.update()` sin abstracción transaccional ni repositorio (líneas 84-97).
  6. *Notificación al cliente:* Composición y formateo de texto del correo electrónico (líneas 99-111).
* **Spaghetti Code:** El flujo del método mezcla múltiples niveles de abstracción (cadenas SQL en bruto, lógica de negocio temporal, operaciones aritméticas e interpolación de strings). Además, el cálculo de descuentos presenta hasta **3 niveles de anidamiento condicional** (`if (tipoCliente.equals("VIP")) -> if (subtotal > 1_000_000) -> else if (...)`), haciendo que la adición de un nuevo tipo de cliente requiera reescribir y recompilar el método entero, violando el Principio Abierto/Cerrado (OCP).

#### Patrones Aplicados
* **Chain of Responsibility (Cadena de Responsabilidad):** Para el flujo de validaciones (`ValidadorStock` y `ValidadorCliente`). Se implementó un objeto `ContextoPedido` mutable que recorre la cadena.
* **Strategy:** Para el cálculo de descuentos según la categoría del cliente (`DescuentoVip`, `DescuentoFrecuente`, `DescuentoEstandar`), administrados por `SelectorEstrategiaDescuento`.
* **Separación de Responsabilidades:** Se extrajo la persistencia hacia `PedidoRepository` y el formateo/notificación hacia `NotificacionPedidoService`, reduciendo `GestorPedidos` a un orquestador delgado de 25 líneas.

#### Alternativa Descartada y Justificación
* **Alternativa evaluada:** Encapsular las validaciones en una lista de predicados funcionales `List<Predicate<ContextoPedido>>` ejecutados en un bucle secuencial dentro de un método `validarTodo()`.
* **Razón del descarte:** Dicha estructura no soporta de forma natural el **corte anticipado (short-circuit)** dependiente del estado del eslabón anterior. Si `ValidadorStock` detecta inventario insuficiente, no tiene sentido invocar la base de datos para consultar el saldo deudor del cliente. La cadena garantiza la terminación inmediata del flujo en cuanto un eslabón marca `contexto.rechazar(...)`.

---

### Parte 2 — Crecimiento del Sistema: Antipatrón Golden Hammer

#### Diagnóstico con Evidencia del Código
Tras solicitar tres campañas promocionales (`BLACK_FRIDAY`, `CORPORATIVO` y `VOLUMEN`), se agregaron las clases `PromocionBlackFriday`, `PromocionCorporativo` y `PromocionVolumen` extendiendo de `ValidadorPedido`. 

* **Antipatrón identificado: Golden Hammer (Martillo de Oro):**
  * **Violación de contrato de la abstracción:** `ValidadorPedido` fue diseñado para decidir si el pedido continúa o se rechaza. Los nuevos eslabones **nunca rechazan un pedido**; su única función es mutar el estado (`contexto.aplicarDescuentoCampana(...)`).
  * **Ausencia de dependencia de orden:** Mientras que la validación de stock debe preceder lógicamente a la consulta de morosidad, las tres campañas no tienen ninguna relación de precedencia: ejecutar `PromocionVolumen` antes o después de `PromocionBlackFriday` produce el mismo resultado funcional.
  * **Uso forzado por familiaridad:** Se adoptó la cadena simplemente porque "ya existía y funcionó en la Parte 1", forzando una estructura de procesamiento secuencial para un problema que en realidad corresponde a la selección y combinación de políticas de precios.

#### Patrón Aplicado y Refactorización
* Se eliminaron por completo las clases `Promocion*` del paquete `validacion`.
* Se implementaron como estrategias (`DescuentoBlackFriday`, `DescuentoCorporativo`, `DescuentoVolumen`) implementando `EstrategiaDescuento`.
* Se introdujo el componente `CalculadorDescuentoFinal`, el cual evalúa de forma desacoplada la estrategia de tipo de cliente y las campañas vigentes, determinando el descuento máximo aplicable sin efectos secundarios ni variables compartidas.
* **Prevención de Lava Flow:** En lugar de dejar los eslabones anteriores comentados "por si acaso", se eliminaron del árbol de trabajo, delegando la trazabilidad histórica exclusivamente al control de versiones (Git).

#### Alternativa Descartada y Justificación
* **Alternativa evaluada:** Mantener los handlers dentro de `ValidadorPedido` introduciendo un flag condicional que distinga entre "eslabones de validación" y "eslabones de modificación de precios".
* **Razón del descarte:** Perpetuaba el antipatrón acoplando dos conceptos dispares bajo una misma jerarquía de herencia, forzando parámetros globales y degradando la cohesión arquitectural del sistema.

---

## 3. Comparativa de Comportamiento (Antes vs. Después)

| Caso de Prueba | Entrada | Salida Original (GestorPedidos Monolítico / Golden Hammer) | Salida Refactorizada (CoR + Strategy + CalculadorFinal) | Estado |
| :--- | :--- | :--- | :--- | :--- |
| **1. Sin stock** | Producto 99, Cant: 50 | Rechazado: `"Stock insuficiente: producto 99"` | Rechazado: `"Stock insuficiente: producto 99"` | Idéntico |
| **2. Cliente no existe** | Cliente 999 | Rechazado: `"Cliente no registrado"` | Rechazado: `"Cliente no registrado"` | Idéntico |
| **3. Cliente moroso (<20:00)** | Cliente 3 (Moroso, deuda $150.000) | Rechazado: `"Cliente con deuda pendiente: $150000.0"` | Rechazado: `"Cliente con deuda pendiente: $150000.0"` | Idéntico |
| **4. Descuento VIP** | Cliente 1 (VIP), Total > $1.000.000 | Confirmado, Descuento 15% aplicado | Confirmado, Descuento 15% aplicado | Idéntico |
| **5. Campaña Black Friday** | Promo activa = true | Confirmado, Descuento 25% aplicado | Confirmado, Descuento 25% aplicado | Idéntico |
| **6. Mayor descuento gana** | Cliente VIP (15%) + Black Friday (25%) | Confirmado, Descuento 25% aplicado | Confirmado, Descuento 25% aplicado | Idéntico |

---

## 4. Instrucciones de Compilación y Ejecución

### Requisitos
* Java JDK 17 o superior
* Apache Maven 3.8+

### Comandos de Ejecución
Desde la carpeta raíz del proyecto (`pedidos-service/`):

1. **Compilar el proyecto:**
   ```bash
   mvn clean compile