package edu.cmu.cs.cloud.aws.model;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

public class SSHTunnelManager {

    public static void sshTunnelInto(Ec2Client ec2, Properties config) {
        // Step 1: List running EC2 instances
        List<Instance> runningInstances = listRunningInstances(ec2);
        if (runningInstances.isEmpty()) {
            System.out.println("No running EC2 instances found.");
            return;
        }

        // Step 2: Display running instances for selection
        System.out.println("Select an EC2 instance to SSH into:");
        IntStream.range(0, runningInstances.size()).forEach(i -> {
            Instance instance = runningInstances.get(i);
            String name = instance.tags().stream()
                    .filter(tag -> tag.key().equalsIgnoreCase("Name"))
                    .map(Tag::value)
                    .findFirst()
                    .orElse("Unnamed Instance");
            System.out.println((i + 1) + ". " + name + " (ID: " + instance.instanceId() + ")");
        });
        System.out.println((runningInstances.size() + 1) + ". Exit SSH menu");

        int choice = InputManager.getIntegerInput("Select an instance: ");
        if (choice == runningInstances.size() + 1 || choice <= 0 || choice > runningInstances.size()) {
            System.out.println("Exiting SSH menu.");
            return;
        }

        // Step 3: Retrieve instance details
        Instance selectedInstance = runningInstances.get(choice - 1);
        String publicDns = selectedInstance.publicDnsName();

        // Step 4: Get SSH key-pair path using InputManager
        String keyPairPath = System.getenv("KEY_PAIR_PATH");
        while (keyPairPath == null || keyPairPath.isEmpty()) {
            keyPairPath = InputManager.getInput("Provide absolute path to your key-pair (.pem): ");
        }

        // Step 5: Get SSH username using InputManager
        String sshUsername = InputManager.getInput("Enter SSH username (default: " + config.getProperty("default.ssh.username") + "): ");
        if (sshUsername.isEmpty()) {
            sshUsername = config.getProperty("default.ssh.username");
        }

        // Step 6: Execute SSH command using ProcessBuilder
        executeSSHCommand(keyPairPath, sshUsername, publicDns);
    }

    /**
     * Lists all running EC2 instances.
     *
     * @param ec2 EC2 client instance
     * @return List of running instances
     */
    private static List<Instance> listRunningInstances(Ec2Client ec2) {
        DescribeInstancesResponse response = ec2.describeInstances();

        return response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .filter(instance -> instance.state().name() == InstanceStateName.RUNNING)
                .toList();
    }

    /**
     * Executes the SSH command using ProcessBuilder and InputManager.
     *
     * @param keyPairPath Path to the PEM key-pair file
     * @param username    SSH username
     * @param publicDns   Public DNS of the EC2 instance
     */
    private static void executeSSHCommand(String keyPairPath, String username, String publicDns) {
        String command = String.format("ssh -i %s %s@%s", keyPairPath, username, publicDns);
        System.out.println("Executing SSH command: " + command);

        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.inheritIO(); // Directly attach to the console for real-time SSH session

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("SSH session ended successfully.");
            } else {
                System.err.println("SSH connection failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to establish SSH connection: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
