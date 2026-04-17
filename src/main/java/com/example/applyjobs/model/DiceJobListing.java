package com.example.applyjobs.model;

public class DiceJobListing extends JobListing {
    private String jobUrl;

    public DiceJobListing(String title, String company, String jobUrl, String description) {
        super(title, company, "", description);
        this.jobUrl = jobUrl;
    }

    @Override
    public String getJobUrl() {
        return jobUrl;
    }
}

