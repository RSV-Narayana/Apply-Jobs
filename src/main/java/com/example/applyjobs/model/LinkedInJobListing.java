package com.example.applyjobs.model;

public class LinkedInJobListing extends JobListing {
    private String id;

    public LinkedInJobListing(String id, String title, String company, String location, String description) {
        super(title, company, location, description);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getJobUrl() {
        return "https://www.linkedin.com/jobs/view/" + id;
    }
}

