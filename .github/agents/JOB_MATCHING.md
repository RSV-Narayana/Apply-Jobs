# Job Matching Algorithm Guide

## Overview

This document describes how the Job Application Agent determines if a job listing matches the user's profile (Job Title and Job Skills).

## Matching Strategy

### Core Algorithm

```
MATCH = (Job Title Match) AND (All Skills Match)

Where:
  - Job Title Match: Target Job Title exists in Job Listing Title OR Description
  - All Skills Match: ALL required Job Skills exist in Job Description (case-insensitive)
```

### Example

**User Configuration:**
```
JOB_TITLE = "Full Stack Engineer"
JOB_SKILLS = "Java,Spring Boot,React,PostgreSQL"
```

**Job Listing 1:**
```
Title: "Full Stack Software Engineer"
Description: "We are looking for a Full Stack Engineer with expertise in Java, 
Spring Boot, React, and PostgreSQL. Must have 5+ years of experience..."

Result: ✅ MATCH
Reason:
  ✓ Job Title contains "Full Stack Engineer"
  ✓ Description contains: Java, Spring Boot, React, PostgreSQL (all required skills)
```

**Job Listing 2:**
```
Title: "Frontend Engineer - React"
Description: "Seeking a Frontend Engineer with React experience. 
Knowledge of HTML5, CSS3, and JavaScript required..."

Result: ❌ NO MATCH
Reason:
  ✗ Job Title does not contain "Full Stack Engineer"
  ✗ Description missing skills: Java, Spring Boot, PostgreSQL (only has React)
```

**Job Listing 3:**
```
Title: "Full Stack Developer - Java/React"
Description: "Looking for a Full Stack Developer proficient in Java, Spring Boot, 
and React. PostgreSQL or MongoDB experience preferred..."

Result: ✅ MATCH
Reason:
  ✓ Job Title contains "Full Stack"
  ✓ Description contains all required: Java, Spring Boot, React, PostgreSQL
```

## Implementation

### Java Code Structure

```java
public class JobMatcher {
    private String jobTitle;
    private List<String> jobSkills;
    private static final Logger logger = LoggerFactory.getLogger(JobMatcher.class);
    
    public JobMatcher(String jobTitle, List<String> jobSkills) {
        this.jobTitle = jobTitle.toLowerCase().trim();
        this.jobSkills = jobSkills.stream()
            .map(skill -> skill.toLowerCase().trim())
            .collect(Collectors.toList());
    }
    
    /**
     * Matches a job listing against user profile
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
     * Check if target job title appears in listing
     */
    private boolean matchJobTitle(JobListing job) {
        String titleAndDescription = (job.getTitle() + " " + job.getDescription()).toLowerCase();
        
        // Check exact phrase match (more reliable)
        if (titleAndDescription.contains(jobTitle)) {
            return true;
        }
        
        // Check partial matches (handle variations)
        String[] titleParts = jobTitle.split(" ");
        if (titleParts.length >= 2) {
            // Match at least 2 consecutive words
            for (int i = 0; i < titleParts.length - 1; i++) {
                String twoWords = titleParts[i] + " " + titleParts[i + 1];
                if (titleAndDescription.contains(twoWords)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if all required skills appear in job description
     */
    private boolean matchJobSkills(JobListing job) {
        String description = job.getDescription().toLowerCase();
        
        for (String skill : jobSkills) {
            if (!description.contains(skill)) {
                logger.debug("Required skill missing: {}", skill);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculate match score (0.0 to 1.0)
     * Useful for ranking jobs
     */
    public double calculateMatchScore(JobListing job) {
        double score = 0.0;
        double maxScore = jobSkills.size() + 1; // +1 for title match
        
        // Title match (weight: 1.0)
        if (matchJobTitle(job)) {
            score += 1.0;
        }
        
        // Skills match (weight: 1.0 per skill)
        String description = job.getDescription().toLowerCase();
        for (String skill : jobSkills) {
            if (description.contains(skill)) {
                score += 1.0;
            }
        }
        
        return score / maxScore;
    }
}

/**
 * Job Listing data structure
 */
public class JobListing {
    private String id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String jobUrl;
    private LocalDateTime postedDate;
    
    // Getters and setters...
    
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", title, company, location);
    }
}
```

