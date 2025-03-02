package edu.cmu.cs.cloud.aws.model;

import java.util.Scanner;

public class InputManager {
    private static final Scanner scanner = new Scanner(System.in);

    static {
        // Ensure scanner closes when the application shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (scanner != null) {
                scanner.close();
                System.out.println("Scanner closed successfully.");
            }
        }));
    }

    private InputManager() {
        // Prevent instantiation
    }

    public static String getInput(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return sanitizeInput(input);
    }

    public static int getIntegerInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(sanitizeInput(scanner.nextLine().trim()));
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    /**
     * Removes leading and trailing single/double quotes from the input.
     */
    private static String sanitizeInput(String input) {
        if (input.length() > 1) {
            if ((input.startsWith("'") && input.endsWith("'")) || 
                (input.startsWith("\"") && input.endsWith("\""))) {
                return input.substring(1, input.length() - 1);
            }
        }
        return input;
    }
}
