# Environment Configuration Guide

## Overview

The Job Application Agent reads all configuration from environment variables. This guide covers setup, validation, and best practices.

## Required Environment Variables

### LinkedIn Credentials
```bash
LINKEDIN_USERNAME=your.email@example.com
LINKEDIN_PASSWORD=your_secure_password
```

### Dice Credentials
```bash
DICE_USERNAME=your.email@example.com
DICE_PASSWORD=your_secure_password
```

### Job Preferences
```bash
JOB_TITLE=Full Stack Engineer
JOB_SKILLS=Java,Spring Boot,React,PostgreSQL,Docker,AWS
```

## Configuration Details

### 1. LinkedIn Credentials

| Variable | Description | Example |
|----------|-------------|---------|
| `LINKEDIN_USERNAME` | LinkedIn account email | `john.doe@gmail.com` |
| `LINKEDIN_PASSWORD` | LinkedIn account password | `SecurePass123!` |

**Notes:**
- Use your actual LinkedIn login email
- If you have 2FA enabled, disable it temporarily or handle the prompt in browser automation
- Password is read once at startup and not logged

### 2. Dice Credentials

| Variable | Description | Example |
|----------|-------------|---------|
| `DICE_USERNAME` | Dice account email | `john.doe@gmail.com` |
| `DICE_PASSWORD` | Dice account password | `SecurePass123!` |

**Notes:**
- Dice may use the same email as LinkedIn
- Credentials are portal-independent
- Configure separately even if same as LinkedIn

### 3. Job Preferences

| Variable | Description | Format | Example |
|----------|-------------|--------|---------|
| `JOB_TITLE` | Target job title to match | String | `Full Stack Engineer` |
| `JOB_SKILLS` | Required skills for matching | Comma-separated | `Java,Spring Boot,React` |

**Notes:**
- `JOB_TITLE` is case-insensitive in matching
- `JOB_SKILLS` is split by commas (no spaces around commas)
- All skills must be present in job description for a match
- Matching is keyword-based, not fuzzy

## Setup Instructions

### macOS/Linux

#### Option 1: Export in Terminal (Temporary)
```bash
# Add to your current terminal session
export LINKEDIN_USERNAME="your.email@example.com"
export LINKEDIN_PASSWORD="your_password"
export DICE_USERNAME="your.email@example.com"
export DICE_PASSWORD="your_password"
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"

# Verify setup
echo $LINKEDIN_USERNAME
```

#### Option 2: Add to Shell Profile (Persistent)
```bash
# For bash (~/.bash_profile or ~/.bashrc)
# For zsh (~/.zshrc)

vim ~/.zshrc

# Add these lines at the end:
export LINKEDIN_USERNAME="your.email@example.com"
export LINKEDIN_PASSWORD="your_password"
export DICE_USERNAME="your.email@example.com"
export DICE_PASSWORD="your_password"
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"

# Reload shell
source ~/.zshrc
```

#### Option 3: Use .env File (Development Only)
Create `.env` file in project root:
```
LINKEDIN_USERNAME=your.email@example.com
LINKEDIN_PASSWORD=your_password
DICE_USERNAME=your.email@example.com
DICE_PASSWORD=your_password
JOB_TITLE=Full Stack Engineer
JOB_SKILLS=Java,Spring Boot,React,PostgreSQL,Docker
```

Load in Java application:
```java
DotEnv dotenv = Dotenv.load();
String linkedInUsername = dotenv.get("LINKEDIN_USERNAME");
```

Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

### Windows

#### Option 1: Set Environment Variables (Temporary)
```powershell
# In PowerShell
[Environment]::SetEnvironmentVariable("LINKEDIN_USERNAME", "your.email@example.com", "Process")
[Environment]::SetEnvironmentVariable("LINKEDIN_PASSWORD", "your_password", "Process")
[Environment]::SetEnvironmentVariable("DICE_USERNAME", "your.email@example.com", "Process")
[Environment]::SetEnvironmentVariable("DICE_PASSWORD", "your_password", "Process")
[Environment]::SetEnvironmentVariable("JOB_TITLE", "Full Stack Engineer", "Process")
[Environment]::SetEnvironmentVariable("JOB_SKILLS", "Java,Spring Boot,React,PostgreSQL,Docker", "Process")

# Verify
[Environment]::GetEnvironmentVariable("LINKEDIN_USERNAME")
```

#### Option 2: Set Environment Variables (Persistent)
```powershell
# In PowerShell (Admin)
[Environment]::SetEnvironmentVariable("LINKEDIN_USERNAME", "your.email@example.com", "User")
[Environment]::SetEnvironmentVariable("LINKEDIN_PASSWORD", "your_password", "User")
# ... repeat for all variables with "User" scope
```

Or use GUI:
1. Right-click "This PC" → Properties
2. Advanced system settings → Environment Variables
3. User variables → New
4. Add each variable

## Validation Checklist

