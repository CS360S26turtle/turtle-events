package com.example.peertutoringmarketplace;

import java.time.LocalDateTime;

public class Review {
    private String reviewId;
    private int rating;
    private String comment;
    private StudentProfile author;
    private TutorProfile target;
    private Session session;
    private LocalDateTime createdAt;

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public StudentProfile getAuthor() {
        return author;
    }

    public void setAuthor(StudentProfile author) {
        this.author = author;
    }

    public TutorProfile getTarget() {
        return target;
    }

    public void setTarget(TutorProfile target) {
        this.target = target;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