### Matching Strategy Variants

#### Variant 1: Basic Matching (Recommended for Start)
```java
public boolean isMatchBasic(JobListing job) {
    // Simple AND logic
    return matchJobTitle(job) && matchJobSkills(job);
}
```

#### Variant 2: Partial Skills Matching
```java
public boolean isMatchPartialSkills(JobListing job) {
    // Title must match, but only 80% of skills required
    if (!matchJobTitle(job)) {
        return false;
    }
    
    String description = job.getDescription().toLowerCase();
    long matchedSkills = jobSkills.stream()
        .filter(skill -> description.contains(skill))
        .count();
    
    double matchPercentage = (double) matchedSkills / jobSkills.size();
    return matchPercentage >= 0.8;
}
```

#### Variant 3: Skill Confidence Matching
```java
public boolean isMatchWithScore(JobListing job, double minScore) {
    // Title must match, skills score must exceed threshold
    if (!matchJobTitle(job)) {
        return false;
    }
    
    double score = calculateMatchScore(job);
    return score >= minScore;
}
```

## Job Description Extraction

### Pattern-Based Extraction

```java
public class JobDescriptionExtractor {
    
    /**
     * Extract job description from different layouts
     */
    public String extractDescription(WebElement jobElement) {
        StringBuilder description = new StringBuilder();
        
        // Pattern 1: Check for "About the job" section
        try {
            WebElement aboutSection = jobElement.findElement(
                By.xpath(".//section[contains(@aria-label, 'About')]")
            );
            description.append(aboutSection.getText()).append(" ");
        } catch (NoSuchElementException e) {
            // Continue to next pattern
        }
        
        // Pattern 2: Get main description div
        try {
            WebElement mainDesc = jobElement.findElement(
                By.xpath(".//div[@class='description']")
            );
            description.append(mainDesc.getText()).append(" ");
        } catch (NoSuchElementException e) {
            // Continue
        }
        
        // Pattern 3: Get all text content
        if (description.length() == 0) {
            description.append(jobElement.getText());
        }
        
        return description.toString().trim();
    }
    
    /**
     * Clean description text for matching
     */
    public String cleanDescription(String description) {
        // Remove extra whitespace
        description = description.replaceAll("\\s+", " ");
        
        // Remove special characters but keep words
        description = description.replaceAll("[^\\w\\s#+-]", " ");
        
        return description.trim();
    }
}
```

## Advanced Matching Features

### Feature 1: Skill Synonym Mapping
```java
public class SkillMatcher {
    private Map<String, List<String>> skillSynonyms = new HashMap<>();
    
    public SkillMatcher() {
        // Setup common synonyms
        skillSynonyms.put("java", Arrays.asList("java", "jdk", "j2ee"));
        skillSynonyms.put("spring boot", Arrays.asList("spring boot", "spring", "springboot"));
        skillSynonyms.put("react", Arrays.asList("react", "reactjs", "react.js"));
        skillSynonyms.put("postgresql", Arrays.asList("postgresql", "postgres", "pg"));
        skillSynonyms.put("docker", Arrays.asList("docker", "containerization", "containers"));
    }
    
    public boolean matchesSkill(String description, String requiredSkill) {
        List<String> variants = skillSynonyms.getOrDefault(
            requiredSkill.toLowerCase(),
            Arrays.asList(requiredSkill)
        );
        
        String desc = description.toLowerCase();
        return variants.stream().anyMatch(desc::contains);
    }
}
```

### Feature 2: Experience Level Matching
```java
public class ExperienceMatcher {
    
    public int extractExperienceRequired(String description) {
        Pattern pattern = Pattern.compile("(\\d+)\\+?\\s*(?:years|yrs)");
        Matcher matcher = pattern.matcher(description.toLowerCase());
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
    
    public boolean meetsExperienceRequirement(String description, int userExperience) {
        int required = extractExperienceRequired(description);
        return userExperience >= required;
    }
}
```

