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

    protected final static int threadsPerFile = 256;

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
                        .name(String.format("file-executor-%s", fileExecutorCount.getAndIncrement()))
                        .factory()
        )) {
            for (var file : files) {
                executor.submit(() -> {
                    var hashResult = this.processFile(file);
                    processResult.add(hashResult);
                });
            }
        }

        return processResult;
    }

    protected ProcessFileResult processFile(final File file) {
        var startAt = LocalDateTime.now();

        try {
            log.info("Start hash processing of file {}", file.getAbsolutePath());

            var concurrentHashResult = new ConcurrentLinkedQueue<ProcessHashResult>();

            try (var inputStream = Files.newInputStream(file.toPath())) {
                var maxBytesAlloc = oneMegaBytesInBytes * 10;

                if (maxBytesAlloc > inputStream.available()) {
                    maxBytesAlloc = inputStream.available();
                }
                var bytesBlocks = new ArrayList<byte[]>(threadsPerFile);
                bytesBlocks.add(new byte[maxBytesAlloc]);

                var partsProcessed = new AtomicInteger();
                var concurrentExec = new AtomicInteger();

                while (inputStream.available() > 0) {
                    final var actualExec = concurrentExec.getAndIncrement();
                    inputStream.readNBytes(bytesBlocks.get(actualExec), 0, maxBytesAlloc);

                    if (actualExec == (threadsPerFile - 1) || (inputStream.available() == 0 && concurrentExec.get() > 0)) {
                        try (var executor = Executors.newThreadPerTaskExecutor(
                                Thread.ofVirtual()
                                        .name(String.format("hash-executor-%s", hashExecutorCount.getAndIncrement()))
                                        .factory()
                        )) {
                            for (var i = 0; i < concurrentExec.get(); i++) {
                                final var lambdaPartProcessed = partsProcessed.incrementAndGet();
                                executor.submit(() -> {
                                    log.debug("Start processing part {}", lambdaPartProcessed + 1);
                                    var hash = getSHA256Hash(bytesBlocks.get(actualExec));
                                    log.debug("Finish processing part {}", lambdaPartProcessed + 1);
                                    concurrentHashResult.add(new ProcessHashResult(lambdaPartProcessed, hash));
                                });
                            }
                        }
                        bytesBlocks.forEach(b -> Arrays.fill(b, (byte) 0));
                        concurrentExec.set(0);
                    }

                    if (bytesBlocks.size() < threadsPerFile) {
                        bytesBlocks.add(new byte[maxBytesAlloc]);
                    }
                }
            }

            log.debug("Consolidating hash: {}", file.getName());
            var intermediaryHash = concurrentHashResult.stream()
                    .sorted(Comparator.comparingInt(ProcessHashResult::position))
                    .map(ProcessHashResult::hash)
                    .collect(Collectors.joining());

            var consolidateHash = this.getSHA256Hash(intermediaryHash.getBytes(StandardCharsets.UTF_8));

            log.info("The hash processing of file {} has been completed with success", file.getAbsolutePath());
            return new ProcessFileResult(
                    true,
                    file,
                    consolidateHash,
                    Duration.between(startAt, LocalDateTime.now()).getSeconds()
            );
        } catch (Exception ex) {
            log.error("The hash processing of file {} has been completed with failed", file.getAbsolutePath(), ex);
            return new ProcessFileResult(
                    false,
                    file,
                    ex.getMessage(),
                    Duration.between(startAt, LocalDateTime.now()).getSeconds()
            );
        }
    }

    protected String getSHA256Hash(final byte[] bytes) {
        return DigestUtils.sha256Hex(bytes);
    }

}
