<div align="center">

<img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Claude-Powered-6B46C1?style=for-the-badge&logo=anthropic&logoColor=white"/>
<img src="https://img.shields.io/badge/ReAct-Loop-00BFFF?style=for-the-badge&logo=robot&logoColor=white"/>
<img src="https://img.shields.io/badge/Windows%20%7C%20Linux%20%7C%20macOS-Compatible-FCC624?style=for-the-badge&logo=linux&logoColor=black"/>
<img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Version-0.2.0-blue?style=for-the-badge"/>

<br/><br/>

```
   █████╗ ██╗      ██████╗ ██████╗ ██████╗ ██╗███╗   ██╗ ██████╗
  ██╔══██╗██║     ██╔════╝██╔═══██╗██╔══██╗██║████╗  ██║██╔════╝
  ███████║██║     ██║     ██║   ██║██║  ██║██║██╔██╗ ██║██║  ███╗
  ██╔══██║██║     ██║     ██║   ██║██║  ██║██║██║╚██╗██║██║   ██║
  ██║  ██║███████╗╚██████╗╚██████╔╝██████╔╝██║██║ ╚████║╚██████╔╝
  ╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚═════╝ ╚═════╝ ╚═╝╚═╝  ╚═══╝ ╚═════╝
     █████╗  ██████╗ ███████╗███╗   ██╗████████╗
    ██╔══██╗██╔════╝ ██╔════╝████╗  ██║╚══██╔══╝
    ███████║██║  ███╗█████╗  ██╔██╗ ██║   ██║
    ██╔══██║██║   ██║██╔══╝  ██║╚██╗██║   ██║
    ██║  ██║╚██████╔╝███████╗██║ ╚████║   ██║
    ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝   ╚═╝
```

###   Agente de IA autónomo para coding — construido en Java 21
#### Analiza proyectos · Escribe y modifica código · Ejecuta comandos · Gestiona Git · Razona paso a paso

</div>

---

## Vista previa

```
╔══════════════════════════════════════════════════════════╗
║           AI Coding Agent  v0.1.0                        ║
╚══════════════════════════════════════════════════════════╝

> agent "analiza el proyecto y corrige los errores de compilación"

 Agente iniciado. Tarea: analiza el proyecto y corrige los errores...
────────────────────────────────────────────────────────────────────────

   Pensando...
   Tool: list_dir
   Razón: Need to understand the project structure first
   ✓ src/main/java/com/app/ — 12 archivos encontrados

   Pensando...
   Tool: analyze_code
   Razón: Scanning all Java files for syntax/semantic issues
   ✓ 3 clases con complejidad ciclomática alta detectadas

   Pensando...
   Tool: read_file
   Razón: Reading UserService.java where errors were reported
   ✓ 187 líneas leídas

   Pensando...
   Tool: write_file
   Razón: Fixing null pointer and missing return statement
   ✓ UserService.java actualizado (189 líneas)

   Pensando...
   Tool: run_command
   Razón: Verifying the fix compiles correctly
   ✓ BUILD SUCCESS

   Tarea completada en 5 paso(s).
```

---

## Features

| Capacidad | Descripción |
|-----------|-------------|
| **ReAct Loop** | Reason → Act → Observe. Planea, ejecuta y corrige hasta completar la tarea |
| **Tool System** | Cada herramienta es una `AgentTool` independiente, testeable y registrable en caliente |
| **Git nativo** | Operaciones Git completas via JGit — sin depender de `git` instalado en el sistema |
| **Análisis AST** | Analiza código Java con JavaParser: clases, métodos, dependencias y complejidad ciclomática |
| **Sandbox de seguridad** | `CommandValidator` bloquea comandos destructivos antes de ejecutarlos |
| **Multiplataforma** | Windows (PowerShell), Linux y macOS (bash/zsh) via `OSAbstraction` |
| **Model routing** | Soporta Anthropic + OpenAI con fallback automático entre proveedores |
| **Extensible** | Añadir una tool nueva = implementar una interfaz + una línea de registro |

---

## Quickstart

### Requisitos

- **Java 21+**
- **API Key** de Anthropic u OpenAI

> El proyecto incluye Gradle Wrapper — no necesitas instalar Gradle. `gradlew.bat` descarga la versión correcta automáticamente.

### 1 — Clonar y compilar

```bash
git clone https://github.com/erac73/Ai-coding-agent.git
cd Ai-coding-agent/ai-coding-agent
./gradlew shadowJar
```

> En Windows PowerShell usa `.\gradlew shadowJar`

Genera `build/libs/agent.jar` — fat JAR con todas las dependencias incluidas.

### 2 — Configurar API Key

```bash
# Opción A: variable de entorno (recomendada)
export ANTHROPIC_API_KEY=sk-ant-...

# Opción B: archivo de configuración
java -jar build/libs/agent.jar --init
# Edita ~/.agent/config.json con tu API key
```

### 3 — Usar

