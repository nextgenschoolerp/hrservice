package com.kvs.hr.config;

/**
 * RBAC authorization expressions for Module 07 HR, Biometric Attendance & Payroll endpoints.
 * Stories: US-07000 to US-07019.
 *
 * The BRD role names (FACULTY_MEMBER, ADMINISTRATIVE_STAFF, SCHOOL_PRINCIPAL) are kept, and the role codes that
 * authservice actually issues to school staff are added next to them: teachers are SUBJECT_TEACHER / CLASS_TEACHER /
 * HOUSE_MASTER / TEACHER, office staff ADMIN_CLERK / OFFICE_CLERK, principals PRINCIPAL / SCHOOL_PRINCIPAL / VICE_PRINCIPAL.
 */
public final class HrRoles {

    private HrRoles() {
    }

    /** Teaching staff: BRD name + authservice role codes */
    private static final String TEACHING = "'FACULTY_MEMBER', 'TEACHER', 'SUBJECT_TEACHER', 'CLASS_TEACHER', 'HOUSE_MASTER'";

    /** Office / administrative staff */
    private static final String OFFICE = "'ADMINISTRATIVE_STAFF', 'ADMIN_CLERK', 'OFFICE_CLERK', 'STAFF'";

    /** School leadership */
    private static final String PRINCIPALS = "'SCHOOL_PRINCIPAL', 'PRINCIPAL', 'VICE_PRINCIPAL'";

    /** Every school staff role (no parents or students): may read the staff directory */
    private static final String ALL_HR_STAFF = "'HR_MANAGER', 'PAYROLL_ACCOUNTANT', " + PRINCIPALS + ", " + OFFICE + ", " + TEACHING + ", "
            + "'COMPLIANCE_OFFICER', 'DEPARTMENT_HEAD', 'AUDITOR', 'TRUSTEE', 'IT_SYSTEM_ADMIN', 'SECURITY_AUDITOR', "
            + "'FINANCE_OFFICER', 'FINANCE_CASHIER', 'CHIEF_ACCOUNTANT', 'FINANCE_CONTROLLER', 'ACCOUNTS_CLERK', 'FINANCE_AUDITOR', "
            + "'ADMISSION_OFFICER', 'FRONT_OFFICE_COUNSELOR', 'REGISTRAR', 'ENTRANCE_EXAM_EVALUATOR', 'MARKETING_MANAGER', "
            + "'SCHOOL_NURSE', 'ALUMNI_COORDINATOR', 'EXAM_CONTROLLER', 'ACADEMIC_AUDITOR', 'ACADEMIC_COORDINATOR', "
            + "'TIMETABLE_INCHARGE', 'SUBSTITUTION_INCHARGE', 'ELEARNING_ADMIN', 'SUPER_ADMIN'";

    /**
     * Roles that see an employee's pay and identity fields (salary, PAN, Aadhaar, PF/ESI numbers, bank account).
     * Everyone else gets the directory fields only, except on their own profile.
     */
    public static final String[] CONFIDENTIAL_VIEWERS = {
            "HR_MANAGER", "PAYROLL_ACCOUNTANT", "SCHOOL_PRINCIPAL", "PRINCIPAL", "SUPER_ADMIN"};

    /** Roles that may act on another employee's leave, CPD and self-service records */
    public static final String[] HR_OFFICERS = {
            "HR_MANAGER", "PAYROLL_ACCOUNTANT", "SCHOOL_PRINCIPAL", "PRINCIPAL", "VICE_PRINCIPAL", "DEPARTMENT_HEAD", "SUPER_ADMIN"};

    /** View permission for staff directory, summaries, and self service */
    public static final String STAFF_VIEW = "hasAnyRole(" + ALL_HR_STAFF + ")";

    /** US-07000: Maintain employee master records, qualifications, contracts */
    public static final String EMPLOYEE_MANAGE = "hasAnyRole('HR_MANAGER', 'SCHOOL_PRINCIPAL', 'PRINCIPAL', 'SUPER_ADMIN')";

    /** US-07001: Ingest biometric/RFID punch logs */
    public static final String BIOMETRIC_INGEST = "hasAnyRole('PAYROLL_ACCOUNTANT', 'HR_MANAGER', " + OFFICE + ", 'SUPER_ADMIN')";

    /** US-07002: Shift roster planning and flexible working hour rules */
    public static final String SHIFT_ROSTER = "hasAnyRole('HR_MANAGER', " + PRINCIPALS + ", " + OFFICE + ", 'SUPER_ADMIN')";

    /** US-07003: Submit digital leave applications (any staff member, for themselves; HR officers for anyone) */
    public static final String LEAVE_APPLY = STAFF_VIEW;

