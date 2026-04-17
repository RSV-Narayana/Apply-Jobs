package com.example.applyjobs.model;

import java.time.LocalDate;

public class ApplicationResult {
    private String jobTitle;
    private String companyName;
    private LocalDate applicationDate;
    private String status;

    public ApplicationResult(String jobTitle, String companyName, LocalDate applicationDate, String status) {
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.applicationDate = applicationDate;
        this.status = status;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s", jobTitle, companyName, applicationDate, status);
    }
}

