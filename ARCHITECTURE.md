# Architecture & Data Flow Documentation

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    ApplyJobsApplication                         │
│                    (Entry Point)                                │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              LinkedInJobApplicationAgent                        │
│              (Main Workflow Orchestrator)                       │
├─────────────────────────────────────────────────────────────────┤
│ Responsibilities:                                               │
│ • Initialize all handlers                                       │
│ • Orchestrate workflow steps                                    │
│ • Manage overall flow control                                   │
│ • Aggregate results and generate reports                        │
└──────┬──────────────┬──────────────┬──────────────┬─────────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
  │ LinkedIn │  │ LinkedIn │  │ LinkedIn │  │   LinkedIn   │
  │  Login   │  │Navigation│  │   Job    │  │ Application  │
  │ Handler  │  │ Handler  │  │Extractor │  │   Handler    │
  └────┬────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘
       │            │             │               │
       └────────────┴─────────────┴───────────────┘
              │
              ▼
    ┌──────────────────────────┐
    │  LinkedInJobValidation   │
    │  Handler (NEW)           │
    │──────────────────────────│
    │ • Employment Type Check  │
    │ • Salary Validation      │
    │ • Skills Matching        │
    │ • Experience Check       │
    └──────────┬───────────────┘
               │
        ┌──────┴──────┐
        │             │
        ▼             ▼
   ┌────────┐    ┌──────────┐
   │Job     │    │Result    │
   │Matcher │    │Logger    │
   └────────┘    └──────────┘
```

## Data Flow - Complete Job Application Cycle

```
┌─────────────────────────────────────────────────────────────────┐
│ START: Initialize Application                                   │
└─────────────┬───────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ LOGIN PHASE                                                     │
├─────────────────────────────────────────────────────────────────┤
│ 1. Load environment variables (username, password)              │
│ 2. Initialize BrowserAutomationEngine (create Chrome session)   │
│ 3. Navigate to linkedin.com                                     │
│ 4. Enter credentials                                            │
│ 5. Authenticate and wait for feed                               │
└─────────────┬───────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ JOB SEARCH PHASE                                                │
├─────────────────────────────────────────────────────────────────┤
│ 1. Navigate to Jobs page                                        │
│ 2. Enter search criteria (JOB_TITLE)                            │
│ 3. Apply filters                                                │
│ 4. Wait for results to load                                     │
└─────────────┬───────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ JOB EXTRACTION PHASE                                            │
├─────────────────────────────────────────────────────────────────┤
│ Input: Job listing page with 25+ job items                      │
│                                                                 │
│ Process:                                                        │
│  1. Find job elements using 2026 XPath selectors               │
│  2. Extract for each job:                                       │
│     • Job ID                                                    │
│     • Title                                                     │
│     • Company name                                              │
│     • Location                                                  │
│     • Description/summary                                       │
│  3. Store up to 10 jobs in LinkedInJobListing objects           │
│                                                                 │
│ Output: List<LinkedInJobListing> (max 10 jobs)                 │
└─────────────┬───────────────────────────────────────────────────┘
              │
              ▼
        ┌─────────────┐
        │ For each job │
        └──────┬──────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│ INITIAL MATCHING PHASE                                          │
