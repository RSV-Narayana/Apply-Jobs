# Custom Job Application Agent System

## Overview

This agent system automates job applications across multiple job portals (LinkedIn and Dice) by:
- Opening Chrome with automated login
- Reading credentials and job preferences from environment variables
- Navigating to job search results
- Matching jobs based on job description and skills
- Applying to matching jobs automatically
- Logging applied jobs with timestamp and provider

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│          Job Application Agent System                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐      ┌──────────────┐               │
│  │   LinkedIn   │      │    Dice      │               │
│  │    Agent     │      │    Agent     │               │
│  └──────────────┘      └──────────────┘               │
│          │                     │                       │
│          └────────┬────────────┘                       │
│                   │                                    │
│         ┌─────────▼──────────┐                        │
│         │ Browser Automation │ (Selenium/Playwright) │
│         │   + ChromeDriver   │                        │
│         └─────────┬──────────┘                        │
│                   │                                    │
│    ┌──────────────┼──────────────┐                    │
│    │              │              │                    │
│    ▼              ▼              ▼                    │
│ ┌──────┐  ┌─────────────┐  ┌─────────┐              │
│ │Login │  │Job Matching │  │ Output  │              │
│ │      │  │& Filtering  │  │ Logging │              │
│ └──────┘  └─────────────┘  └─────────┘              │
│    │              │              │                    │
│    └──────────────┼──────────────┘                    │
│                   │                                    │
│           ┌───────▼────────┐                          │
│           │ Environment    │                          │
│           │ Configuration  │                          │
│           └────────────────┘                          │
└─────────────────────────────────────────────────────────┘
```

## Component Structure

### 1. **Core Components**
- **BrowserAutomationEngine**: Handles Chrome driver initialization and web interactions
- **LoginManager**: Manages credential reading and login flow per portal
- **JobMatcher**: Implements job description parsing and skill matching logic
- **ApplicationExecutor**: Executes job application workflow
- **ResultLogger**: Writes applied jobs to timestamped files

### 2. **Portal-Specific Agents**
- **LinkedInAgent**: LinkedIn portal navigation and job application
- **DiceAgent**: Dice portal navigation and job application

### 3. **Configuration Management**
- **EnvironmentVariableLoader**: Reads from system environment variables
- **PortalConfig**: Portal-specific URLs, selectors, and workflows

## Workflow Execution Flow

```
START
  │
  ├─→ Load Environment Variables
  │   └─→ Username, Password, Job Title, Job Skills
  │
  ├─→ For Each Portal (LinkedIn, Dice):
  │   │
  │   ├─→ Open Chrome Browser
  │   │
  │   ├─→ Navigate to Login URL
  │   │
  │   ├─→ Perform Login
  │   │   ├─→ Enter Username
  │   │   ├─→ Enter Password
  │   │   └─→ Submit & Wait for Auth
  │   │
  │   ├─→ Navigate to Jobs Section
  │   │
  │   ├─→ Find Recent Job Searches
  │   │
  │   ├─→ Click on "full stack engineer" Search
  │   │
  │   ├─→ Wait for Results to Load
  │   │
  │   ├─→ Process Latest 5 Job Listings:
  │   │   │
  │   │   ├─→ Extract Job Title, Company, Description
  │   │   │
  │   │   ├─→ Match Job Description Against Skills
  │   │   │   └─→ Match: JOB_TITLE + All JOB_SKILLS in Description?
  │   │   │
  │   │   ├─→ If Match:
  │   │   │   ├─→ Click "Apply" Button
  │   │   │   ├─→ Complete Application Form (if required)
  │   │   │   ├─→ Submit Application
  │   │   │   └─→ Log to Applied Jobs File
  │   │   │
  │   │   └─→ If No Match:
  │   │       └─→ Skip Job
  │   │
  │   └─→ Close Browser
  │
  ├─→ Generate Output Files
  │   └─→ applied_jobs_YYYY-MM-DD_linkedin.txt
  │   └─→ applied_jobs_YYYY-MM-DD_dice.txt
  │
  └─→ END (With Summary Report)
