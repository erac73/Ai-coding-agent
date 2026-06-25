# AI Coding Agent

Agente de IA autónomo para coding, construido en Java. Similar a Claude Code u OpenCode pero con arquitectura propia.

## Quickstart

### 1. Requisitos
- Java 21+
- Gradle 8+ (o usa el wrapper incluido)

### 2. Compilar

El proyecto incluye Gradle Wrapper — no necesitas instalar Gradle:

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

# Modo interactivo (REPL)
java -jar build/libs/agent.jar --interactive

# Alias útil (añadir a .bashrc / .zshrc)
alias agent='java -jar /path/to/agent.jar'
agent "crea una clase User con nombre y email"
```

## Arquitectura

```
AgentCommand (CLI)
    └── AgentOrchestrator (ReAct loop)
            ├── ModelRouter (Anthropic / OpenAI)
            ├── ToolRegistry
            │       ├── ReadFileTool
            │       ├── WriteFileTool
            │       ├── EditFileTool     ← reemplazo quirúrgico
            │       ├── ListDirTool
            │       ├── GrepTool         ← búsqueda regex
            │       ├── GitTool          ← JGit (status/log/commit/etc)
            │       └── RunCommandTool
            ├── PromptManager
            ├── ShortTermMemory
            └── Tests: 6 suites, 27+ tests
```

## Tools disponibles

| Tool | Descripción |
|------|-------------|
| Tool | Descripción |
|------|-------------|
| `read_file` | Lee el contenido completo de un archivo |
| `write_file` | Crea o sobreescribe un archivo con contenido |
| `edit_file` | Reemplazo quirúrgico: busca `oldString` y lo cambia por `newString`. Soporta `replaceAll=true` |
| `list_dir` | Lista la estructura de un directorio (hasta 3 niveles) |
| `grep` | Busca patrones regex en archivos, con filtro por extensión |
| `git` | Operaciones Git: status, log, diff, add, commit, branch, checkout |
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
- [x] Filesystem tools (read, write, edit, list, grep)
- [x] GitTool via JGit (status, log, diff, add, commit, branch, checkout)
- [x] Terminal tool con sandbox de seguridad
- [x] Memoria de corto plazo
- [x] Multi-provider (Anthropic + OpenAI)
- [x] Logging estructurado (Logback)
- [x] Tests unitarios — 6 suites, 27+ tests
- [ ] ASTAnalyzerTool (JavaParser)
- [ ] RAG + memoria largo plazo (Qdrant)
- [ ] Modo interactivo mejorado (JLine3)
- [ ] Agente de terminal (modo REPL con historial)
- [ ] Ollama provider para modelos locales
- [ ] Spring Boot API REST
- [ ] Multi-agent orchestration

## Tests

```bash
./gradlew test
```

## Logs

Los logs se guardan en `~/.agent/logs/agent.log`
