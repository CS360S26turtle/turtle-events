package com.example.peertutoringmarketplace;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

//The following test is from ChatGPT, "Generate tests for user class", 2026-04-06
public class UserTest {

    private User user;

    @Before
    public void setUp() {
        user = new User("123", "test@email.com", "John Doe", "student");
    }

    @Test
    public void testConstructorInitialization() {
        assertEquals("123", user.getUserID());
        assertEquals("test@email.com", user.getEmail());
        assertEquals("John Doe", user.getFullName());
        assertEquals("student", user.getRole());
        assertEquals("pending", user.getVerificationStatus());

        assertNull(user.getStudentID());
        assertNull(user.getTutorID());
    }

    @Test
    public void testSetAndGetStudentID() {
        user.setStudentID("stu123");
        assertEquals("stu123", user.getStudentID());
    }

    @Test
    public void testSetAndGetTutorID() {
        user.setTutorID("tut456");
        assertEquals("tut456", user.getTutorID());
    }

    @Test
    public void testSubjectsList() {
        user.setSubjects(Arrays.asList("Math", "Physics"));
        assertEquals(2, user.getSubjects().size());
        assertTrue(user.getSubjects().contains("Math"));
    }

    @Test
    public void testSessionsList() {
        user.setSessions(Arrays.asList("session1", "session2"));
        assertEquals(2, user.getSessions().size());
    }

    @Test
    public void testVerificationStatus() {
        user.setVerificationStatus("accepted");
        assertEquals("accepted", user.getVerificationStatus());
    }
}