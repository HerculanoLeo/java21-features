package com.herculanoleo.entrypoint;

import com.herculanoleo.models.exception.ProcessFileException;
import com.herculanoleo.processor.ProcessorFile;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

@Log4j2
public class FileEntrypoint implements Entrypoint {

    public static final FileEntrypoint shared = new FileEntrypoint();

    protected final static Scanner scanner = new Scanner(System.in);

    @Override
    public void start() {
        try {
            System.out.print("Enter the root directory that files are processing: ");
            var directoryFilesPath = scanner.next();

            if (!Files.isDirectory(Path.of(directoryFilesPath))) {
                throw new ProcessFileException("Please enter a valid root directory");
            }

            System.out.print("Enter the directory that the result of processing will save: ");
            var directoryResultPath = scanner.next();

            if (!Files.isDirectory(Path.of(directoryResultPath))) {
                throw new ProcessFileException("Please enter a valid directory for saving result file");
            }

            ProcessorFile.shared.processFolder(new File(directoryFilesPath), new File(directoryResultPath));
        } catch (ProcessFileException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
