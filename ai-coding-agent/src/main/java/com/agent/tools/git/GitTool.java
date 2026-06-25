package com.agent.tools.git;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.log.LogCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class GitTool implements AgentTool {

    @Override
    public String getName() { return "git"; }

    @Override
    public String getDescription() {
        return "Executes Git operations on the repository. Supports status, log, diff, add, commit, branch, checkout. " +
               "Uses JGit internally, no system git required. Operates on the current working directory.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "action", "Git operation to perform: status | log | diff | add | commit | branch | checkout",
            "path", "(Optional) File path for add/checkout operations",
            "message", "(Optional) Commit message (required for commit action)",
            "branch", "(Optional) Branch name for branch/checkout operations"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String action = params.get("action");
        if (action == null || action.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'action' is required: status, log, diff, add, commit, branch, checkout");
        }

        try {
            Path repoDir = context.getWorkingDirectory();
            try (Repository repository = openRepository(repoDir)) {
                return switch (action.toLowerCase()) {
                    case "status" -> status(repository);
                    case "log" -> log(repository, params);
                    case "diff" -> diff(repository);
                    case "add" -> add(repository, params);
                    case "commit" -> commit(repository, params);
                    case "branch" -> branch(repository, params);
                    case "checkout" -> checkout(repository, params);
                    default -> ToolResult.fail(getName(), "Unknown git action: " + action + ". Available: status, log, diff, add, commit, branch, checkout");
                };
            }
        } catch (Exception e) {
            return ToolResult.fail(getName(), "Git operation failed: " + e.getMessage());
        }
    }

    private Repository openRepository(Path dir) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setWorkTree(dir.toFile())
            .findGitDir(dir.toFile())
            .build();
    }

    private ToolResult status(Repository repository) throws Exception {
        Git git = new Git(repository);
        Status status = git.status().call();

        StringBuilder sb = new StringBuilder();
        sb.append("Branch: ").append(repository.getBranch()).append("\n");
        sb.append("Changes to be committed:\n");
        status.getChanged().forEach(f -> sb.append("  modified:   ").append(f).append("\n"));
        status.getAdded().forEach(f -> sb.append("  added:      ").append(f).append("\n"));
        status.getRemoved().forEach(f -> sb.append("  deleted:    ").append(f).append("\n"));

        sb.append("\nUnstaged changes:\n");
        status.getModified().forEach(f -> sb.append("  modified:   ").append(f).append("\n"));
        status.getMissing().forEach(f -> sb.append("  deleted:    ").append(f).append("\n"));

        sb.append("\nUntracked files:\n");
        status.getUntracked().forEach(f -> sb.append("  ").append(f).append("\n"));

        return ToolResult.ok(getName(), sb.toString(),
            Map.of("branch", repository.getBranch(), "action", "status"));
    }

    private ToolResult log(Repository repository, Map<String, String> params) throws Exception {
        int count = parseInt(params.getOrDefault("count", "10"), 10);
        Git git = new Git(repository);
        LogCommand log = git.log().setMaxCount(count);

        StringBuilder sb = new StringBuilder();
        for (RevCommit commit : log.call()) {
            sb.append(commit.getId().abbreviate(8).name()).append(" ")
              .append(commit.getAuthorIdent().getName()).append(" ")
              .append(commit.getCommitTime()).append("\n")
              .append("  ").append(commit.getShortMessage()).append("\n");
        }
        return ToolResult.ok(getName(), sb.toString(), Map.of("action", "log", "count", count));
    }

    private ToolResult diff(Repository repository) throws Exception {
        Git git = new Git(repository);
        String diffText = git.diff().call().stream()
            .map(d -> d.toString())
            .reduce("", (a, b) -> a + b);

        if (diffText.isEmpty()) {
            return ToolResult.ok(getName(), "No unstaged changes.");
        }
        return ToolResult.ok(getName(), diffText, Map.of("action", "diff"));
    }

    private ToolResult add(Repository repository, Map<String, String> params) throws Exception {
        String path = params.get("path");
        if (path == null || path.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'path' is required for add action");
        }
        Git git = new Git(repository);
        git.add().addFilepattern(path).call();
        return ToolResult.ok(getName(), "Added to staging: " + path, Map.of("action", "add", "path", path));
    }

    private ToolResult commit(Repository repository, Map<String, String> params) throws Exception {
        String message = params.get("message");
        if (message == null || message.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'message' is required for commit action");
        }
        Git git = new Git(repository);
        RevCommit commit = git.commit().setMessage(message).call();
        return ToolResult.ok(getName(),
            "Committed: " + commit.getId().abbreviate(8).name() + " - " + message,
            Map.of("action", "commit", "hash", commit.getId().name()));
    }

    private ToolResult branch(Repository repository, Map<String, String> params) throws Exception {
        Git git = new Git(repository);
        String branchName = params.get("branch");

        StringBuilder sb = new StringBuilder();
        if (branchName == null || branchName.isBlank()) {
            git.branchList().call().forEach(ref ->
                sb.append(ref.getName().replace("refs/heads/", "")).append("\n"));
        } else {
            git.branchCreate().setName(branchName).call();
            sb.append("Created branch: ").append(branchName);
        }
        return ToolResult.ok(getName(), sb.toString(), Map.of("action", "branch"));
    }

    private ToolResult checkout(Repository repository, Map<String, String> params) throws Exception {
        String branch = params.get("branch");
        String path = params.get("path");

        Git git = new Git(repository);

        if (branch != null && !branch.isBlank()) {
            git.checkout().setName(branch).call();
            return ToolResult.ok(getName(), "Switched to branch: " + branch,
                Map.of("action", "checkout", "branch", branch));
        } else if (path != null && !path.isBlank()) {
            git.checkout().addPath(path).call();
            return ToolResult.ok(getName(), "Restored file: " + path,
                Map.of("action", "checkout", "path", path));
        } else {
            return ToolResult.fail(getName(), "Parameter 'branch' or 'path' is required for checkout");
        }
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
