package com.example.applyjobs;

import com.example.applyjobs.linkedin.LinkedInJobApplicationAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class ApplyJobsApplication {

    private static final Logger logger = LoggerFactory.getLogger(ApplyJobsApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Apply-Jobs Application");

        // Check if we have arguments to determine which agent to run
        if (args.length > 0 && "linkedin".equalsIgnoreCase(args[0])) {
            logger.info("Running LinkedIn Job Application Agent");
            try {
                LinkedInJobApplicationAgent agent = new LinkedInJobApplicationAgent();
                agent.executeWorkflow();
            } catch (Exception e) {
                logger.error("LinkedIn agent failed", e);
                System.exit(1);
            }
        } else {
            // Start Spring application normally
            SpringApplication.run(ApplyJobsApplication.class, args);
        }
    }

}
