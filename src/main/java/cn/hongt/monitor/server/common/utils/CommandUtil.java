package cn.hongt.monitor.server.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yrb
 * @date 2021/7/20
 * @Description: 调用Linux命令工具类
 */
@Slf4j
public final class CommandUtil {

    private static final long DEFAULT_TIMEOUT_MS = 10_000L;
    private static final int DESTROY_WAIT_SECONDS = 1;
    private static final int STREAM_READ_WAIT_SECONDS = 3;
    private static final int LOG_OUTPUT_MAX_LENGTH = 1000;
    private static final Long DEFAULT_DISK_IO_VALUE = 0L;
    private static final AtomicInteger STREAM_READER_THREAD_INDEX = new AtomicInteger(1);
    private static final ExecutorService STREAM_READER_EXECUTOR = createStreamReaderExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdownStreamReaderExecutor();
            }
        }, "command-util-shutdown"));
    }

    private CommandUtil() {
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

    public static HashMap<String, List<String>> dockerStatsCmd(String[] cmd) {
        //log.info("dockerStatsCmd 开始执行命令，cmd={}", Arrays.toString(cmd));
        HashMap<String, List<String>> dockerStatsMap = new HashMap<String, List<String>>();
        CommandResult result = executeCommand(cmd, DEFAULT_TIMEOUT_MS);
        logWhenFailed("dockerStatsCmd", result);

        for (String line : result.stdoutLines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            List<String> tempList = Arrays.asList(line.replaceAll("\\s{2,}", ",").split(","));
            if (tempList.size() <= 4 || StringUtils.isBlank(tempList.get(1))) {
                log.warn("dockerStatsCmd 输出格式异常，line={}", line);
                continue;
            }
            dockerStatsMap.put(tempList.get(1), tempList);
        }
        //log.info("dockerStatsCmd 解析完成，containerCount={}", dockerStatsMap.size());
        return dockerStatsMap;
    }

    public static HashMap<String, Long> dockerDiskIOCmd(String[] cmd) {
        //log.info("dockerDiskIOCmd 开始执行命令，cmd={}", Arrays.toString(cmd));
        HashMap<String, Long> dockerDiskIOMap = defaultDockerDiskIOMap();
        CommandResult result = executeCommand(cmd, DEFAULT_TIMEOUT_MS);
        logWhenFailed("dockerDiskIOCmd", result);

        String firstLine = firstNonBlankLine(result.stdoutLines);
        if (StringUtils.isBlank(firstLine)) {
            //log.info("dockerDiskIOCmd 未读取到有效输出，返回默认值 read=0, write=0");
            return dockerDiskIOMap;
        }

        String[] tokens = firstLine.trim().split("\\s+");
        if (tokens.length <= 7) {
            log.warn("dockerDiskIOCmd 输出字段不足，line={}", firstLine);
            return dockerDiskIOMap;
        }

        Long readValue = parseLongSafely(tokens[3], "read", firstLine);
        Long writeValue = parseLongSafely(tokens[7], "write", firstLine);
        if (readValue != null) {
            dockerDiskIOMap.put("read", readValue);
        }
        if (writeValue != null) {
            dockerDiskIOMap.put("write", writeValue);
        }
        //log.info("dockerDiskIOCmd 解析完成，read={}, write={}", dockerDiskIOMap.get("read"), dockerDiskIOMap.get("write"));
        return dockerDiskIOMap;
    }

    private static CommandResult executeCommand(String[] cmd, long timeoutMs) {
        String commandText = Arrays.toString(cmd);
        if (isInvalidCommand(cmd)) {
            log.error("命令参数非法，command={}", commandText);
            return new CommandResult(cmd, -1, false, Collections.<String>emptyList(),
                    Collections.singletonList("invalid command"), 0L, timeoutMs, new IllegalArgumentException("invalid command"));
        }

        long start = System.currentTimeMillis();
        Process process = null;
        ExecutorService executor = STREAM_READER_EXECUTOR;
        //log.info("开始执行系统命令，command={}, timeoutMs={}", commandText, timeoutMs);
        try {
            process = new ProcessBuilder(cmd).start();
            final Process currentProcess = process;
            Future<List<String>> stdoutFuture = executor.submit(new StreamReader(currentProcess.getInputStream()));
            Future<List<String>> stderrFuture = executor.submit(new StreamReader(currentProcess.getErrorStream()));

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                if (!process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS);
                }
            }

            List<String> stdoutLines = getFutureLines(stdoutFuture, "stdout", cmd);
            List<String> stderrLines = getFutureLines(stderrFuture, "stderr", cmd);
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
            if (process != null) {
                process.destroyForcibly();
            }
            return new CommandResult(cmd, -1, false, Collections.<String>emptyList(),
                    Collections.singletonList("command interrupted: " + e.getMessage()), System.currentTimeMillis() - start, timeoutMs, e);
        } finally {
            // 全局线程池 STREAM_READER_EXECUTOR 的生命周期由 shutdownHook 管理（见第43-50行），
            // 不应在此处 shutdown，否则第一次命令执行后线程池即废，后续命令全部失败。
            if (process != null) {
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
                closeQuietly(process.getOutputStream());
                process.destroy();
            }
        }
    }


    private static ExecutorService createStreamReaderExecutor() {
        return Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "command-util-stream-" + STREAM_READER_THREAD_INDEX.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private static void shutdownStreamReaderExecutor() {
        STREAM_READER_EXECUTOR.shutdown();
        try {
            if (!STREAM_READER_EXECUTOR.awaitTermination(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)) {
                STREAM_READER_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            STREAM_READER_EXECUTOR.shutdownNow();
        }
    }
    private static List<String> getFutureLines(Future<List<String>> future, String streamName, String[] cmd) {
        try {
            return future.get(STREAM_READ_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            // 流读取失败意味着命令输出数据丢失，升级为 ERROR 便于运维发现
            log.error("读取{}被中断，command={}", streamName, Arrays.toString(cmd));
        } catch (ExecutionException e) {
            future.cancel(true);
            log.error("读取{}失败，command={}, error={}", streamName, Arrays.toString(cmd), e.getMessage());
        } catch (TimeoutException e) {
            future.cancel(true);
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

    private static String firstNonBlankLine(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        for (String line : lines) {
            if (StringUtils.isNotBlank(line)) {
                return line;
            }
        }
        return "";
    }

    private static Long parseLongSafely(String value, String fieldName, String line) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("dockerDiskIOCmd 解析{}失败，value={}, line={}", fieldName, value, line);
            return null;
        }
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

    private static HashMap<String, Long> defaultDockerDiskIOMap() {
        HashMap<String, Long> dockerDiskIOMap = new HashMap<String, Long>();
        dockerDiskIOMap.put("read", DEFAULT_DISK_IO_VALUE);
        dockerDiskIOMap.put("write", DEFAULT_DISK_IO_VALUE);
        return dockerDiskIOMap;
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



