package edu.cmu.cs.cloud.aws.model;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DockerManager {

    public static void buildDockerImage(Properties config) throws IOException, InterruptedException {
        // Check if Docker is running
        if (!isDockerRunning()) {
            System.out.println("Docker is not running. Please start Docker and try again.");
            return;
        }

        // Prompt user for root directory
        String rootDirectory = InputManager.getInput("Enter the path of the application root directory (Dockerfile should be inside docker/): ").trim();
        File dockerfile = new File(rootDirectory, "docker/Dockerfile");

        // Validate the Dockerfile exists
        if (!dockerfile.exists()) {
            System.out.println("Error: No Dockerfile found in " + dockerfile.getAbsolutePath());
            return;
        }

        // Choose platform (local or linux/amd64)
        String platformChoice = InputManager.getInput("Do you want to build for local machine or linux/amd64? (local/linux): ").trim();
        String platformOption = platformChoice.equalsIgnoreCase("linux") ? "--platform linux/amd64" : "";

        // Get tag identifier (default from config or user input)
        String defaultTagIdentifier = config.getProperty("docker.tag.identifier", "twitter-phase-1/container");
        String tagIdentifier = InputManager.getInput("Enter tag identifier or press enter to use default [" + defaultTagIdentifier + "]: ").trim();
        if (tagIdentifier.isEmpty()) {
            tagIdentifier = defaultTagIdentifier;
        }

        // Get tag name
        String tagName = InputManager.getInput("Enter the tag name (e.g., latest, v1.0): ").trim();

        // Construct and execute the Docker build command
        String dockerCommand = String.format(
                "docker buildx build %s -f %s --rm --tag %s:%s %s",
                platformOption, dockerfile.getAbsolutePath(), tagIdentifier, tagName, rootDirectory
        );

        System.out.println("Executing: " + dockerCommand);
        executeCommand(dockerCommand);
    }

    private static boolean isDockerRunning() {
        try {
            Process process = new ProcessBuilder("docker", "info").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static void executeCommand(String command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("bash", "-c", command)
                .inheritIO()
                .start();
        process.waitFor();
    }
}
