package com.cst438.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/test")
public class TestDataController {

    private final JdbcTemplate jdbc;

    public TestDataController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Seed with exactly the rows your tests expect */
    @PostMapping("/seed")
    public ResponseEntity<String> seed() {
        try {
            // First, clean up any existing data to avoid constraint violations
            reset();

            // Insert test data
            jdbc.update("""
              INSERT INTO user_table (id, name, email, password, type) VALUES
              (5, 'sama', 'sama@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
              (6, 'samb', 'samb@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
              (7, 'samc', 'samc@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT')
              """);

            jdbc.update("""
              INSERT INTO course VALUES ('cst599', 'Capstone', 4)
              """);

            jdbc.update("""
              INSERT INTO section (section_no, course_id, section_id, term_id, building, room, times, instructor_email) VALUES
              (2, 'cst599', 2, 10, '90', 'B104', 'W F 10-11', 'ted@csumb.edu')
              """);

            jdbc.update("""
              INSERT INTO enrollment (enrollment_id, grade, section_no, user_id) VALUES 
              (1, NULL, 2, 5),
              (2, NULL, 2, 6),
              (3, NULL, 2, 7)
              """);

            return ResponseEntity.ok("Test data seeded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error seeding data: " + e.getMessage());
        }
    }

    /** Wipe out everything you seeded (back to a clean slate) */
    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        try {
            // Delete in correct order to avoid foreign key constraint violations
            jdbc.update("DELETE FROM enrollment WHERE section_no = 2");
            jdbc.update("DELETE FROM section WHERE section_no = 2");
            jdbc.update("DELETE FROM course WHERE course_id IN ('cst499', 'cst599')");
            jdbc.update("DELETE FROM user_table WHERE id IN (5, 6, 7)");

            return ResponseEntity.ok("Test data reset successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error resetting data: " + e.getMessage());
        }
    }
}