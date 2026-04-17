package com.example.applyjobs;

import com.example.applyjobs.linkedin.LinkedInJobApplicationAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for running the LinkedIn job application agent
 *
 * Required Environment Variables:
 * - LINKEDIN_USERNAME: Your LinkedIn email
 * - LINKEDIN_PASSWORD: Your LinkedIn password
 * - JOB_TITLE: Job title to search for (e.g., "Full Stack Engineer")
 * - JOB_SKILLS: Comma-separated skills (e.g., "Java,Spring Boot,React")
 * - JOB_MATCHING_THRESHOLD: Skill match percentage (default: 50)
 * - OUTPUT_DIRECTORY: Where to save results (default: current directory)
 */
public class LinkedInJobApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobApplicationRunner.class);

    public static void main(String[] args) {
        logger.info("========== LinkedIn Job Application Runner ==========");
        logger.info("Starting LinkedIn job application agent...");

        try {
            LinkedInJobApplicationAgent agent = new LinkedInJobApplicationAgent();
            agent.executeWorkflow();

            logger.info("========== Workflow completed successfully ==========");
            System.exit(0);
        } catch (Exception e) {
            logger.error("========== Workflow failed ==========", e);
            System.exit(1);
        }
    }
}

