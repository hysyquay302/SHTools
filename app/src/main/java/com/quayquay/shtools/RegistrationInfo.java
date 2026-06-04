package com.quayquay.shtools;
import com.google.gson.annotations.SerializedName;
public class RegistrationInfo {
    public enum Gender {
        @SerializedName("None")
        NONE,

        @SerializedName("Female")
        FEMALE,

        @SerializedName("Male")
        MALE
    }

    // 2. Chuyển toàn bộ các thuộc tính sang private
    private String Firstname;
    private String Lastname;
    private int DayOfBirth;
    private int MonthOfBirth;
    private int YearOfBirth;
    private int Age;
    @SerializedName("Gender")
    private Gender gender;

    private int ChildrenCount;
    private String ChildrenFirstname1;
    private String ChildrenLastname1;
    @SerializedName("childrenGender1")
    private Gender childrenGender1;
    private int ChildrenDayOfBirth1;
    private int ChildrenMonthOfBirth1;
    private int ChildrenYearOfBirth1;
    private int ChildrenAge1;

    private String ChildrenFirstname2;
    private String ChildrenLastname2;
    @SerializedName("childrenGender2")
    private Gender childrenGender2;
    private int ChildrenDayOfBirth2;
    private int ChildrenMonthOfBirth2;
    private int ChildrenYearOfBirth2;
    private int ChildrenAge2;

    private int ZipCode;
    private String Province;
    private String Education;
    private String ChildrenInfo;
    private String LinhVucNghe;
    private String ChucDanh;
    private String KhuVuc;
    private String Emails;

    // =========================================
    // 3. GETTER VÀ SETTER
    // =========================================

    public String getFirstname() { return Firstname; }
    public void setFirstname(String firstname) { Firstname = firstname; }

    public String getLastname() { return Lastname; }
    public void setLastname(String lastname) { Lastname = lastname; }

    public int getDayOfBirth() { return DayOfBirth; }
    public void setDayOfBirth(int dayOfBirth) { DayOfBirth = dayOfBirth; }

    public int getMonthOfBirth() { return MonthOfBirth; }
    public void setMonthOfBirth(int monthOfBirth) { MonthOfBirth = monthOfBirth; }

    public int getYearOfBirth() { return YearOfBirth; }
    public void setYearOfBirth(int yearOfBirth) { YearOfBirth = yearOfBirth; }

    public int getAge() { return Age; }
    public void setAge(int age) { Age = age; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public int getChildrenCount() { return ChildrenCount; }
    public void setChildrenCount(int childrenCount) { ChildrenCount = childrenCount; }

    public String getChildrenFirstname1() { return ChildrenFirstname1; }
    public void setChildrenFirstname1(String childrenFirstname1) { ChildrenFirstname1 = childrenFirstname1; }

    public String getChildrenLastname1() { return ChildrenLastname1; }
    public void setChildrenLastname1(String childrenLastname1) { ChildrenLastname1 = childrenLastname1; }

    public Gender getChildrenGender1() { return childrenGender1; }
    public void setChildrenGender1(Gender childrenGender1) { this.childrenGender1 = childrenGender1; }

    public int getChildrenDayOfBirth1() { return ChildrenDayOfBirth1; }
    public void setChildrenDayOfBirth1(int childrenDayOfBirth1) { ChildrenDayOfBirth1 = childrenDayOfBirth1; }

    public int getChildrenMonthOfBirth1() { return ChildrenMonthOfBirth1; }
    public void setChildrenMonthOfBirth1(int childrenMonthOfBirth1) { ChildrenMonthOfBirth1 = childrenMonthOfBirth1; }

    public int getChildrenYearOfBirth1() { return ChildrenYearOfBirth1; }
    public void setChildrenYearOfBirth1(int childrenYearOfBirth1) { ChildrenYearOfBirth1 = childrenYearOfBirth1; }

    public int getChildrenAge1() { return ChildrenAge1; }
    public void setChildrenAge1(int childrenAge1) { ChildrenAge1 = childrenAge1; }

    public String getChildrenFirstname2() { return ChildrenFirstname2; }
    public void setChildrenFirstname2(String childrenFirstname2) { ChildrenFirstname2 = childrenFirstname2; }

    public String getChildrenLastname2() { return ChildrenLastname2; }
    public void setChildrenLastname2(String childrenLastname2) { ChildrenLastname2 = childrenLastname2; }

    public Gender getChildrenGender2() { return childrenGender2; }
    public void setChildrenGender2(Gender childrenGender2) { this.childrenGender2 = childrenGender2; }

    public int getChildrenDayOfBirth2() { return ChildrenDayOfBirth2; }
    public void setChildrenDayOfBirth2(int childrenDayOfBirth2) { ChildrenDayOfBirth2 = childrenDayOfBirth2; }

    public int getChildrenMonthOfBirth2() { return ChildrenMonthOfBirth2; }
    public void setChildrenMonthOfBirth2(int childrenMonthOfBirth2) { ChildrenMonthOfBirth2 = childrenMonthOfBirth2; }

    public int getChildrenYearOfBirth2() { return ChildrenYearOfBirth2; }
    public void setChildrenYearOfBirth2(int childrenYearOfBirth2) { ChildrenYearOfBirth2 = childrenYearOfBirth2; }

    public int getChildrenAge2() { return ChildrenAge2; }
    public void setChildrenAge2(int childrenAge2) { ChildrenAge2 = childrenAge2; }

    public int getZipCode() { return ZipCode; }
    public void setZipCode(int zipCode) { ZipCode = zipCode; }

    public String getProvince() { return Province; }
    public void setProvince(String province) { Province = province; }

    public String getEducation() { return Education; }
    public void setEducation(String education) { Education = education; }

    public String getChildrenInfo() { return ChildrenInfo; }
    public void setChildrenInfo(String childrenInfo) { ChildrenInfo = childrenInfo; }

    public String getLinhVucNghe() { return LinhVucNghe; }
    public void setLinhVucNghe(String linhVucNghe) { LinhVucNghe = linhVucNghe; }

    public String getChucDanh() { return ChucDanh; }
    public void setChucDanh(String chucDanh) { ChucDanh = chucDanh; }

    public String getKhuVuc() { return KhuVuc; }
    public void setKhuVuc(String khuVuc) { KhuVuc = khuVuc; }

    public String getEmails() { return Emails; }
    public void setEmails(String emails) { Emails = emails; }
}