Create `validate-env.sh`:
```bash
#!/bin/bash

REQUIRED_VARS=("LINKEDIN_USERNAME" "LINKEDIN_PASSWORD" "DICE_USERNAME" "DICE_PASSWORD" "JOB_TITLE" "JOB_SKILLS")

echo "Validating Environment Variables..."
MISSING=0

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Missing: $var"
        MISSING=$((MISSING + 1))
    else
        echo "✓ Found: $var"
    fi
done

if [ $MISSING -eq 0 ]; then
    echo ""
    echo "✅ All environment variables configured!"
    echo ""
    echo "Configuration Summary:"
    echo "LinkedIn Username: ${LINKEDIN_USERNAME:0:5}..."
    echo "Job Title: ${JOB_TITLE}"
    echo "Job Skills Count: $(echo $JOB_SKILLS | tr ',' '\n' | wc -l)"
else
    echo ""
    echo "❌ Missing $MISSING environment variable(s)"
    exit 1
fi
```

Run validation:
```bash
chmod +x validate-env.sh
./validate-env.sh
```

## Java Code: Reading Environment Variables

```java
public class EnvironmentVariableLoader {
    
    // LinkedIn Credentials
    public static String getLinkedInUsername() {
        return System.getenv("LINKEDIN_USERNAME");
    }
    
    public static String getLinkedInPassword() {
        return System.getenv("LINKEDIN_PASSWORD");
    }
    
    // Dice Credentials
    public static String getDiceUsername() {
        return System.getenv("DICE_USERNAME");
    }
    
    public static String getDicePassword() {
        return System.getenv("DICE_PASSWORD");
    }
    
    // Job Preferences
    public static String getJobTitle() {
        return System.getenv("JOB_TITLE");
    }
    
    public static List<String> getJobSkills() {
        String skillsStr = System.getenv("JOB_SKILLS");
        if (skillsStr == null || skillsStr.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(skillsStr.split(","));
    }
    
    // Validation
    public static void validateEnvironmentVariables() {
        String[] required = {
            "LINKEDIN_USERNAME", "LINKEDIN_PASSWORD",
            "DICE_USERNAME", "DICE_PASSWORD",
            "JOB_TITLE", "JOB_SKILLS"
        };
        
        List<String> missing = new ArrayList<>();
        for (String var : required) {
            if (System.getenv(var) == null || System.getenv(var).isEmpty()) {
                missing.add(var);
            }
        }
        
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Missing environment variables: " + String.join(", ", missing)
            );
        }
    }
}
```

## Security Best Practices

### ✅ DO:
- Store credentials in environment variables only
- Use secure passwords (min 12 characters, mixed case, numbers, symbols)
- Disable logging of credentials
- Use `.env` files only in development
- Rotate passwords periodically
- Consider using a secrets manager (e.g., AWS Secrets Manager, HashiCorp Vault)

### ❌ DON'T:
- Hardcode credentials in source code
- Commit `.env` files to version control
- Log or print credentials
- Share credentials via email or chat
- Use weak passwords (birthdate, "password123", etc.)
- Store credentials in plain text files

## Example: Complete Setup

```bash
#!/bin/bash
# setup-env.sh - Interactive environment setup

echo "Job Application Agent - Environment Setup"
echo "=========================================="
echo ""

read -p "Enter LinkedIn Email: " LINKEDIN_USERNAME
read -sp "Enter LinkedIn Password: " LINKEDIN_PASSWORD
echo ""
read -p "Enter Dice Email: " DICE_USERNAME
read -sp "Enter Dice Password: " DICE_PASSWORD
echo ""
read -p "Enter Target Job Title: " JOB_TITLE
read -p "Enter Required Skills (comma-separated): " JOB_SKILLS

export LINKEDIN_USERNAME
export LINKEDIN_PASSWORD
export DICE_USERNAME
export DICE_PASSWORD
export JOB_TITLE
export JOB_SKILLS

echo ""
echo "✅ Environment variables set for this session"
echo "To make persistent, add to ~/.zshrc or ~/.bash_profile"
```

## Troubleshooting

### Variables Not Found
```bash
# Check if variable is set
env | grep LINKEDIN_USERNAME

# Try with different shell
/bin/bash -c "echo $LINKEDIN_USERNAME"
```

### Application Not Reading Variables
```java
// Add debug logging
System.out.println("LINKEDIN_USERNAME: " + System.getenv("LINKEDIN_USERNAME"));

// In IDE (IntelliJ), set Run Configuration:
// Run → Edit Configurations → Environment variables
```

### Password Contains Special Characters
- Escape with quotes: `export LINKEDIN_PASSWORD="P@ssw0rd!"`
- Or use single quotes: `export LINKEDIN_PASSWORD='P@ssw0rd!'`

## Example Configuration Files

See additional files for practical examples:
- [BROWSER_AUTOMATION.md](BROWSER_AUTOMATION.md) - How variables are used in login
- [LINKEDIN_AGENT.md](LINKEDIN_AGENT.md) - LinkedIn-specific variable usage
- [DICE_AGENT.md](DICE_AGENT.md) - Dice-specific variable usage

