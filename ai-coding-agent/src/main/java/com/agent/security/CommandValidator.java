package com.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Valida comandos antes de ejecutarlos.
 *
 * No intenta ser un sandbox perfecto — ese nivel requiere containers.
 * El objetivo es evitar accidentes y comandos claramente destructivos.
 */
public class CommandValidator {

    private static final Logger log = LoggerFactory.getLogger(CommandValidator.class);

    public record ValidationResult(boolean isAllowed, String reason) {
        public static ValidationResult allow() { return new ValidationResult(true, null); }
        public static ValidationResult block(String reason) { return new ValidationResult(false, reason); }
    }

    // Comandos que eliminan datos de forma irreversible
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        Pattern.compile("rm\\s+-rf\\s+/"),                      // rm -rf /
        Pattern.compile("rm\\s+-rf\\s+~"),                      // rm -rf ~
        Pattern.compile(":\\s*\\(\\s*\\)\\s*\\{.*fork"),        // fork bomb
        Pattern.compile("mkfs\\."),                             // formatear disco
        Pattern.compile("dd\\s+.*of=/dev/[sh]d"),               // sobreescribir disco
        Pattern.compile(">\\s*/dev/[sh]d"),                     // sobreescribir dispositivo
        Pattern.compile("chmod\\s+-R\\s+777\\s+/"),             // chmod 777 raíz
        Pattern.compile("sudo\\s+rm\\s+-rf"),                   // sudo rm -rf
        Pattern.compile("DROP\\s+DATABASE", Pattern.CASE_INSENSITIVE), // SQL destructivo
        Pattern.compile("shutdown\\s+-[hr]\\s+now"),            // apagar el sistema
        Pattern.compile("halt|poweroff|reboot"),                // reinicio
        Pattern.compile("passwd\\s+root")                       // cambiar password root
    );

    // Comandos que requieren confirmación explícita del usuario
    private static final List<Pattern> WARN_PATTERNS = List.of(
        Pattern.compile("rm\\s+-rf"),
        Pattern.compile("git\\s+push.*--force"),
        Pattern.compile("git\\s+reset\\s+--hard"),
        Pattern.compile("DROP\\s+TABLE", Pattern.CASE_INSENSITIVE),
        Pattern.compile("truncate\\s+table", Pattern.CASE_INSENSITIVE)
    );

    private final boolean strictMode;

    public CommandValidator() {
        this(false);
    }

    public CommandValidator(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public ValidationResult validate(String command) {
        if (command == null || command.isBlank()) {
            return ValidationResult.block("Empty command");
        }

        // Bloquear siempre
        for (Pattern p : DANGEROUS_PATTERNS) {
            if (p.matcher(command).find()) {
                log.warn("BLOCKED dangerous command pattern '{}' in: {}", p.pattern(), command);
                return ValidationResult.block(
                    "Command matches dangerous pattern: %s".formatted(p.pattern()));
            }
        }

        // En modo estricto, bloquear también los que necesitan confirmación
        if (strictMode) {
            for (Pattern p : WARN_PATTERNS) {
                if (p.matcher(command).find()) {
                    return ValidationResult.block(
                        "Command requires explicit confirmation in strict mode: " + command);
                }
            }
        }

        return ValidationResult.allow();
    }
}
