/*
 * File: Review.java
 * Purpose: Captures student feedback and ratings for tutors after a session.
 * Design Pattern: Domain Entity.
 * Outstanding Issues: Input validation for rating (1-5) is not yet implemented.
 */
package com.example.peertutoringmarketplace;

import com.google.type.DateTime;

/**
 * Represents a performance review left by a student for a tutor.
 */
public class Review {
    private String reviewId;
    private int rating;
    private String comment;
    private String studentId;
    private String tutorId;
    private DateTime createdAt;

    /**
     * @return The unique ID of the review.
     */
    public String getReviewId() { return reviewId; }

    /**
     * @param reviewId The unique ID to set.
     */
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    /**
     * @return The numerical rating given (1 to 5).
     */
    public int getRating() { return rating; }

    /**
     * @param rating The numerical rating to set.
     */
    public void setRating(int rating) { this.rating = rating; }

    /**
     * @return The written feedback comment.
     */
    public String getComment() { return comment; }

    /**
     * @param comment The written feedback comment to set.
     */
    public void setComment(String comment) { this.comment = comment; }

    /**
     * @return The timestamp when the review was created.
     */
    public DateTime getCreatedAt() { return createdAt; }

    /**
     * @param createdAt The creation timestamp.
     */
    public void setCreatedAt(DateTime createdAt) { this.createdAt = createdAt; }

    /**
     * @return The ID of the student who authored the review.
     */
    public String getStudentId() { return studentId; }

    /**
     * @param studentId The student author ID.
     */
    public void setStudentId(String studentId) { this.studentId = studentId; }

    /**
     * @return The ID of the tutor being reviewed.
     */
    public String getTutorId() { return tutorId; }

    /**
     * @param tutorId The tutor ID.
     */
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }
}