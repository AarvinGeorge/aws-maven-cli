package edu.cmu.cs.cloud.aws.model;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.Properties;

public class SecurityGroupManager {

    /**
     * Creates a new security group and returns its ID.
     *
     * @param ec2    EC2 client instance.
     * @param config Configuration properties.
     * @return Security Group ID or null if creation fails.
     */
    public static String createSecurityGroup(Ec2Client ec2, Properties config) {
        String sgName = InputManager.getInput("Name of the security group: ");
        if (sgName.isEmpty()) {
            sgName = config.getProperty("default.security.group.name");
        }

        // User input for inbound rules
        String httpInput = InputManager.getInput("Would you like to open inbound HTTP port 80? (y/n): ");
        String sshInput = InputManager.getInput("Would you like to open inbound SSH port 22? (y/n): ");

        // Build security group creation request
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .groupName(sgName)
                .description("Created via Java AWS SDK")
                .build();

        try {
            // Create security group and retrieve group ID
            CreateSecurityGroupResponse response = ec2.createSecurityGroup(request);
            String groupId = response.groupId();

            // Add inbound rules based on user input
            if (httpInput.equalsIgnoreCase("y") || httpInput.isEmpty()) {
                addInboundRule(ec2, groupId, 80);
            }
            if (sshInput.equalsIgnoreCase("y") || sshInput.isEmpty()) {
                addInboundRule(ec2, groupId, 22);
            }

            // Apply tags to the security group
            List<TagSpecification> tagSpecifications = TagManager.getTagSpecifications("security-group");
            CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                    .resources(groupId)
                    .tags(tagSpecifications.get(0).tags())
                    .build();
            ec2.createTags(tagRequest);

            // Success message
            System.out.println("Security Group Created: " + sgName + ", ID: " + groupId);
            return groupId; // Return the created Security Group ID

        } catch (Ec2Exception e) {
            System.err.println("Failed to create security group: " + e.awsErrorDetails().errorMessage());
            return null; // Return null if creation fails
        }
    }

    /**
     * Adds inbound rules to a security group for a specified port.
     *
     * @param ec2     EC2 client instance.
     * @param groupId Security group ID.
     * @param port    Port number to allow inbound traffic.
     */
    private static void addInboundRule(Ec2Client ec2, String groupId, int port) {
        AuthorizeSecurityGroupIngressRequest ingressRequest = AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(groupId)
                .ipPermissions(IpPermission.builder()
                        .ipProtocol("tcp")
                        .fromPort(port)
                        .toPort(port)
                        .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                        .build())
                .build();
        ec2.authorizeSecurityGroupIngress(ingressRequest);
    }

    /**
     * Deletes a security group by its ID.
     *
     * @param ec2 EC2 client instance.
     */
    public static void deleteSecurityGroup(Ec2Client ec2) {
        String sgId = InputManager.getInput("Enter Security Group ID to delete: ");
        try {
            DeleteSecurityGroupRequest deleteRequest = DeleteSecurityGroupRequest.builder()
                    .groupId(sgId)
                    .build();
            ec2.deleteSecurityGroup(deleteRequest);
            System.out.println("Security Group deleted: " + sgId);
        } catch (Ec2Exception e) {
            System.err.println("Error deleting Security Group: " + e.awsErrorDetails().errorMessage());
        }
    }
}
