package com.agent.tools.terminal;

/**
 * Abstrae las diferencias entre Windows, Linux y macOS.
 * Todos los módulos deben usar esta clase en vez de detectar el OS ellos mismos.
 */
public class OSAbstraction {

    public enum OS { WINDOWS, LINUX, MAC, UNKNOWN }

    private static final OS CURRENT;

    static {
        String name = System.getProperty("os.name", "").toLowerCase();
        if (name.contains("win"))        CURRENT = OS.WINDOWS;
        else if (name.contains("mac"))   CURRENT = OS.MAC;
        else if (name.contains("nix") || name.contains("nux") || name.contains("aix")) CURRENT = OS.LINUX;
        else                             CURRENT = OS.UNKNOWN;
    }

    public static OS getCurrent()     { return CURRENT; }
    public static boolean isWindows() { return CURRENT == OS.WINDOWS; }
    public static boolean isLinux()   { return CURRENT == OS.LINUX; }
    public static boolean isMac()     { return CURRENT == OS.MAC; }

    /**
     * Devuelve el comando shell correcto según el OS.
     * En Windows usa PowerShell. En Unix usa bash/sh.
     */
    public static String[] wrapCommand(String command) {
        if (isWindows()) {
            return new String[]{"powershell.exe", "-NoProfile", "-Command", command};
        } else {
            String shell = System.getenv("SHELL");
            if (shell == null || shell.isBlank()) shell = "/bin/sh";
            return new String[]{shell, "-c", command};
        }
    }

    /** Normaliza separadores de ruta según el OS actual. */
    public static String normalizePath(String path) {
        if (isWindows()) return path.replace("/", "\\");
        return path.replace("\\", "/");
    }

    /** Detecta si hay WSL disponible en Windows. */
    public static boolean isWSLAvailable() {
        if (!isWindows()) return false;
        try {
            Process p = new ProcessBuilder("wsl", "--status")
                .redirectErrorStream(true)
                .start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
