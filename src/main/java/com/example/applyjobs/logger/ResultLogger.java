package com.example.applyjobs.logger;

import com.example.applyjobs.model.ApplicationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ResultLogger {
    private static final Logger logger = LoggerFactory.getLogger(ResultLogger.class);
    private String outputDirectory;
    private String provider;
    private List<ApplicationResult> results;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ResultLogger(String outputDirectory, String provider) {
        this.outputDirectory = outputDirectory;
        this.provider = provider;
        this.results = new ArrayList<>();
    }

    /**
     * Log a single application result
     */
    public void logResult(ApplicationResult result) {
        try {
            logger.info("Logging result: {} - {}", result.getJobTitle(), result.getCompanyName());
            results.add(result);
            writeResultToFile(result);
        } catch (IOException e) {
            logger.error("Failed to log result", e);
        }
    }

    /**
     * Write result to file
     */
    private void writeResultToFile(ApplicationResult result) throws IOException {
        String filename = getLogFilename();
        File file = new File(filename);

        boolean fileExists = file.exists();

        try (FileWriter writer = new FileWriter(filename, true)) {
            // Write header if file is new
            if (!fileExists) {
                writer.write("Job Title\tCompany Name\tApplication Date\tStatus\n");
            }

            // Write result
            writer.write(result.toString() + "\n");
            writer.flush();

            logger.info("Result written to file: {}", filename);
        }
    }

    /**
     * Get log filename based on provider and date
     */
    private String getLogFilename() {
        String dateStr = LocalDate.now().format(dateFormatter);
        String filename = String.format("applied_jobs_%s_%s.txt", provider.toLowerCase(), dateStr);
        return new File(outputDirectory, filename).getAbsolutePath();
    }

    /**
     * Write summary log
     */
    public void writeSummary() {
        try {
            logger.info("Writing summary for {} applications", results.size());

            if (results.isEmpty()) {
                logger.warn("No results to summarize");
                return;
            }

            long successCount = results.stream()
                .filter(r -> r.getStatus().equalsIgnoreCase("SUCCESS"))
                .count();

            long failedCount = results.stream()
                .filter(r -> r.getStatus().equalsIgnoreCase("FAILED"))
                .count();

            logger.info("=== Summary ===");
            logger.info("Provider: {}", provider);
            logger.info("Total Applications: {}", results.size());
            logger.info("Successful: {}", successCount);
            logger.info("Failed: {}", failedCount);
            logger.info("Log File: {}", getLogFilename());

        } catch (Exception e) {
            logger.error("Failed to write summary", e);
        }
    }

    /**
     * Get all logged results
     */
    public List<ApplicationResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Get count of successful applications
     */
    public long getSuccessCount() {
        return results.stream()
            .filter(r -> r.getStatus().equalsIgnoreCase("SUCCESS"))
            .count();
    }

    /**
     * Get count of failed applications
     */
    public long getFailedCount() {
        return results.stream()
            .filter(r -> r.getStatus().equalsIgnoreCase("FAILED"))
            .count();
    }
}

