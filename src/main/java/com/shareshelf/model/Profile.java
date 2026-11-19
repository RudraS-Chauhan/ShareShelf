package com.shareshelf.model;

import java.util.ArrayList;
import java.util.List;

/** What other students may see about a student: name, branch, year, track record and revealed ratings. No contact details. */
public class Profile {

    /** One revealed rating with its comment. */
    public static class Review {
        private final int stars;
        private final String comment, raterName, role, whenText;

        public Review(int stars, String comment, String raterName, String role, String whenText) {
            this.stars = stars;
            this.comment = comment;
            this.raterName = raterName;
            this.role = role;
            this.whenText = whenText;
        }

        public int getStars() { return stars; }
        public String getComment() { return comment; }
        public String getRaterName() { return raterName; }
        /** "borrower" or "lender": the role of the person being reviewed. */
        public String getRole() { return role; }
        public String getWhenText() { return whenText; }
    }

    private String studentId, name, branch, hostelOrDay;
    private int year, completed, late, borrowerStarsN, lenderStarsN, listings;
    private double borrowerStars, lenderStars;
    private final List<Review> reviews = new ArrayList<>();

    public String getStudentId() { return studentId; }
    public void setStudentId(String v) { studentId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getBranch() { return branch; }
    public void setBranch(String v) { branch = v; }
    public String getHostelOrDay() { return hostelOrDay; }
    public void setHostelOrDay(String v) { hostelOrDay = v; }
    public int getYear() { return year; }
    public void setYear(int v) { year = v; }
    public int getCompleted() { return completed; }
    public void setCompleted(int v) { completed = v; }
    public int getLate() { return late; }
    public void setLate(int v) { late = v; }
    public int getListings() { return listings; }
    public void setListings(int v) { listings = v; }
    public double getBorrowerStars() { return borrowerStars; }
    public void setBorrowerStars(double v) { borrowerStars = v; }
    public int getBorrowerStarsN() { return borrowerStarsN; }
    public void setBorrowerStarsN(int v) { borrowerStarsN = v; }
    public double getLenderStars() { return lenderStars; }
    public void setLenderStars(double v) { lenderStars = v; }
    public int getLenderStarsN() { return lenderStarsN; }
    public void setLenderStarsN(int v) { lenderStarsN = v; }
    public List<Review> getReviews() { return reviews; }

    public String getBorrowerStarsText() { return borrowerStarsN == 0 ? "" : String.format(java.util.Locale.ENGLISH, "%.1f", borrowerStars); }
    public String getLenderStarsText() { return lenderStarsN == 0 ? "" : String.format(java.util.Locale.ENGLISH, "%.1f", lenderStars); }

    public String getBorrowerBadge() { return Request.badge("Reliable borrower", completed, late, borrowerStars, borrowerStarsN); }
    public String getLenderBadge() { return Request.badge("Trusted lender", completed, 0, lenderStars, lenderStarsN); }
}
