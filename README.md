<div align="center">
  <h1>🐮 miGanadito_Control</h1>
  <p><strong>Gestión ganadera inteligente y simplificada • Demo v1</strong></p>
  <p>Una herramienta diseñada para facilitar el control de ganado y el cumplimiento de las normativas de SINIIGA.</p>
  <hr />
</div>

## ✨ Características de la Demo (v1)

Esta primera versión demo está enfocada en asentar las bases de la gestión digital del rancho:

*   **📋 Registro de Ganado:** Control individual de tus animales con datos esenciales.
*   **📱 Interfaz Intuitiva:** Diseñada para que sea fácil de usar incluso bajo el sol del campo.


---

## 🛠️ Cómo ejecutar la Demo en tu PC

Para correr este proyecto de Android de manera local, sigue estos pasos:

### Prerrequisitos

*   **Android Studio** (Koala o más reciente) instalado.
*   Una llave de API de **Google AI Studio** (Gemini API Key).

### Instrucciones de Instalación

1. **Clona o descarga** este repositorio en tu computadora.
2. Abre **Android Studio**, selecciona **Open** y elige la carpeta de este proyecto.
3. Espera a que Android Studio descargue las dependencias y sincronice el proyecto con Gradle.

### Configuración del Entorno

Para que la inteligencia artificial funcione, necesitas configurar tu API Key de Gemini:

1. En la raíz del proyecto, crea un archivo llamado `.env` (puedes guiarte de `.env.example`).
2. Agrega tu clave de la siguiente manera:
   ```env
   GEMINI_API_KEY=tu_clave_secreta_aqui