### Feature 3: Salary Range Matching
```java
public class SalaryMatcher {
    
    public Range<Integer> extractSalaryRange(String description) {
        // Pattern: $X - $Y or $XK - $YK
        Pattern pattern = Pattern.compile("\\$(\\d+)k?\\s*-\\s*\\$(\\d+)k?");
        Matcher matcher = pattern.matcher(description.toLowerCase());
        
        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            
            // Convert K to thousands if needed
            if (description.toLowerCase().contains("k")) {
                min *= 1000;
                max *= 1000;
            }
            
            return new Range<>(min, max);
        }
        return null;
    }
    
    public boolean meetsMinimumSalary(String description, int minSalary) {
        Range<Integer> range = extractSalaryRange(description);
        return range != null && range.getMin() >= minSalary;
    }
}

class Range<T extends Comparable<T>> {
    private T min;
    private T max;
    
    public Range(T min, T max) {
        this.min = min;
        this.max = max;
    }
    
    public T getMin() { return min; }
    public T getMax() { return max; }
}
```

## Testing & Validation

### Unit Test Examples

```java
public class JobMatcherTest {
    private JobMatcher matcher;
    private JobListing job;
    
    @Before
    public void setUp() {
        matcher = new JobMatcher(
            "Full Stack Engineer",
            Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL")
        );
    }
    
    @Test
    public void testExactMatch() {
        job = new JobListing(
            "Full Stack Engineer - Java/React",
            "This position requires Full Stack Engineer expertise in Java, Spring Boot, React, and PostgreSQL..."
        );
        assertTrue(matcher.isMatch(job));
    }
    
    @Test
    public void testNoMatchMissingSkill() {
        job = new JobListing(
            "Full Stack Engineer",
            "Looking for Full Stack Engineer with Java and React experience..."
        );
        // Missing Spring Boot and PostgreSQL
        assertFalse(matcher.isMatch(job));
    }
    
    @Test
    public void testNoMatchWrongTitle() {
        job = new JobListing(
            "Frontend Engineer",
            "React developer needed with knowledge of Java, Spring Boot, PostgreSQL..."
        );
        assertFalse(matcher.isMatch(job));
    }
    
    @Test
    public void testPartialMatch() {
        double score = matcher.calculateMatchScore(job);
        assertTrue(score > 0 && score < 1.0);
    }
}
```

## Configuration & Tuning

### Matching Sensitivity Levels

```java
public enum MatchingSensitivity {
    STRICT(1.0),           // Title + ALL skills
    NORMAL(0.8),           // Title + 80% skills
    LENIENT(0.6),          // Title + 60% skills
    VERY_LENIENT(0.4);     // Title + 40% skills
    
    private double minScore;
    
    MatchingSensitivity(double minScore) {
        this.minScore = minScore;
    }
    
    public double getMinScore() {
        return minScore;
    }
}

public class ConfigurableJobMatcher extends JobMatcher {
    private MatchingSensitivity sensitivity;
    
    public ConfigurableJobMatcher(String jobTitle, List<String> jobSkills, 
                                  MatchingSensitivity sensitivity) {
        super(jobTitle, jobSkills);
        this.sensitivity = sensitivity;
    }
    
    @Override
    public boolean isMatch(JobListing job) {
        if (sensitivity == MatchingSensitivity.STRICT) {
            return super.isMatch(job);
        }
        
        return matchJobTitle(job) && 
               calculateMatchScore(job) >= sensitivity.getMinScore();
    }
}
```

## Performance Considerations

### Optimization Tips

1. **Cache cleaned descriptions** - Don't re-clean same description multiple times
2. **Use case-insensitive matching once** - Convert to lowercase at start
3. **Check title before skills** - Title match is faster, fail-fast
4. **Batch process jobs** - Process multiple jobs in parallel

```java
public class PerformantJobMatcher {
    private String jobTitleLower;
    private List<String> jobSkillsLower;
    
    public PerformantJobMatcher(String jobTitle, List<String> jobSkills) {
        // Pre-process once
        this.jobTitleLower = jobTitle.toLowerCase();
        this.jobSkillsLower = jobSkills.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }
    
    // Use cached lowercase values in all matching
}
```

## Integration with Job Application Agent

See:
- [LINKEDIN_AGENT.md](LINKEDIN_AGENT.md) - How matching integrates with LinkedIn workflow
- [DICE_AGENT.md](DICE_AGENT.md) - How matching integrates with Dice workflow
- [README.md](README.md) - Overall system architecture

