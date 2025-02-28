package edu.cmu.cs.cloud.aws.controller;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.io.IOException;
import java.util.Properties;

import edu.cmu.cs.cloud.aws.model.AWSConfigLoader;
import edu.cmu.cs.cloud.aws.model.EC2Manager;
import edu.cmu.cs.cloud.aws.model.InputManager;
import edu.cmu.cs.cloud.aws.model.SecurityGroupManager;
import edu.cmu.cs.cloud.aws.model.SSHTunnelManager;
import edu.cmu.cs.cloud.aws.model.DockerManager;

public class MainMenu {
    private static Properties config;
    private static Ec2Client ec2Client;

    public static void main(String[] args) {
        config = AWSConfigLoader.loadConfig();
        initializeEC2Client();
        displayActionMenu();
    }

    private static void initializeEC2Client() {
        ec2Client = Ec2Client.builder()
                .region(Region.of(config.getProperty("aws.region")))
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

    private static void displayActionMenu() {
        while (true) {
            System.out.println("\nWhat action do you wish to take:");
            System.out.println("1. Create a security group");
            System.out.println("2. Create an EC2 instance");
            System.out.println("3. Delete an EC2 instance");
            System.out.println("4. Delete a security group");
            System.out.println("5. SSH into EC2 instance");
            System.out.println("6. Build Docker Image");
            System.out.println("7. Quit action menu go back to terminal");

            String choice = InputManager.getInput("Select an option: ");

            switch (choice) {
                case "1":
                    SecurityGroupManager.createSecurityGroup(ec2Client, config);
                    break;
                case "2":
                    EC2Manager.createEC2Instance(ec2Client, config);
                    break;
                case "3":
                    EC2Manager.deleteEC2Instance(ec2Client);
                    break;
                case "4":
                    SecurityGroupManager.deleteSecurityGroup(ec2Client);
                    break;
                case "5":
                    SSHTunnelManager.sshTunnelInto(ec2Client, config);
                    break;
                case "6":
                    try {
                        DockerManager.buildDockerImage(config);
                    } catch (IOException | InterruptedException e) {
                        System.out.println("Error occurred while building Docker image: " + e.getMessage());
                    }
                    break;
                case "7":
                    System.out.println("Exiting... Goodbye!");
                    ec2Client.close();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
}
