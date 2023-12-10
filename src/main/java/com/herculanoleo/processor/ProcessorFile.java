package com.herculanoleo.processor;

import com.herculanoleo.models.exception.ProcessFileException;
import com.herculanoleo.models.file.ProcessFileResult;
import com.herculanoleo.models.file.ProcessHashResult;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/*
    VirtualThreads
*/
@Log4j2
public class ProcessorFile {

    public static final ProcessorFile shared = new ProcessorFile();

    protected static final FileFilter isDirectory = FileFilterUtils.directoryFileFilter();

    protected static final FileFilter isFile = FileFilterUtils.fileFileFilter();

    protected static final int THREADS_PER_FILE = 2;

    protected static final int BYTES_IN_ONE_MEGABYTE = 1048576;

    protected static final int MEGABYTES_IN_BYTES_BLOCK = 10;

    protected static final String FILE_EXECUTOR_NAME_FORMAT = "file-executor-%s";

    protected static final String HASH_EXECUTOR_NAME_FORMAT = "hash-executor-%s";

    protected static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    protected final static Semaphore fileSemaphore = new Semaphore(AVAILABLE_PROCESSORS * 128);

    protected final static Semaphore hashSemaphore = new Semaphore(AVAILABLE_PROCESSORS * 1024);
    protected static final AtomicInteger fileExecutorCount = new AtomicInteger();

    protected static final AtomicInteger hashExecutorCount = new AtomicInteger();

    protected static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public void processFolder(final File directory, final File resultDestination) {
        var startAt = LocalDateTime.now();
        try {
            if (directory.isDirectory() && resultDestination.isDirectory()) {
                var filepath = Path.of(resultDestination.getPath(), String.format("result-%s.txt", dateTimeFormatter.format(LocalDateTime.now())));

                log.info("Start processing directory: {}", directory.getAbsolutePath());
                log.info("The result of processing directory will save to: {}", filepath.toString());

                var files = listFile(directory, isFile);

                var results = processFiles(files);

                var data = results.stream()
                        .map(ProcessFileResult::toString)
                        .collect(Collectors.joining("\n"));

                try {
                    FileUtils.writeStringToFile(filepath.toFile(), data, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    log.error("An error occurred while write the result file", ex);
                    throw new ProcessFileException();
                }

                log.info("Finish processing directory: {}", directory.getAbsolutePath());
            } else {
                throw new ProcessFileException();
            }
        } finally {
            fileExecutorCount.set(0);
            hashExecutorCount.set(0);
            log.info("The total processing time was {}s", calculateDurationInSeconds(startAt));
            System.gc();
        }
    }

    protected List<File> listFile(final File directory, final FileFilter fileFilter) {
        var result = new LinkedList<File>();

        var files = directory.listFiles(fileFilter);

        if (null != files) {
            result.addAll(Arrays.asList(files));
        }

        var subDirectories = directory.listFiles(isDirectory);

        if (null != subDirectories) {
            for (var sub : subDirectories) {
                result.addAll(listFile(sub, fileFilter));
            }
        }

        return result;
    }

    protected Collection<ProcessFileResult> processFiles(final Collection<File> files) {
        var processResult = new ConcurrentLinkedQueue<ProcessFileResult>();

        try (var executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name(String.format(FILE_EXECUTOR_NAME_FORMAT, fileExecutorCount.getAndIncrement()))
                        .factory()
        )) {
            for (var file : files) {
                executor.submit(() -> {
                    try {
                        fileSemaphore.acquire();
                        var hashResult = this.processFile(file);
                        processResult.add(hashResult);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        fileSemaphore.release();
                    }
                });
            }
        }

        return processResult;
    }

    protected ProcessFileResult processFile(final File file) {
        var startAt = LocalDateTime.now();

        if (Objects.isNull(file)) {
            return new ProcessFileResult(false, null, "File not found", calculateDurationInSeconds(startAt));
        }

        try {
            log.info("Start hash processing of file {}", file.getAbsolutePath());

            var concurrentHashResult = concurrentProcessFileHash(file);

            var consolidateHash = consolidateHash(file, concurrentHashResult);

            log.info("The hash processing of file {} has been completed with success", file.getAbsolutePath());
            return new ProcessFileResult(
                    true,
                    file,
                    consolidateHash,
                    calculateDurationInSeconds(startAt)
            );
        } catch (Throwable ex) {
            log.error("The hash processing of file {} has been completed with failed", file.getAbsolutePath(), ex);
            return new ProcessFileResult(
                    false,
                    file,
                    ex.getMessage(),
                    calculateDurationInSeconds(startAt)
            );
        }
    }

    protected Collection<ProcessHashResult> concurrentProcessFileHash(final File file) throws IOException {
        try (var inputStream = Files.newInputStream(file.toPath())) {
            var concurrentHashResult = new ConcurrentLinkedQueue<ProcessHashResult>();

            var maxBytesAlloc = Math.min(BYTES_IN_ONE_MEGABYTE * MEGABYTES_IN_BYTES_BLOCK, inputStream.available());

            var bytesBlocks = new ArrayList<byte[]>(THREADS_PER_FILE);
            bytesBlocks.add(new byte[maxBytesAlloc]);

            var concurrentExec = new AtomicInteger();

            while (inputStream.available() > 0) {
                final var actualExec = concurrentExec.getAndIncrement();
                inputStream.readNBytes(bytesBlocks.get(actualExec), 0, maxBytesAlloc);

                if (actualExec == (THREADS_PER_FILE - 1) || (inputStream.available() == 0 && concurrentExec.get() > 0)) {
                    processFileBlock(bytesBlocks, concurrentExec, concurrentHashResult);
                    bytesBlocks.forEach(b -> Arrays.fill(b, (byte) 0));
                    concurrentExec.set(0);
                }

                if (bytesBlocks.size() < THREADS_PER_FILE) {
                    bytesBlocks.add(new byte[maxBytesAlloc]);
                }
            }

            return concurrentHashResult;
        }
    }

    protected void processFileBlock(List<byte[]> bytesBlocks, AtomicInteger concurrentExec, ConcurrentLinkedQueue<ProcessHashResult> concurrentHashResult) {
        try (var executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name(String.format(HASH_EXECUTOR_NAME_FORMAT, hashExecutorCount.getAndIncrement()))
                        .factory()
        )) {
            for (var i = 0; i < concurrentExec.get(); i++) {
                final int it = i;
                executor.submit(() -> {
                    try {
                        hashSemaphore.acquire();
                        log.debug("Start processing part {}", it + 1);
                        var hash = getSHA256Hash(bytesBlocks.get(it));
                        log.debug("Finish processing part {}", it + 1);
                        concurrentHashResult.add(new ProcessHashResult(it, hash));
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        hashSemaphore.release();
                    }
                });
            }
        }
    }

    protected String consolidateHash(File file, Collection<ProcessHashResult> concurrentHashResult) {
        log.debug("Consolidating hash: {}", file.getName());
        var intermediaryHash = concurrentHashResult.stream()
                .sorted(Comparator.comparingInt(ProcessHashResult::position))
                .map(ProcessHashResult::hash)
                .collect(Collectors.joining());

        return this.getSHA256Hash(intermediaryHash.getBytes(StandardCharsets.UTF_8));
    }

    protected String getSHA256Hash(final byte[] bytes) {
        return DigestUtils.sha256Hex(bytes);
    }

    protected Long calculateDurationInSeconds(LocalDateTime startAt) {
        return Duration.between(startAt, LocalDateTime.now()).getSeconds();
    }

}
