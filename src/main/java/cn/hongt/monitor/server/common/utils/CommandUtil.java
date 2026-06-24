package cn.hongt.monitor.server.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author yrb
 * @date 2021/7/20
 * @Description: 调用Linux命令工具类
 */
@Slf4j
@Component
public class CommandUtil {

    private CommandUtil() {
    }

    private static final long DEFAULT_TIMEOUT_MS = 10_000L;
    private static final int DESTROY_WAIT_SECONDS = 1;
    private static final int STREAM_READ_WAIT_SECONDS = 3;
    private static final int LOG_OUTPUT_MAX_LENGTH = 1000;
    private static volatile Executor commandExecutor;

    @Autowired
    @Qualifier("commandExecutor")
    public void setCommandExecutor(Executor executor) {
        CommandUtil.commandExecutor = executor;
    }

    public static String execuCmd(String cmd) {
        if (StringUtils.isBlank(cmd)) {
            log.warn("execuCmd 命令为空");
            return "";
        }
        //log.info("execuCmd 开始执行命令，cmd={}", cmd);
        CommandResult result = executeCommand(new String[]{"sh", "-c", cmd}, DEFAULT_TIMEOUT_MS);
        logWhenFailed("execuCmd", result);
        String output = joinText(result.stdoutLines, false);
        //log.info("execuCmd 执行完成，outputLength={}", output.length());
        return output;
    }

    public static String diskCmd(String[] cmd) {
        //log.info("diskCmd 开始执行命令，cmd={}", Arrays.toString(cmd));
        CommandResult result = executeCommand(cmd, DEFAULT_TIMEOUT_MS);
        logWhenFailed("diskCmd", result);
        String output = joinText(result.stdoutLines, true);
        //log.info("diskCmd 执行完成，outputLength={}", output.length());
        return output;
    }

    public static String containerCmd(String[] cmd) {
        //log.info("containerCmd 开始执行命令，cmd={}", Arrays.toString(cmd));
        CommandResult result = executeCommand(cmd, DEFAULT_TIMEOUT_MS);
        logWhenFailed("containerCmd", result);
        String output = joinCommaText(result.stdoutLines);
        //log.info("containerCmd 执行完成，itemCount={}", result.stdoutLines.size());
        return output;
    }

