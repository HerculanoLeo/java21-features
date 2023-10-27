package com.herculanoleo;

import com.herculanoleo.entrypoint.ShapeEntrypoint;

import java.util.Scanner;

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
            System.out.println("0 - Exit");

            try {
                selectedOption = scanner.nextInt();

                switch (selectedOption) {
                    case 1 -> ShapeEntrypoint.shared.start();
                    case 0 -> System.out.println("The program has been terminated!");
                    default -> throw new RuntimeException("Invalid option");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Please enter with a valid option");
            }
        }
    }
}