```

## Environment Variables

**Required:**
```
LINKEDIN_USERNAME=your.email@example.com
LINKEDIN_PASSWORD=your_secure_password
DICE_USERNAME=your.email@example.com
DICE_PASSWORD=your_secure_password
JOB_TITLE=Full Stack Engineer
JOB_SKILLS=Java,Spring Boot,React,PostgreSQL,Docker
```

See [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md) for details.

## Technology Stack

- **Language**: Java (Spring Boot)
- **Browser Automation**: Selenium WebDriver (recommended) or Playwright for Java
- **Dependencies to Add**:
  - `org.seleniumhq.selenium:selenium-java`
  - `org.apache.commons:commons-lang3` (for string utilities)

## File Organization

```
agents/
├── README.md                      # This file
├── ENVIRONMENT_SETUP.md           # Environment variable configuration
├── BROWSER_AUTOMATION.md          # Browser automation patterns
├── JOB_MATCHING.md                # Job matching algorithm
├── LINKEDIN_AGENT.md              # LinkedIn implementation guide
├── DICE_AGENT.md                  # Dice implementation guide
└── OUTPUT_LOGGING.md              # Output file format & logging
```

## Implementation Phases

### Phase 1: Foundation
- Set up Selenium/Playwright dependencies
- Implement BrowserAutomationEngine
- Create EnvironmentVariableLoader
- Write basic Chrome initialization

### Phase 2: LinkedIn Integration
- Implement LinkedIn login flow
- Create job search navigation
- Build job extraction logic
- Integrate with job matcher

### Phase 3: Job Matching
- Implement skill matching algorithm
- Create job description parser
- Add filtering logic

### Phase 4: Application Flow
- Implement "Apply" button interaction
- Handle application forms
- Add error recovery

### Phase 5: Dice Integration
- Replicate LinkedIn flow for Dice
- Adapt to Dice-specific UI patterns
- Integrate with existing matcher/logger

### Phase 6: Output & Logging
- Implement result file generation
- Add metadata (timestamp, provider, status)
- Create summary reports

## Quick Start

1. **Setup Environment Variables**:
   ```bash
   export LINKEDIN_USERNAME=your.email@example.com
   export LINKEDIN_PASSWORD=your_password
   export DICE_USERNAME=your.email@example.com
   export DICE_PASSWORD=your_password
   export JOB_TITLE="Full Stack Engineer"
   export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"
   ```

2. **Add Dependencies** to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.seleniumhq.selenium</groupId>
       <artifactId>selenium-java</artifactId>
       <version>4.15.0</version>
   </dependency>
   ```

3. **Run Agent**:
   ```bash
   java -jar target/applyjobs-0.0.1-SNAPSHOT.jar
   ```

## Key Design Decisions

1. **Job Matching**: Uses keyword-based matching (Job Title + All Skills must be present in job description)
2. **Portal Handling**: Separate agent classes per portal for maintainability
3. **Error Recovery**: Automatic retries with exponential backoff for transient failures
4. **Logging**: Persistent file output with date and provider information
5. **Headless Mode**: Optional headless Chrome for batch processing

## Security Considerations

- Store credentials in environment variables (not in code)
- Use Java's `System.getenv()` for secure reading
- Consider encrypted environment variable storage for production
- Never log sensitive credentials

## Troubleshooting

See individual agent documentation:
- [LINKEDIN_AGENT.md](LINKEDIN_AGENT.md) - LinkedIn-specific issues
- [DICE_AGENT.md](DICE_AGENT.md) - Dice-specific issues
- [BROWSER_AUTOMATION.md](BROWSER_AUTOMATION.md) - General browser issues

## Future Enhancements

- [ ] Web UI dashboard for monitoring application progress
- [ ] Database storage of applied jobs
- [ ] Email notifications on successful applications
- [ ] Schedule-based execution (daily/hourly runs)
- [ ] Support for additional job portals (Indeed, GitHub Jobs, etc.)
- [ ] ML-based job matching instead of keyword matching
- [ ] Application status tracking and follow-ups

