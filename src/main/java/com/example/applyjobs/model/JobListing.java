package com.example.applyjobs.model;

public abstract class JobListing {
    protected String title;
    protected String company;
    protected String location;
    protected String description;

    public JobListing(String title, String company, String location, String description) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
    }

    public abstract String getJobUrl();

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", title, company, location);
    }
}

