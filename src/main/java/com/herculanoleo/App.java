package com.herculanoleo;

import com.herculanoleo.entrypoint.FileEntrypoint;
import com.herculanoleo.entrypoint.ShapeEntrypoint;
import lombok.extern.log4j.Log4j2;

import java.util.Scanner;

@Log4j2
public class App {

    public static void main(String[] args) {
        entrypoint();
    }

    public static void entrypoint() {
        var scanner = new Scanner(System.in);
        var selectedOption = -1;

        while (selectedOption != 0) {

            System.out.println("Select the your desire mode");
            System.out.println("1 - Shape");
            System.out.println("2 - File");
            System.out.println("0 - Exit");

            try {
                selectedOption = scanner.nextInt();

                switch (selectedOption) {
                    case 1 -> ShapeEntrypoint.shared.start();
                    case 2 -> FileEntrypoint.shared.start();
                    case 0 -> System.out.println("The program has been terminated!");
                    default -> throw new RuntimeException("Invalid option");
                }
            } catch (Exception ex) {
                System.out.println("Please enter with a valid option");
                log.error("An error occurred", ex);
            }
        }
    }
}