├─────────────────────────────────────────────────────────────────┤
│ Use existing JobMatcher to check:                               │
│  • Job title contains JOB_TITLE                                 │
│  • Job description contains JOB_SKILLS (≥50% match)             │
│                                                                 │
│ If NO match → Skip to next job                                  │
│ If YES → Proceed to validation                                  │
└─────────────┬───────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ JOB CLICK & DETAILS PANEL LOAD                                  │
├─────────────────────────────────────────────────────────────────┤
│ 1. Click job listing in left panel                              │
│ 2. Wait for right panel to load job details                     │
│ 3. Extract all visible job details text                         │
│ 4. Parse using regex and DOM selectors                          │
└─────────────┬───────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ ADVANCED VALIDATION PHASE (NEW)                                 │
├─────────────────────────────────────────────────────────────────┤
│ Using LinkedInJobValidationHandler:                             │
│                                                                 │
│ ┌─ Condition 1: Employment Type Check                           │
│ │  └─ Search for "Contract" keyword                             │
│ │     └─ Result: PASS/FAIL                                      │
│ │                                                               │
│ ├─ Condition 2: Salary Validation                               │
│ │  └─ Regex extract: $\d+ pattern                               │
│ │     └─ Check: max salary ≥ $60                                │
│ │        └─ Result: PASS/FAIL                                   │
│ │                                                               │
│ ├─ Condition 3: Skills Matching                                 │
│ │  └─ Check job description for JOB_SKILLS                      │
│ │     └─ Calculate match percentage                             │
│ │        └─ Require: ≥60% skill match                           │
│ │           └─ Result: PASS/FAIL                                │
│ │                                                               │
│ └─ Condition 4: Experience Requirement                          │
│    └─ Regex extract: \d+\s+years experience                    │
│       └─ Check: requirement ≥ 10 years                          │
│          └─ Result: PASS/FAIL                                   │
│                                                                 │
│ Final Decision: ALL conditions PASS → Apply | FAIL → Skip       │
└─────────────┬───────────────────────────────────────────────────┘
              │
         ┌────┴─────┐
         │           │
    FAIL │           │ PASS
         │           │
         ▼           ▼
      SKIP       ┌─────────────────────────────┐
      JOB        │ APPLICATION PHASE           │
                 ├─────────────────────────────┤
                 │ 1. Click "Apply" button     │
                 │ 2. Wait for Easy Apply      │
                 │    modal to appear          │
                 │ 3. Detect form fields       │
                 │                             │
                 │    Loop (up to 15 steps):   │
                 │    ┌─────────────────────┐  │
                 │    │ Check review step   │  │
                 │    │ ↓                   │  │
                 │    │ Fill form fields    │  │
                 │    │ ├─ Inputs           │  │
                 │    │ ├─ Textareas       │  │
                 │    │ ├─ Selects         │  │
                 │    │ └─ Checkboxes      │  │
                 │    │ ↓                   │  │
                 │    │ Click "Next"?       │  │
                 │    │ └─ YES: Continue    │  │
                 │    │ └─ NO: Check Submit │  │
                 │    │     ↓               │  │
                 │    │ Click "Submit"?     │  │
                 │    │ └─ YES: SUCCESS! ✓  │  │
                 │    │ └─ NO: Exit loop    │  │
                 │    └─────────────────────┘  │
                 │                             │
                 └────────────────┬────────────┘
                                  │
                                  ▼
                 ┌─────────────────────────────┐
                 │ LOGGING & COMPLETION        │
                 ├─────────────────────────────┤
                 │ SUCCESS:                    │
                 │  • Log to CSV: Success      │
                 │  • Wait 10-30 seconds       │
                 │  • Continue to next job     │
                 │                             │
                 │ FAILURE:                    │
                 │  • Don't log (not recorded) │
                 │  • Continue to next job     │
                 └────────────────┬────────────┘
                                  │
                                  ▼
                 ┌─────────────────────────────┐
                 │ Check: More jobs?           │
                 ├─────────────────────────────┤
                 │ YES → Loop to next job      │
                 │ NO  → Exit loop             │
                 └────────────────┬────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ CLEANUP & REPORTING PHASE                                       │
├─────────────────────────────────────────────────────────────────┤
│ 1. Generate summary report                                      │
│ 2. Write results to CSV file                                    │
│ 3. Close browser                                                │
│ 4. Exit application                                             │
└─────────────────────────────────────────────────────────────────┘
```

## Component Interactions

### 1. Job Extractor → Application Handler Flow

```
LinkedInJobExtractor
├─ Extracts: LinkedInJobListing objects
│  ├─ id, title, company, location
│  └─ description, hasEasyApply
│
└─→ LinkedInApplicationHandler
   ├─ Receives: LinkedInJobListing
   ├─ Action: Click job, view details
   ├─ Call: LinkedInJobValidationHandler.validateJob()
   │  └─ Returns: ValidationResult
   │     ├─ employmentTypeValid
   │     ├─ salaryValid
   │     ├─ skillsValid
   │     ├─ experienceValid
   │     └─ isValid (all 4 conditions)
   │
   ├─ If isValid=true:
   │  ├─ Click Apply button
   │  ├─ Loop: handleEasyApplyFlow()
   │  │  ├─ Fill form fields
   │  │  ├─ Click Next buttons
   │  │  └─ Click Submit button
   │  └─ Return: true (success)
   │
   └─ If isValid=false:
      └─ Return: false (skip job)
