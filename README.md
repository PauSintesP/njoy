# Aplicación Android - Gestión de Eventos y Entradas

## Arquitectura Cliente/Servidor

La aplicación sigue una arquitectura cliente-servidor:

- **Cliente (Android)**: Aplicación nativa en Kotlin. Interfaz para explorar eventos, registrarse, iniciar sesión, comprar entradas y escanear tickets.
- **Servidor (API REST)**: Gestiona la lógica de negocio y persistencia. Permite operaciones CRUD sobre eventos, usuarios, tickets y pagos.
- **Comunicación**: Mediante HTTP y JSON usando Retrofit.

---

## Tecnologías Utilizadas

### Cliente Android (Frontend)

- **Kotlin** – Lenguaje principal.
- **Android SDK** – Desarrollo de UI nativa.
- **Retrofit** – Comunicación con el servidor.
- **Glide** – Carga de imágenes.
- **CameraX** – Escaneo de códigos QR.
- **Coroutines** – Operaciones asíncronas.

### Servidor (Backend)

- **API REST** – Gestión de datos y lógica.
- **Base de datos** – Almacenamiento de usuarios, eventos, tickets y pagos.

---

## Organización del Código

- `Activities`: Pantallas principales (LoginActivity, MainActivity, EventActivity...).
- `Adapters`: Controladores de listas (EventAdapter, TicketAdapter...).
- `Data Classes`: Modelos como `User`, `Event`, `Ticket`, etc.
- `Servicios`: `ApiClient`, `ApiService`, para acceso remoto.
- `Utilidades`: `SessionManager`, validadores, extensiones, etc.

---

## Funcionalidades Generales

### Registro e Inicio de Sesión

- **Registro**: Validación de campos y creación de cuenta.
- **Login**: Autenticación y persistencia de sesión mediante `SessionManager`.

### Visualización de Eventos

- **Listado de Eventos**: Consulta a la API, filtrado por tipo, visualización en tarjetas.
- **Detalle de Evento**: Muestra toda la información y permite seleccionar entradas.

### Compra de Entradas

- **Selección**: Elección de número de entradas.
- **Pago**: Selección de método (VISA, MasterCard, PayPal).
- **Proceso**:
  - Validación de plazas.
  - Creación de tickets.
  - Registro de pago.
  - Confirmación de compra.

### Gestión de Tickets

- **Historial de Pagos**: Lista los pagos realizados.
- **Entradas Adquiridas**: Visualización de entradas y su código QR.

---

## Funcionalidades del Administrador

Al iniciar sesión como administrador (`admin@example.com`, contraseña `12345678`), se accede a:

### Gestión de Eventos

- Crear, editar y eliminar eventos.
- Visualizar estadísticas de ventas y plazas restantes.

### Escaneo de Tickets (Validación QR)

- **Interfaz de escaneo con CameraX**.
- **Verificación**:
  - Existencia del ticket.
  - Correspondencia con el evento actual.
  - Estado de uso (`activado`).
- **Feedback visual**:
  - Verde = ticket válido.
  - Rojo = ticket inválido o ya usado.
- **Reinicio automático del escaneo tras 3 segundos**.
- **Selección de evento** mediante un `RecyclerView`.

### Monitoreo y Estadísticas

- Seguimiento de ventas por evento.
- Control en tiempo real de plazas disponibles.
- Resumen de pagos y actividad de entradas escaneadas.

---

## Recursos y Dependencias

- **Retrofit**: HTTP Client para APIs REST.
- **Glide**: Carga eficiente de imágenes.
- **CameraX**: Integración con la cámara.
- **Coroutines**: Código asincrónico.
- **Material Design**: UI moderna.

---

## Herramientas de Desarrollo

- **Android Studio**
- **Gradle**
- **Git**

---

## Autor

Proyecto desarrollado en el marco del ciclo formativo DAM (Desarrollo de Aplicaciones Multiplataforma).
