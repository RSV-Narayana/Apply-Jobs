package com.example.applyjobs.model;

import org.openqa.selenium.WebElement;

public class LinkedInJobListing extends JobListing {
    private String id;
    private String employmentType;
    private double minSalary;
    private double maxSalary;
    private String salaryCurrency;
    private int yearsExperienceRequired;
    private boolean hasEasyApply;
    private WebElement jobElement; // To enable direct interaction after extraction

    public LinkedInJobListing(String id, String title, String company, String location, String description) {
        super(title, company, location, description);
        this.id = id;
        this.minSalary = 0;
        this.maxSalary = 0;
        this.yearsExperienceRequired = 0;
        this.hasEasyApply = false;
    }

    public String getId() {
        return id;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public double getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(double minSalary) {
        this.minSalary = minSalary;
    }

    public double getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(double maxSalary) {
        this.maxSalary = maxSalary;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public void setSalaryCurrency(String salaryCurrency) {
        this.salaryCurrency = salaryCurrency;
    }

    public int getYearsExperienceRequired() {
        return yearsExperienceRequired;
    }

    public void setYearsExperienceRequired(int yearsExperienceRequired) {
        this.yearsExperienceRequired = yearsExperienceRequired;
    }

    public boolean isHasEasyApply() {
        return hasEasyApply;
    }

    public void setHasEasyApply(boolean hasEasyApply) {
        this.hasEasyApply = hasEasyApply;
    }

    public org.openqa.selenium.WebElement getJobElement() {
        return jobElement;
    }

    public void setJobElement(org.openqa.selenium.WebElement jobElement) {
        this.jobElement = jobElement;
    }

    @Override
    public String getJobUrl() {
        return "https://www.linkedin.com/jobs/view/" + id;
    }
}