```

### 2. Validation Handler Flow

```
LinkedInJobValidationHandler
│
├─ Input: LinkedInJobListing (after details panel loaded)
│
├─ validateJob():
│  │
│  ├─ 1. validateEmploymentType()
│  │  ├─ Search: XPath selectors for "Contract"
│  │  ├─ Search: Job details text
│  │  └─ Set: job.employmentType
│  │
│  ├─ 2. validateMinimumSalary()
│  │  ├─ Extract: Job details text
│  │  ├─ Regex: \$(\d+)(?:\s*-\s*\$(\d+))?
│  │  ├─ Check: maxSalary >= 60
│  │  └─ Set: job.minSalary, job.maxSalary
│  │
│  ├─ 3. validateSkillsMatch()
│  │  ├─ Extract: Job details + description
│  │  ├─ Count: Skill occurrences
│  │  ├─ Calculate: Match percentage
│  │  └─ Check: percentage >= 60%
│  │
│  └─ 4. validateExperienceRequirement()
│     ├─ Extract: Job details text
│     ├─ Regex: (\d+)\+?\s*(?:years?|yrs)
│     ├─ Find: Highest experience number
│     └─ Check: years >= 10
│
└─ Output: ValidationResult
   └─ isValid: true/false (all 4 checks)
```

### 3. Easy Apply Form Navigation

```
Easy Apply Modal Opened
│
├─ Step Detection:
│  ├─ Is review step? → uncheck Follow Company
│  └─ Has form fields? → Fill all fields
│
├─ Field Auto-Fill Logic:
│  │
│  ├─ getQuestionText(field)
│  │  └─ Extract from: name, aria-label, placeholder
│  │
│  └─ generateResponseForQuestion(question)
│     ├─ "experience" → "5+ years professional"
│     ├─ "location" → "Willing to relocate"
│     ├─ "start date" → "Immediately"
│     ├─ "salary" → "Open to discussion"
│     ├─ "notice period" → "2 weeks"
│     ├─ "remote" → "Flexible arrangement"
│     ├─ "visa" → "No sponsorship needed"
│     └─ default → "Yes, interested"
│
├─ Button Detection:
│  ├─ Try: clickSubmitButton()
│  │  └─ Selectors: "Submit", "Done", etc.
│  │     └─ If found & clickable: RETURN TRUE (SUCCESS)
│  │
│  └─ Try: clickNextButton()
│     └─ Selectors: "Next", etc.
│        └─ If found & clickable: Continue loop
│
└─ Loop (max 15 iterations)
   └─ If no buttons found: EXIT (application complete)
```

## Data Structures

### LinkedInJobListing
```
- id: String (unique job identifier)
- title: String (job title)
- company: String (company name)
- location: String (job location)
- description: String (job description)
- [NEW] employmentType: String (Contract, Full-time, etc.)
- [NEW] minSalary: double ($)
- [NEW] maxSalary: double ($)
- [NEW] salaryCurrency: String (USD, etc.)
- [NEW] yearsExperienceRequired: int (years)
- [NEW] hasEasyApply: boolean
- [NEW] jobElement: WebElement (DOM reference)
```

### ValidationResult
```
- jobId: String
- employmentTypeValid: boolean
- salaryValid: boolean
- skillsValid: boolean
- experienceValid: boolean
- isValid: boolean (all 4 pass)
```

### ApplicationResult (for logging)
```
- jobTitle: String
- companyName: String
- dateApplied: LocalDate
- status: String ("SUCCESS", "FAILED", "ATTEMPTED")
```

## Configuration Flow

```
Environment Variables
│
├─ LINKEDIN_USERNAME
├─ LINKEDIN_PASSWORD
├─ JOB_TITLE
├─ JOB_SKILLS
├─ JOB_MATCHING_THRESHOLD
└─ OUTPUT_DIRECTORY
    │
    ▼
EnvironmentVariableLoader
    │
    ▼
LinkedInJobApplicationAgent
    │
    ├─ jobTitle → JobMatcher
    ├─ jobSkills → JobMatcher & ValidationHandler
    ├─ matchingThreshold → JobMatcher
    ├─ outputDirectory → ResultLogger
    └─ credentials → LoginHandler
```

## Error Handling Strategy

```
Application Execution
│
├─ LoginException
│  └─ Credentials invalid / Account locked / 2FA issue
│
├─ NavigationException
│  └─ Page not found / Navigation timeout
│
├─ JobExtractionException
│  └─ XPath selectors all failed / Page structure changed
│
├─ ApplicationException
│  └─ Apply button not found / Form not accessible
│
└─ General Exception
   ├─ Catch & Log
   ├─ Cleanup resources
   └─ Graceful shutdown
```

---

**Architecture Version**: 2.0
**Diagram Created**: 2026-04-18

