package edu.cmu.cs.cloud.aws.model;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EC2Manager {

    /**
     * Create an EC2 instance with user-defined options.
     *
     * @param ec2    EC2 client
     * @param config Configuration properties
     */
    public static void createEC2Instance(Ec2Client ec2, Properties config) {
        // Step 1: Get Instance Name
        String instanceName = InputManager.getInput("Enter the name of the instance: ");
        if (instanceName.isEmpty()) {
            instanceName = config.getProperty("default.ec2.instance.name");
        }
        String keyPairName = InputManager.getInput("Enter the key-pair name: ");

        // Step 2: List existing security groups
        List<String> securityGroupIds = listSecurityGroups(ec2);

        int choice = InputManager.getIntegerInput("Select a security group (or choose last option to create a new group): ");
        String selectedSecurityGroupId;

        if (choice == securityGroupIds.size() + 1) {
            // User opted to create a new security group
            selectedSecurityGroupId = SecurityGroupManager.createSecurityGroup(ec2, config);
        } else if (choice >= 1 && choice <= securityGroupIds.size()) {
            // User selected an existing security group
            selectedSecurityGroupId = securityGroupIds.get(choice - 1);
        } else {
            System.out.println("Invalid selection. Exiting EC2 creation process.");
            return;
        }

        // Step 3: Set tags for the EC2 instance
        List<TagSpecification> tagSpecifications = TagManager.getTagSpecifications("instance");
        List<Tag> updatedTags = new ArrayList<>(tagSpecifications.get(0).tags());
        updatedTags.add(Tag.builder().key("Name").value(instanceName).build());

        TagSpecification updatedTagSpecification = TagSpecification.builder()
                .resourceType("instance")
                .tags(updatedTags)
                .build();

        tagSpecifications.set(0, updatedTagSpecification);

        // Step 4: Set instance market options
        boolean useSpotPricing = InputManager.getInput("Opt for spot pricing (y/n): ").trim().equalsIgnoreCase("y");
        InstanceMarketOptionsRequest marketOptions = getInstanceMarketOptions(useSpotPricing, config);

        // Step 5: Create the EC2 instance
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(config.getProperty("ami.id"))
                .instanceType(InstanceType.fromValue(config.getProperty("instance.type")))
                .keyName(keyPairName)
                .maxCount(1)
                .minCount(1)
                .securityGroupIds(selectedSecurityGroupId)
                .tagSpecifications(tagSpecifications)
                .instanceMarketOptions(marketOptions)
                .build();

        try {
            RunInstancesResponse response = ec2.runInstances(runRequest);
            String instanceId = response.instances().get(0).instanceId();
            System.out.println("EC2 Instance creation initiated.");

            // Dynamically poll for instance running status
            boolean isRunning = false;
            while (!isRunning) {
                DescribeInstancesRequest describeRequest = DescribeInstancesRequest.builder()
                        .instanceIds(instanceId)
                        .build();
                DescribeInstancesResponse describeResponse = ec2.describeInstances(describeRequest);

                InstanceStateName state = describeResponse.reservations().get(0).instances().get(0).state().name();
                if (state == InstanceStateName.RUNNING) {
                    isRunning = true;
                    System.out.println("Instance is now running.");
                }
            }

            // Load instance details after it is running
            System.out.println("Instance Name: " + instanceName);
            System.out.println("Instance ID: " + instanceId);

        } catch (Ec2Exception e) {
            System.err.println("Failed to launch EC2 instance: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Sets the instance market options based on user input.
     * @param useSpotPricing Boolean to determine if spot pricing should be used
     * @param config Configuration properties2
     * @return InstanceMarketOptionsRequest object
     */
    private static InstanceMarketOptionsRequest getInstanceMarketOptions(boolean useSpotPricing, Properties config) {
        if (useSpotPricing) {
            String maxPriceInput = InputManager.getInput("Set max price ($) per hour (default " + config.getProperty("spot.max.price", "0.05") + "): ");
            String maxPrice = maxPriceInput.isEmpty() ? config.getProperty("spot.max.price", "0.05") : maxPriceInput;

            SpotMarketOptions spotOptions = SpotMarketOptions.builder()
                    .maxPrice(maxPrice)
                    .spotInstanceType(SpotInstanceType.ONE_TIME)
                    .build();
            return InstanceMarketOptionsRequest.builder()
                    .marketType(MarketType.SPOT)
                    .spotOptions(spotOptions)
                    .build();
        }
        return null; // Null means On-Demand pricing
    }

    /**
     * Lists existing security groups and returns their IDs.
     *
     * @param ec2 EC2 client
     * @return List of security group IDs
     */
    private static List<String> listSecurityGroups(Ec2Client ec2) {
        DescribeSecurityGroupsResponse response = ec2.describeSecurityGroups();

        List<String> securityGroupIds = new ArrayList<>();
        System.out.println("Select a security group or create a new one:");

        int index = 1;
        for (SecurityGroup sg : response.securityGroups()) {
            System.out.println(index + ". " + sg.groupName() + " (ID: " + sg.groupId() + ")");
            securityGroupIds.add(sg.groupId());
            index++;
        }

        System.out.println(index + ". Create new security group");

        return securityGroupIds;
    }

    /**
     * Deletes an EC2 instance.
     *
     * @param ec2 EC2 client
     */
    public static void deleteEC2Instance(Ec2Client ec2) {
        String instanceId = InputManager.getInput("Enter EC2 Instance ID to terminate: ");
        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        try {
            ec2.terminateInstances(terminateRequest);
            System.out.println("EC2 Instance terminated: " + instanceId);
        } catch (Ec2Exception e) {
            System.err.println("Failed to terminate EC2 instance: " + e.awsErrorDetails().errorMessage());
        }
    }
}
