package com.example.applyjobs.matcher;

import com.example.applyjobs.model.JobListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class JobMatcher {
    private static final Logger logger = LoggerFactory.getLogger(JobMatcher.class);
    private String jobTitle;
    private List<String> jobSkills;
    private int skillMatchThreshold;

    public JobMatcher(String jobTitle, List<String> jobSkills) {
        this(jobTitle, jobSkills, 50);
    }

    public JobMatcher(String jobTitle, List<String> jobSkills, int skillMatchThreshold) {
        this.jobTitle = jobTitle.toLowerCase().trim();
        this.jobSkills = jobSkills.stream()
            .map(skill -> skill.toLowerCase().trim())
            .collect(Collectors.toList());
        this.skillMatchThreshold = skillMatchThreshold;
    }

    /**
     * Matches a job listing against user profile
     * Returns true if job title matches AND skills match threshold
     */
    public boolean isMatch(JobListing job) {
        logger.debug("Checking match for job: {}", job.getTitle());

        boolean titleMatch = matchJobTitle(job);
        boolean skillsMatch = matchJobSkills(job);

        boolean result = titleMatch && skillsMatch;

        logger.info("Job: {} | Title Match: {} | Skills Match: {} | Overall: {}",
            job.getTitle(), titleMatch, skillsMatch, result);

        return result;
    }

    /**
     * Check if target job title appears in listing title or description
     */
    private boolean matchJobTitle(JobListing job) {
        String jobListingTitle = job.getTitle().toLowerCase();
        String jobListingDescription = job.getDescription().toLowerCase();

        boolean titleMatches = jobListingTitle.contains(jobTitle) || jobListingDescription.contains(jobTitle);

        logger.debug("Title match check: {} - found={}", jobTitle, titleMatches);

        return titleMatches;
    }

    /**
     * Check if required skills appear in job description
     */
    private boolean matchJobSkills(JobListing job) {
        String description = job.getDescription().toLowerCase();

        // Count how many required skills are found in description
        long matchedSkills = jobSkills.stream()
            .filter(skill -> description.contains(skill))
            .count();

        // Calculate percentage match
        double matchPercentage = (matchedSkills * 100.0) / jobSkills.size();

        boolean skillsMatch = matchPercentage >= skillMatchThreshold;

        logger.debug("Skills match check: {}/{} found ({}% >= {}%)",
            matchedSkills, jobSkills.size(), (int)matchPercentage, skillMatchThreshold);

        return skillsMatch;
    }

    /**
     * Get detailed match information
     */
    public MatchDetails getMatchDetails(JobListing job) {
        boolean titleMatch = matchJobTitle(job);
        boolean skillsMatch = matchJobSkills(job);

        String description = job.getDescription().toLowerCase();
        long matchedSkills = jobSkills.stream()
            .filter(skill -> description.contains(skill))
            .count();

        return new MatchDetails(
            titleMatch,
            skillsMatch,
            matchedSkills,
            jobSkills.size()
        );
    }

    /**
     * Inner class for detailed match information
     */
    public static class MatchDetails {
        public boolean titleMatched;
        public boolean skillsMatched;
        public long skillsFound;
        public long totalSkills;

        public MatchDetails(boolean titleMatched, boolean skillsMatched, long skillsFound, long totalSkills) {
            this.titleMatched = titleMatched;
            this.skillsMatched = skillsMatched;
            this.skillsFound = skillsFound;
            this.totalSkills = totalSkills;
        }

        @Override
        public String toString() {
            return String.format("Title: %s, Skills: %d/%d (%d%%)",
                titleMatched, skillsFound, totalSkills, (skillsFound * 100) / totalSkills);
        }
    }
}