    /** US-07004: Approve or reject staff leave requests */
    public static final String LEAVE_APPROVE = "hasAnyRole('HR_MANAGER', 'DEPARTMENT_HEAD', " + PRINCIPALS + ", 'SUPER_ADMIN')";

    /** US-07005: Gross-to-net payroll calculation */
    public static final String PAYROLL_CALCULATE = "hasAnyRole('PAYROLL_ACCOUNTANT', 'HR_MANAGER', 'SUPER_ADMIN')";

    /** US-07006: Unexcused absence LWP auto-deduction */
    public static final String LWP_DEDUCTION = "hasAnyRole('PAYROLL_ACCOUNTANT', 'HR_MANAGER', 'DEPARTMENT_HEAD', 'SUPER_ADMIN')";

    /** US-07007: Statutory PF, ESI, TDS monthly return files */
    public static final String STATUTORY_REPORTS = "hasAnyRole('PAYROLL_ACCOUNTANT', 'AUDITOR', 'HR_MANAGER', 'COMPLIANCE_OFFICER', 'SUPER_ADMIN')";

    /** US-07008: Password-protected encrypted PDF payslip generation & distribution */
    public static final String PAYSLIP_DISTRIBUTE = "hasAnyRole('HR_MANAGER', 'PAYROLL_ACCOUNTANT', 'SUPER_ADMIN')";

    /** US-07009: Log CPD training hours and credits */
    public static final String CPD_MANAGE = "hasAnyRole(" + TEACHING + ", 'STAFF', 'HR_MANAGER', " + PRINCIPALS + ", 'SUPER_ADMIN')";

    /** US-07010: Track probation completion and schedule appraisal reviews */
    public static final String PROBATION_APPRAISAL = "hasAnyRole('HR_MANAGER', 'SCHOOL_PRINCIPAL', 'PRINCIPAL', 'DEPARTMENT_HEAD', 'SUPER_ADMIN')";

    /** US-07011: Gratuity, leave encashment & full/final settlement */
    public static final String OFFBOARDING_SETTLEMENT = "hasAnyRole('PAYROLL_ACCOUNTANT', 'HR_MANAGER', 'SUPER_ADMIN')";

    /** US-07012: Export monthly salary disbursement statements to bank bulk payout portals */
    public static final String BANK_DISBURSEMENT = "hasAnyRole('PAYROLL_ACCOUNTANT', 'ADMINISTRATIVE_STAFF', 'HR_MANAGER', 'SUPER_ADMIN')";

    /** US-07013: Department-wise staff attendance summaries and principal dashboard */
    public static final String ATTENDANCE_ANALYTICS = "hasAnyRole(" + PRINCIPALS + ", 'COMPLIANCE_OFFICER', 'HR_MANAGER', 'DEPARTMENT_HEAD', 'SUPER_ADMIN')";

    /** US-07014: Configure Income Tax TDS slab rates and process employee tax declarations */
    public static final String TAX_DECLARATION = "hasAnyRole(" + TEACHING + ", " + OFFICE + ", 'HR_MANAGER', 'PAYROLL_ACCOUNTANT', 'SUPER_ADMIN')";

    /** US-07015: Flag biometric punch anomalies and manage regularisation */
    public static final String ANOMALY_REGULARISE = "hasAnyRole('HR_MANAGER', 'AUDITOR', " + TEACHING + ", " + OFFICE + ", 'SUPER_ADMIN')";

    /** US-07016: Manage resignation notices and generate official clearance NOCs */
    public static final String RESIGNATION_CLEARANCE = "hasAnyRole('HR_MANAGER', 'SCHOOL_PRINCIPAL', 'PRINCIPAL', " + TEACHING + ", " + OFFICE + ", 'SUPER_ADMIN')";

    /** US-07017: Track maternity, medical and statutory leave quotas */
    public static final String STATUTORY_QUOTAS = "hasAnyRole('HR_MANAGER', 'PAYROLL_ACCOUNTANT', 'COMPLIANCE_OFFICER', 'SUPER_ADMIN')";

    /** US-07018: Generate annual staff cost and payroll budget forecasts */
    public static final String PAYROLL_FORECAST = "hasAnyRole('SCHOOL_PRINCIPAL', 'PRINCIPAL', 'HR_MANAGER', 'PAYROLL_ACCOUNTANT', 'SUPER_ADMIN')";

    /** US-07019: Archive retired staff employment records for statutory audit compliance */
    public static final String ARCHIVE_MANAGE = "hasAnyRole('HR_MANAGER', 'COMPLIANCE_OFFICER', 'AUDITOR', 'SUPER_ADMIN')";
}
