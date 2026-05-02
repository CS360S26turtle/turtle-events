/*
 * File: TimeSlot.java
 * Purpose: Defines availability windows for tutors to be booked.
 * Design Pattern: Domain Entity.
 * Outstanding Issues: End time validation (must be after start time) is not yet added.
 */
package com.example.peertutoringmarketplace;

import java.util.Date;

/**
 * Represents a specific block of time available for tutoring.
 */
public class TimeSlot {
    private String slotId;
    private Date startTime;
    private Date endTime;
    private int maxCapacity;
    private String tutorId;

    /**
     * @return The unique ID of the time slot.
     */
    public String getSlotId() { return slotId; }

    /**
     * @param slotId The unique ID to set.
     */
    public void setSlotId(String slotId) { this.slotId = slotId; }

    /**
     * @return The start date and time.
     */
    public Date getStartTime() { return startTime; }

    /**
     * @param startTime The start time to set.
     */
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    /**
     * @return The end date and time.
     */
    public Date getEndTime() { return endTime; }

    /**
     * @param endTime The end time to set.
     */
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    /**
     * @return The maximum number of students allowed in this slot.
     */
    public int getMaxCapacity()
    {
        return maxCapacity;
    }

    /**
     * @param maxCapacity The capacity to set.
     */
    public void setMaxCapacity(int maxCapacity)
    {
        this.maxCapacity = maxCapacity;
    }

    public String getTutorId() {
        return tutorId;
    }

    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
    }
}