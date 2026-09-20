package com.shareshelf.model;

public class Student {
    private String studentId, name, branch, email, hostelOrDay, password, whatsappNumber;
    private int year;

    public Student() {}

    public String getStudentId() 
    { 
        return studentId; 
    }
    public void setStudentId(String studentId) 
    { 
        this.studentId = studentId; 
    }
    public String getName() 
    { 
        return name; 
    }
    public void setName(String name) 
    { 
        this.name = name; 
    }
    public String getBranch() 
    { 
        return branch; 
    }
    public void setBranch(String branch) 
    { 
        this.branch = branch; 
    }
    public int getYear() 
    { 
        return year; 
    }
    public void setYear(int year) 
    { 
        this.year = year; 
    }
    public String getEmail() 
    { 
        return email; 
    }
    public void setEmail(String email) 
    { 
        this.email = email; 
    }
    public String getHostelOrDay() 
    { 
        return hostelOrDay; 
    }
    public void setHostelOrDay(String hostelOrDay) 
    { 
        this.hostelOrDay = hostelOrDay; 
    }
    public String getPassword() 
    { 
        return password; 
    }
    public void setPassword(String password) 
    {
         this.password = password; 
     }
    public String getWhatsappNumber() 
    {
         return whatsappNumber; 
 }
    public void setWhatsappNumber(String whatsappNumber) 
    { 
        this.whatsappNumber = whatsappNumber; 
    }

}