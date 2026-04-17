package com.example.applyjobs.linkedin;

import com.example.applyjobs.config.EnvironmentVariableLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LinkedIn Job Application Agent Tests")
public class LinkedInJobApplicationAgentTest {

    private EnvironmentVariableLoader config;

    @BeforeEach
    void setUp() {
        config = new EnvironmentVariableLoader();
    }

    @Test
    @DisplayName("Should load environment variables correctly")
    void testEnvironmentVariableLoading() {
        // Set some test env vars
        assertNotNull(config.getJobTitle());
        assertNotNull(config.getJobSkills());
        assertTrue(config.getJobSkills().size() > 0);
        assertTrue(config.getMatchingThreshold() > 0);
    }

    @Test
    @DisplayName("Should validate LinkedIn credentials")
    void testLinkedInCredentialsValidation() {
        // This will throw if credentials are not set
        try {
            config.validateLinkedInCredentials();
            // If we get here, credentials are set
            assertNotNull(config.getLinkedInUsername());
            assertNotNull(config.getLinkedInPassword());
        } catch (IllegalArgumentException e) {
            // Expected if env vars not set - that's ok for testing
            assertTrue(e.getMessage().contains("LinkedIn credentials"));
        }
    }
}

