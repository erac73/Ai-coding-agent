# AI Coding Agent

Agente de IA autónomo para coding, construido en Java. Similar a Claude Code u OpenCode pero con arquitectura propia.

## Quickstart

### 1. Requisitos
- Java 21+
- API key de Anthropic, OpenAI u Ollama (local)

El proyecto incluye Gradle Wrapper — no necesitas instalar Gradle.

### 2. Compilar

```bash
# En PowerShell:
.\gradlew shadowJar

# En bash/zsh:
./gradlew shadowJar
```

Genera `build/libs/agent.jar`

### 3. Configurar API key

```bash
# Opción A: variable de entorno
export ANTHROPIC_API_KEY=sk-ant-...

# Opción B: archivo de config
java -jar build/libs/agent.jar --init
# Edita ~/.agent/config.json
```

### 4. Usar

```bash
# Tarea directa
java -jar build/libs/agent.jar "analiza este proyecto y explica qué hace"

# En un directorio específico
java -jar build/libs/agent.jar --dir /path/to/my/project "corrige los errores de compilación"

# Modo interactivo (REPL con historial y autocompletado)
java -jar build/libs/agent.jar --interactive

# Docker
docker build -t agent .
docker run agent "analiza el proyecto"
```

## Arquitectura

```
AgentCommand (CLI)
    └── AgentOrchestrator (ReAct loop)
            ├── ModelRouter (Anthropic / OpenAI / Ollama)
            ├── ToolRegistry
            │       ├── ReadFileTool
            │       ├── WriteFileTool
            │       ├── EditFileTool         ← reemplazo quirúrgico
            │       ├── DeleteFileTool       ← eliminar archivos
            │       ├── MoveFileTool         ← mover/copiar archivos
            │       ├── ListDirTool
            │       ├── GrepTool             ← búsqueda regex
            │       ├── GitTool              ← JGit
            │       ├── ASTAnalyzerTool      ← JavaParser
            │       ├── FetchUrlTool         ← HTTP
            │       └── RunCommandTool
            ├── PromptManager
            ├── ShortTermMemory
            └── Tests: 12 suites, 40+ tests
```

## Tools disponibles

| Tool | Descripción |
|------|-------------|
| `read_file` | Lee el contenido completo de un archivo |
| `write_file` | Crea o sobreescribe un archivo con contenido |
| `edit_file` | Reemplazo quirúrgico: busca `oldString` y lo cambia por `newString`. Soporta `replaceAll=true` |
| `delete_file` | Elimina un archivo o directorio vacío |
| `move_file` | Mueve, renombra o copia archivos y directorios |
| `list_dir` | Lista la estructura de un directorio (hasta 3 niveles) |
| `grep` | Busca patrones regex en archivos, con filtro por extensión |
| `git` | Operaciones Git: status, log, diff, add, commit, branch, checkout |
| `analyze_code` | Analiza código Java con AST: analyze, summarize, find, metrics |
| `fetch_url` | Obtiene contenido de URLs (documentación, APIs) |
| `run_command` | Ejecuta comandos en terminal con sandbox de seguridad |

## Añadir una nueva tool

```java
public class MyCustomTool implements AgentTool {

    @Override public String getName() { return "my_tool"; }

    @Override public String getDescription() {
        return "Description the LLM will read to decide when to use this tool";
    }

    @Override public Map<String, String> getParameterDescriptions() {
        return Map.of("input", "The input parameter description");
    }

    @Override public ToolResult execute(Map<String, String> params, AgentContext ctx) {
        String input = params.get("input");
        // ... tu lógica aquí
        return ToolResult.ok(getName(), "Result: " + input);
    }
}
```

Registrarla en `AgentFactory`:
```java
registry.register(new MyCustomTool());
```

## Roadmap

- [x] CLI básico (Picocli)
- [x] Tool system extensible
- [x] ReAct loop (Reason → Act → Observe)
- [x] Filesystem tools (read, write, edit, delete, move, list, grep)
- [x] GitTool via JGit (status, log, diff, add, commit, branch, checkout)
- [x] ASTAnalyzerTool (JavaParser) — analyze, summarize, find, metrics
- [x] Terminal tool con sandbox de seguridad
- [x] Memoria de corto plazo
- [x] Multi-provider (Anthropic + OpenAI + Ollama)
- [x] REPL con JLine3 (historial, autocompletado Tab)
- [x] Fetch URL tool (documentación online)
- [x] Logging estructurado (Logback)
- [x] Docker multi-stage
- [x] CI/CD con GitHub Actions
- [x] Tests unitarios — 12 suites, 40+ tests
- [ ] RAG + memoria largo plazo (Qdrant)
- [ ] Spring Boot API REST
- [ ] Multi-agent orchestration
- [ ] Plugin system (JARs externos)
- [ ] Native Image (GraalVM)

## Tests

```bash
# Todos los tests
./gradlew test

# Tests específicos
./gradlew test --tests "*GitToolTest"
./gradlew test --tests "*AgentOrchestratorTest"
```

## Logs

Los logs se guardan en `~/.agent/logs/agent.log`
