package edu.cmu.cs.cloud.aws.model;

import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

import java.util.ArrayList;
import java.util.List;
// import java.util.Properties;

public class TagManager {

    /**
     * Prompts the user to input tag key-value pairs with default values from config.
     *
     * @param resourceType The type of AWS resource (e.g., \"instance\", \"security-group\")
     * @return A list of TagSpecifications for the resource
     */
    public static List<TagSpecification> getTagSpecifications(String resourceType) {
        List<Tag> tags = new ArrayList<>();
        boolean addMoreTags = true;

        String defaultKey = AWSConfigLoader.getDefaultTagKey();
        String defaultValue = AWSConfigLoader.getDefaultTagValue();

        System.out.println("Enter tag specifications for the " + resourceType + ":");
        System.out.println("Press [Enter] for default key-value pair: " + defaultKey + "=" + defaultValue);

        while (addMoreTags) {
            String key = InputManager.getInput("Tag Key (default: " + defaultKey + "): ");
            if (key.isEmpty()) {
                key = defaultKey;
            }

            String value = InputManager.getInput("Tag Value (default: " + defaultValue + "): ");
            if (value.isEmpty()) {
                value = defaultValue;
            }

            tags.add(Tag.builder().key(key).value(value).build());

            String continueInput = InputManager.getInput("Add another tag? (y/n): ");
            if (!continueInput.equalsIgnoreCase("y") && !continueInput.isEmpty()) {
                addMoreTags = false;
            }
        }

        if (tags.isEmpty()) {
            System.out.println("No tags specified.");
        }

        List<TagSpecification> tagSpecifications = new ArrayList<>();
        tagSpecifications.add(TagSpecification.builder()
                .resourceType(resourceType)
                .tags(tags)
                .build());

        return tagSpecifications;
    }
}
