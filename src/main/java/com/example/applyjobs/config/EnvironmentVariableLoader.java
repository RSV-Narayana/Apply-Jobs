package com.example.applyjobs.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentVariableLoader {

    private String linkedInUsername;
    private String linkedInPassword;
    private String diceUsername;
    private String dicePassword;
    private String jobTitle;
    private List<String> jobSkills;
    private int matchingThreshold;
    private String outputDirectory;

    public EnvironmentVariableLoader() {
        loadEnvironmentVariables();
    }

    private void loadEnvironmentVariables() {
        this.linkedInUsername = getEnvironmentVariable("LINKEDIN_USERNAME", "");
        this.linkedInPassword = getEnvironmentVariable("LINKEDIN_PASSWORD", "");
        this.diceUsername = getEnvironmentVariable("DICE_USERNAME", "");
        this.dicePassword = getEnvironmentVariable("DICE_PASSWORD", "");
        this.jobTitle = getEnvironmentVariable("JOB_TITLE", "Full Stack Engineer");

        String skillsEnv = getEnvironmentVariable("JOB_SKILLS", "Java,Spring Boot,React,PostgreSQL");
        this.jobSkills = Arrays.stream(skillsEnv.split(","))
            .map(String::trim)
            .collect(Collectors.toList());

        String threshold = getEnvironmentVariable("JOB_MATCHING_THRESHOLD", "50");
        this.matchingThreshold = Integer.parseInt(threshold);

        this.outputDirectory = getEnvironmentVariable("OUTPUT_DIRECTORY", System.getProperty("user.dir"));
    }

    private String getEnvironmentVariable(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    public String getLinkedInUsername() {
        return linkedInUsername;
    }

    public String getLinkedInPassword() {
        return linkedInPassword;
    }

    public String getDiceUsername() {
        return diceUsername;
    }

    public String getDicePassword() {
        return dicePassword;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public List<String> getJobSkills() {
        return jobSkills;
    }

    public int getMatchingThreshold() {
        return matchingThreshold;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void validateLinkedInCredentials() {
        if (linkedInUsername.isEmpty() || linkedInPassword.isEmpty()) {
            throw new IllegalArgumentException("LinkedIn credentials not set. Set LINKEDIN_USERNAME and LINKEDIN_PASSWORD environment variables.");
        }
    }

    public void validateDiceCredentials() {
        if (diceUsername.isEmpty() || dicePassword.isEmpty()) {
            throw new IllegalArgumentException("Dice credentials not set. Set DICE_USERNAME and DICE_PASSWORD environment variables.");
        }
    }
}