```bash
# Tarea directa
java -jar build/libs/agent.jar "analiza este proyecto y dime qué hace"

# En un directorio específico
java -jar build/libs/agent.jar --dir /ruta/mi-proyecto "corrige los errores"

# Modo interactivo (REPL)
java -jar build/libs/agent.jar --interactive

# Alias global recomendado — añadir a .bashrc / .zshrc
alias agent='java -jar /ruta/completa/agent.jar'
agent "crea una clase User con nombre y email"
```

---

## Tools disponibles

| Tool | Acción | Descripción |
|------|--------|-------------|
| `read_file` | — | Lee el contenido completo de un archivo (hasta 50k caracteres) |
| `write_file` | — | Crea o sobreescribe un archivo; crea carpetas padre automáticamente |
| `edit_file` | — | Reemplazo quirúrgico de texto (`oldString` → `newString`), soporta `replaceAll` |
| `list_dir` | — | Árbol del proyecto hasta 3 niveles, ignora `build/`, `node_modules/`, `.git/` |
| `grep` | — | Busca patrones regex en archivos, con filtro por extensión y exclusión de carpetas |
| `run_command` | — | Ejecuta comandos en terminal con sandbox de seguridad y timeout de 30s |
| `git` | `status` | Estado del working tree y staging area |
| `git` | `log` | Historial de commits con autor, fecha y mensaje |
| `git` | `diff` | Cambios pendientes en formato diff |
| `git` | `add` | Añade archivos al staging area |
| `git` | `commit` | Hace commit con el mensaje dado |
| `git` | `branch` | Lista o crea branches |
| `git` | `checkout` | Cambia de branch o restaura archivos |
| `analyze_code` | `analyze` | Análisis profundo de un archivo `.java`: campos, métodos, complejidad |
| `analyze_code` | `summarize` | Resumen de todas las clases/métodos del proyecto |
| `analyze_code` | `find` | Busca usos de una clase o método en todo el proyecto |
| `analyze_code` | `metrics` | Métricas de complejidad ciclomática por archivo |

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    CLI (Picocli)                             │
│          agent "tarea"  /  agent --interactive              │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                 AgentOrchestrator                            │
│              ┌─── ReAct Loop ───────────────┐               │
│              │  1. REASON  → llamar al LLM  │               │
│              │  2. ACT     → ejecutar tool  │               │
│              │  3. OBSERVE → añadir al ctx  │               │
│              │  4. REPEAT  → hasta completar│               │
│              └──────────────────────────────┘               │
└──────┬──────────────────┬───────────────────┬───────────────┘
       │                  │                   │
┌──────▼──────┐  ┌────────▼───────┐  ┌───────▼────────┐
│ ModelRouter │  │  ToolRegistry  │  │ ShortTermMemory│
│             │  │                │  │                │
│ • Anthropic │  │ • ReadFile     │  │ • Historial    │
│ • OpenAI    │  │ • WriteFile    │  │ • Compresión   │
│ • Ollama    │  │ • EditFile     │  │   automática   │
│             │  │ • ListDir      │  └────────────────┘
│ fallback    │  │ • Grep         │
│ automático  │  │ • RunCommand   │
│             │  │ • GitTool (JGit)│
│             │  │ • ASTAnalyzer  │
└─────────────┘  └────────────────┘
```

Siguiendo **Clean Architecture** + **Hexagonal Architecture**:

- `model/` — Domain puro. `AgentContext`, `ToolResult` sin dependencias externas
- `orchestrator/` + `tools/` + `memory/` — Application layer
- `ai/providers/` + `config/` — Infrastructure / adaptadores externos
- `cli/` — Interface layer (entrada del usuario)

---

## Añadir una tool nueva

Implementar la interfaz y registrarla es todo lo que hace falta:

```java
// 1. Implementar AgentTool
public class MyTool implements AgentTool {

    @Override
    public String getName() { return "my_tool"; }

    @Override
    public String getDescription() {
        // El LLM lee esto para decidir cuándo usar la tool
        return "Does X given Y. Use when the user needs Z.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "input", "The input to process",
            "mode",  "(Optional) fast | thorough"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext ctx) {
        String input = params.get("input");
        // ... tu lógica aquí
        return ToolResult.ok(getName(), "Result: " + input);
    }
}
```

```java
// 2. Registrarla en AgentFactory.java — una sola línea
registry.register(new MyTool());
```

El agente la usará automáticamente en la próxima ejecución. Sin configuración adicional.

---

## Configuración

### Variables de entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `ANTHROPIC_API_KEY` | API key de Anthropic (Claude) | — |
| `OPENAI_API_KEY` | API key de OpenAI | — |
| `ANTHROPIC_MODEL` | Modelo a usar | `claude-sonnet-4-20250514` |
| `OLLAMA_BASE_URL` | URL de Ollama para modelos locales | `http://localhost:11434` |
| `AGENT_STRICT_SECURITY` | Activa el modo estricto de seguridad | `false` |