    private static CommandResult executeCommand(String[] cmd, long timeoutMs) {
        String commandText = Arrays.toString(cmd);
        if (isInvalidCommand(cmd)) {
            log.error("命令参数非法，command={}", commandText);
            return new CommandResult(cmd, -1, false, Collections.<String>emptyList(),
                    Collections.singletonList("invalid command"), 0L, timeoutMs, new IllegalArgumentException("invalid command"));
        }

        Executor executor = commandExecutor;
        if (executor == null) {
            log.error("commandExecutor 未注入，无法执行命令，command={}", commandText);
            return new CommandResult(cmd, -1, false, Collections.<String>emptyList(),
                    Collections.singletonList("commandExecutor not initialized"), 0L, timeoutMs, new IllegalStateException("commandExecutor not initialized"));
        }

        long start = System.currentTimeMillis();
        Process process = null;
        FutureTask<List<String>> stdoutTask = null;
        FutureTask<List<String>> stderrTask = null;
        //log.info("开始执行系统命令，command={}, timeoutMs={}", commandText, timeoutMs);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            process = processBuilder.start();
            final Process currentProcess = process;

            // 使用 commandExecutor 执行流读取任务。
            // FutureTask 桥接 Callable → Runnable，兼容 Executor 接口。
            // stdout 和 stderr 分开读取，避免 Docker CLI 的 stderr 警告混入 stdout 导致解析损坏。
            stdoutTask = new FutureTask<>(new StreamReader(currentProcess.getInputStream()));
            stderrTask = new FutureTask<>(new StreamReader(currentProcess.getErrorStream()));
            executor.execute(stdoutTask);
            executor.execute(stderrTask);

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                if (!process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS);
                }
            }

            List<String> stdoutLines = getFutureLines(stdoutTask, "stdout", cmd);
            List<String> stderrLines = getFutureLines(stderrTask, "stderr", cmd);
            long durationMs = System.currentTimeMillis() - start;
            int exitCode = finished ? process.exitValue() : -1;
            if (finished && exitCode == 0) {
                //log.info("系统命令执行成功，command={}, durationMs={}, stdoutLineCount={}", commandText, durationMs, stdoutLines.size());
            }
            return new CommandResult(cmd, exitCode, !finished, stdoutLines, stderrLines, durationMs, timeoutMs, null);
        } catch (IOException e) {
            return new CommandResult(cmd, -1, false, Collections.<String>emptyList(),
                    Collections.singletonList("start process failed: " + e.getMessage()), System.currentTimeMillis() - start, timeoutMs, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (stdoutTask != null) {
                stdoutTask.cancel(true);
            }
            if (stderrTask != null) {
                stderrTask.cancel(true);
            }
            if (process != null) {
                process.destroyForcibly();
            }
            return new CommandResult(cmd, -1, false, Collections.<String>emptyList(),
                    Collections.singletonList("command interrupted: " + e.getMessage()), System.currentTimeMillis() - start, timeoutMs, e);
        } finally {
            if (process != null) {
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
                closeQuietly(process.getOutputStream());
                process.destroy();
            }
        }
    }

    private static List<String> getFutureLines(FutureTask<List<String>> task, String streamName, String[] cmd) {
        try {
            return task.get(STREAM_READ_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.cancel(true);
            // 流读取失败意味着命令输出数据丢失，升级为 ERROR 便于运维发现
            log.error("读取{}被中断，command={}", streamName, Arrays.toString(cmd));
        } catch (ExecutionException e) {
            task.cancel(true);
            log.error("读取{}失败，command={}, error={}", streamName, Arrays.toString(cmd), e.getMessage());
        } catch (TimeoutException e) {
            task.cancel(true);
            log.error("读取{}超时，command={}, error={}", streamName, Arrays.toString(cmd), e.getMessage());
        }
        return Collections.emptyList();
    }

    private static String joinText(List<String> lines, boolean trimEachLine) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(trimEachLine ? StringUtils.trimToEmpty(line) : line);
        }
        return out.toString();
    }

    private static String joinCommaText(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(line);
        }
        return out.toString();
    }

    private static boolean isInvalidCommand(String[] cmd) {
        if (cmd == null || cmd.length == 0) {
            return true;
        }
        for (String item : cmd) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    private static void logWhenFailed(String methodName, CommandResult result) {
        if (result == null || result.isSuccess()) {
            return;
        }

        String command = Arrays.toString(result.command);
        String stdout = StringUtils.abbreviate(joinText(result.stdoutLines, false), LOG_OUTPUT_MAX_LENGTH);
        String stderr = StringUtils.abbreviate(joinText(result.stderrLines, false), LOG_OUTPUT_MAX_LENGTH);

        if (result.timedOut) {
            log.error("{} 执行超时，command={}, timeoutMs={}, stdout={}, stderr={}",
                    methodName, command, result.timeoutMs, stdout, stderr);
            return;
        }

        if (result.exception != null) {
            log.error("{} 执行异常，command={}, durationMs={}, stdout={}, stderr={}",
                    methodName, command, result.durationMs, stdout, stderr, result.exception);
            return;
        }

        log.error("{} 执行失败，command={}, exitCode={}, durationMs={}, stdout={}, stderr={}",
                methodName, command, result.exitCode, result.durationMs, stdout, stderr);
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            log.warn("关闭流失败", e);
        }
    }

    private static class StreamReader implements Callable<List<String>> {

        private final InputStream inputStream;

        private StreamReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public List<String> call() throws Exception {
            List<String> lines = new ArrayList<String>();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                return lines;
            } finally {
                closeQuietly(reader);
            }
        }
    }

    private static class CommandResult {

        private final String[] command;
        private final int exitCode;
        private final boolean timedOut;
        private final List<String> stdoutLines;
        private final List<String> stderrLines;
        private final long durationMs;
        private final long timeoutMs;
        private final Exception exception;

        private CommandResult(String[] command, int exitCode, boolean timedOut,
                              List<String> stdoutLines, List<String> stderrLines,
                              long durationMs, long timeoutMs, Exception exception) {
            this.command = command;
            this.exitCode = exitCode;
            this.timedOut = timedOut;
            this.stdoutLines = stdoutLines == null ? Collections.<String>emptyList() : stdoutLines;
            this.stderrLines = stderrLines == null ? Collections.<String>emptyList() : stderrLines;
            this.durationMs = durationMs;
            this.timeoutMs = timeoutMs;
            this.exception = exception;
        }

        private boolean isSuccess() {
            return !timedOut && exception == null && exitCode == 0;
        }
    }
}
