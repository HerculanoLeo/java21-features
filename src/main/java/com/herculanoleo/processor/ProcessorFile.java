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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/*
    VirtualThreads
*/
@Log4j2
public class ProcessorFile {

    public static final ProcessorFile shared = new ProcessorFile();

    protected final FileFilter isDirectory = FileFilterUtils.directoryFileFilter();

    protected final FileFilter isFile = FileFilterUtils.fileFileFilter();

    protected final static int threadsPerFile = 8;

    protected final static int oneMegaBytesInBytes = 1048576;

    protected final static AtomicInteger fileExecutorCount = new AtomicInteger();

    protected final static AtomicInteger hashExecutorCount = new AtomicInteger();

    protected final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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
            log.info("The total processing time was {}s", Duration.between(startAt, LocalDateTime.now()).getSeconds());
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
        var processResult = new Vector<ProcessFileResult>(files.size());

        try (var executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name(String.format("file-executor-%s", fileExecutorCount.getAndIncrement()))
                        .factory()
        )) {
            var futures = new ArrayList<Future<Boolean>>();

            for (var file : files) {
                log.info("Start processing of file: {}", file.getAbsolutePath());
                var future = executor.submit(() -> this.processFile(file).thenApply((processResult::add)).get());
                futures.add(future);
            }

            for (var future : futures) {
                try {
                    future.get();
                } catch (Exception ex) {
                    log.error("An error occurred while processing the files", ex);
                    throw new ProcessFileException();
                }
            }
        }

        return processResult;
    }

    protected CompletableFuture<ProcessFileResult> processFile(final File file) {
        var startAt = LocalDateTime.now();

        try (var executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name(String.format("hash-executor-%s", hashExecutorCount.getAndIncrement()))
                        .factory()
        )) {
            var completionService = new ExecutorCompletionService<ProcessHashResult>(executor);
            log.info("Start hash processing of file {}", file.getAbsolutePath());

            try (var inputStream = Files.newInputStream(file.toPath())) {
                var hashResult = new LinkedList<ProcessHashResult>();

                var maxBytesAlloc = oneMegaBytesInBytes * 10;

                if (maxBytesAlloc > inputStream.available()) {
                    maxBytesAlloc = inputStream.available();
                }
                var bytesBlocks = new ArrayList<byte[]>(threadsPerFile);
                bytesBlocks.add(new byte[maxBytesAlloc]);

                var partsProcessed = new AtomicInteger();
                var concurrentExec = new AtomicInteger();

                do {
                    final var actualExec = concurrentExec.getAndIncrement();

                    inputStream.readNBytes(bytesBlocks.get(actualExec), 0, maxBytesAlloc);

                    final var lambdaPartProcessed = partsProcessed.incrementAndGet();
                    completionService.submit(() -> getSHA256Hash(lambdaPartProcessed, bytesBlocks.get(actualExec)));

                    if (bytesBlocks.size() < threadsPerFile) {
                        bytesBlocks.add(new byte[maxBytesAlloc]);
                    }

                    if (actualExec == (threadsPerFile - 1)) {
                        for (int i = 0; i <= actualExec; i++) {
                            var future = completionService.take();
                            var result = future.get();
                            hashResult.add(result);
                        }

                        bytesBlocks.forEach(b -> Arrays.fill(b, (byte) 0));

                        concurrentExec.set(0);
                    }
                } while (inputStream.available() > 0);

                if (concurrentExec.get() > 0) {
                    for (var i = 0; i < concurrentExec.get(); i++) {
                        var future = completionService.take();
                        var result = future.get();
                        hashResult.add(result);
                    }
                }

                hashResult.sort(Comparator.comparingInt(ProcessHashResult::position));

                var intermediaryHash = hashResult.stream().map(ProcessHashResult::hash).collect(Collectors.joining());

                log.debug("Consolidating hash: {}", file.getName());
                var result = executor.submit(() -> this.getSHA256Hash(0, intermediaryHash.getBytes(StandardCharsets.UTF_8))).get();

                log.info("The hash processing of file {} has been completed with success", file.getAbsolutePath());
                return CompletableFuture.completedFuture(new ProcessFileResult(
                        true,
                        file,
                        result.hash(),
                        Duration.between(startAt, LocalDateTime.now()).getSeconds()
                ));
            }
        } catch (Exception ex) {
            log.error("The hash processing of file {} has been completed with failed", file.getAbsolutePath(), ex);
            return CompletableFuture.completedFuture(new ProcessFileResult(
                    false,
                    file,
                    ex.getMessage(),
                    Duration.between(startAt, LocalDateTime.now()).getSeconds()
            ));
        } finally {
            System.gc();
        }
    }

    protected ProcessHashResult getSHA256Hash(final Integer position, final byte[] bytes) {
        log.debug("Start processing part {}", position + 1);
        var hash = DigestUtils.sha256Hex(bytes);
        log.debug("Finish processing part {}", position + 1);
        return new ProcessHashResult(position, hash);
    }

}