### `~/.agent/config.json`

```json
{
  "anthropicApiKey": "sk-ant-...",
  "openAiApiKey": "",
  "anthropicModel": "claude-sonnet-4-20250514",
  "ollamaBaseUrl": "http://localhost:11434",
  "strictSecurity": false
}
```

Generar el archivo base con: `agent --init`

---

## Seguridad

El `CommandValidator` intercepta y bloquea **siempre**, independientemente del contexto:

| Patrón bloqueado | Motivo |
|-----------------|--------|
| `rm -rf /` y variantes | Elimina todo el sistema de archivos |
| Fork bombs `: () { :|: & }; :` | Colapsa el sistema |
| `mkfs.*` / `dd of=/dev/sd*` | Formatea discos |
| `shutdown`, `halt`, `reboot` | Apaga el sistema |
| `passwd root` | Cambia contraseña root |
| `DROP DATABASE` | Destruye bases de datos |

Con `strictSecurity=true` también requiere confirmación explícita para: `rm -rf`, `git push --force`, `git reset --hard`, `DROP TABLE`.

Los logs de auditoría se guardan en `~/.agent/logs/agent.log`.

---

## Roadmap

### Fase 1 — MVP CLI
- [x] CLI con Picocli — modo tarea directa + modo REPL interactivo
- [x] ReAct loop completo (Reason → Act → Observe)
- [x] Tool system extensible (`AgentTool` + `ToolRegistry`)
- [x] Filesystem tools (`read_file`, `write_file`, `edit_file`, `list_dir`, `grep`)
- [x] Terminal tool con sandbox de seguridad
- [x] Memoria de corto plazo con compresión automática
- [x] Integración Anthropic API (Claude) + OpenAI API (GPT-4o)
- [x] OS Abstraction (Windows / Linux / macOS / WSL)
- [x] Git nativo via JGit (status, log, diff, add, commit, branch, checkout)
- [x] Logging estructurado con Logback (consola + rolling file)
- [x] Tests unitarios (JUnit 5 + Mockito + AssertJ) — 6 suites, 27+ tests

---

## Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 21 |
| Build | Gradle 8 + Shadow plugin |
| CLI | Picocli 4.7 |
| IA / LLMs | Anthropic API (Claude), LangChain4j |
| Git | JGit 6.8 |
| AST | JavaParser 3.25 |
| Terminal | JLine3 3.25 + Jansi |
| Logging | SLF4J + Logback |
| Serialización | Jackson 2.17 |
| Tests | JUnit 5 + Mockito + AssertJ |

---

## Tests

```bash
# Ejecutar todos los tests
./gradlew test

# Con reporte detallado
./gradlew test --info

# Reporte HTML en: build/reports/tests/test/index.html
```

---

## Estructura del proyecto

```
ai-coding-agent/
├── src/
│   ├── main/java/com/agent/
│   │   ├── cli/                    ← AgentCommand, InteractiveMode
│   │   ├── orchestrator/           ← AgentOrchestrator (ReAct loop)
│   │   ├── tools/
│   │   │   ├── AgentTool.java      ← Interfaz base
│   │   │   ├── ToolRegistry.java   ← Registro central
│   │   │   ├── filesystem/         ← ReadFile, WriteFile, EditFile, ListDir, Grep
│   │   │   ├── terminal/           ← RunCommand, OSAbstraction
│   │   │   ├── git/                ← GitTool (JGit)
│   │   │   └── analysis/           ← ASTAnalyzerTool (JavaParser)
│   │   ├── ai/
│   │   │   ├── ModelRouter.java    ← Enrutamiento entre LLMs
│   │   │   ├── PromptManager.java  ← Construcción de prompts
│   │   │   └── providers/          ← Anthropic, OpenAI, Ollama
│   │   ├── memory/                 ← ShortTermMemory
│   │   ├── security/               ← CommandValidator
│   │   ├── model/                  ← AgentContext, ToolResult
│   │   └── config/                 ← AgentConfig, AgentFactory
│   └── test/java/com/agent/
│       ├── ToolRegistryTest.java
│       ├── tools/filesystem/      ← EditFileToolTest, GrepToolTest
│       ├── security/              ← CommandValidatorTest
│       ├── memory/                ← ShortTermMemoryTest
│       └── model/                 ← ToolResultTest
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Requisitos del sistema

```bash
# Verificar Java
java -version    # Debe ser 21+

# Instalar Java 21 en Ubuntu/Debian
sudo apt install openjdk-21-jdk

# Instalar Java 21 en Arch Linux
sudo pacman -S jdk21-openjdk

# Instalar Java 21 en Fedora
sudo dnf install java-21-openjdk-devel
```

---

## Licencia

Este proyecto está bajo la licencia **MIT** — libre para uso personal y comercial.

---

<div align="center">
  Construido con ☕ Java 21 · Arquitectura Clean + Hexagonal
</div>
