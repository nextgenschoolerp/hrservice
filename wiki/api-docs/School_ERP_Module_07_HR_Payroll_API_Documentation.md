# School ERP - Module 07: Staff HR, Biometric Attendance & Payroll - API Documentation

{info}
Service `hrservice` · package `com.kvs.hr` · port `8088` (test `18088`) · base URL `http://localhost:8088/hrservice/api/v1/hr`.
Shared PostgreSQL database `authservice`; manages employee master profiles, biometric hardware terminal integration, flexible shifts, digital leaves, gross-to-net statutory payroll, and audit trails.
{info}

h2. Conventions

* *Base path* `/api/v1/hr`.
* *Authentication*: `Authorization: Bearer <authservice token>`. The token's `tenantId` claim isolates institutional data; `userId` identifies staff and faculty members.
* *Envelope*: success `{ success: true, message, data, traceId, timestamp }`; error `{ success: false, status, errorCode, message, path, traceId }`. `X-Trace-Id` is echoed in headers and payload.
* *Errors*: `400 VALIDATION_FAILED` / `MALFORMED_REQUEST`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 RESOURCE_NOT_FOUND`, `409 Conflict`.
* *Roles*: Enforced via Spring Security `@PreAuthorize(HrRoles.<RULE>)`. `SUPER_ADMIN` has global tenant-crossing override.

h2. Roles Matrix (BRD Actor → Spring Role)

||BRD Actor||Role||Description||
|HR Manager|`ROLE_HR_MANAGER`|Staff master profiles, probation appraisals, resignations, NOCs, and leaves|
|Payroll Accountant|`ROLE_PAYROLL_ACCOUNTANT`|Gross-to-net payroll, statutory PF/ESI, tax declarations, and bank payout batches|
|School Principal|`ROLE_SCHOOL_PRINCIPAL`|Executive attendance summaries, shift rosters, appraisal confirmations, budget forecasts|
|Faculty Member / Teacher|`ROLE_FACULTY_MEMBER`|Self-service leave application, CPD credit logging, payslip viewing|
|Administrative Staff|`ROLE_ADMINISTRATIVE_STAFF`|Department clearance, shift coordination, biometric anomaly regularisation|
|Auditor|`ROLE_AUDITOR`|Statutory tax returns, PF/ESI audit records, archival dossiers|
|Compliance Officer|`ROLE_COMPLIANCE_OFFICER`|Maternity/medical statutory quotas, labour law compliance, retention policies|

---

{expand:title=US-07000: Maintain employee master records, qualification certificates, and contract terms}
*Primary Actor*: HR Manager (`ROLE_HR_MANAGER`)

Manages full lifecycle employee master records: personal information, educational qualifications, employment contract type, statutory identification numbers (PAN, Aadhaar, PF UAN, ESI), and bank disbursement details.

*Business Rules*
* `employeeCode` must be unique across the tenant (`409 Conflict` if duplicate).
* `basicSalary` must be positive.
* Newly registered staff start with `probationStatus = ON_PROBATION` and `employmentStatus = ACTIVE`.
* Audit log entry `CREATE_EMPLOYEE` or `UPDATE_EMPLOYEE` is recorded.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/employees`|HR_MANAGER, SCHOOL_PRINCIPAL, SUPER_ADMIN|
|GET|`/api/v1/hr/employees/{staffId}`|All authenticated staff|
|GET|`/api/v1/hr/employees/by-code/{code}`|All authenticated staff|
|GET|`/api/v1/hr/employees`|All authenticated staff|
|PUT|`/api/v1/hr/employees/{staffId}`|HR_MANAGER, SCHOOL_PRINCIPAL, SUPER_ADMIN|

*Request Example (`POST /api/v1/hr/employees`)*
{code:language=json}
{
  "employeeCode": "DPS-EMP-1001",
  "firstName": "Rajesh",
  "lastName": "Sharma",
  "department": "Academics",
  "designation": "Senior Mathematics Teacher",
  "basicSalary": 55000.00,
  "joiningDate": "2024-06-01",
  "email": "rajesh.sharma@dps.edu.in",
  "phone": "+919811100001",
  "panNumber": "ABCDE1234F",
  "aadhaarNumber": "123456789012",
  "pfUan": "100904567801",
  "esiNumber": "3100456701",
  "bankName": "State Bank of India",
  "bankAccountNumber": "301234567801",
  "bankIfsc": "SBIN0001234",
  "qualifications": "M.Sc Mathematics, B.Ed",
  "contractTerms": "Permanent Full-Time"
}
{code}
{expand}

{expand:title=US-07001: Ingest daily staff entry logs in real time from biometric fingerprint scanners}
*Primary Actor*: Payroll Accountant (`ROLE_PAYROLL_ACCOUNTANT`), HR Staff

Ingests raw scan events from biometric hardware devices (fingerprint / RFID terminals) stationed at school gates and reception desks.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/attendance/scans`|PAYROLL_ACCOUNTANT, HR_MANAGER, ADMINISTRATIVE_STAFF, SUPER_ADMIN|
|GET|`/api/v1/hr/attendance/scans/{staffId}`|All authenticated staff|

*Request Example (`POST /api/v1/hr/attendance/scans`)*
{code:language=json}
{
  "staffId": "5eed0007-0001-4000-8000-000000000001",
  "terminalDeviceId": "DEVICE-GATE-01",
  "scanTimestamp": "2026-10-21T07:28:00Z",
  "scanType": "IN"
}
{code}
{expand}

{expand:title=US-07002: Apply flexible shift roster schedules for administrative and teaching personnel}
*Primary Actor*: School Principal (`ROLE_SCHOOL_PRINCIPAL`), HR Manager

Configures daily shift timing windows, grace periods, and half-day thresholds, and binds staff members to shifts.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/attendance/shifts`|HR_MANAGER, SCHOOL_PRINCIPAL, ADMINISTRATIVE_STAFF, SUPER_ADMIN|
|GET|`/api/v1/hr/attendance/shifts`|All authenticated staff|
|POST|`/api/v1/hr/attendance/shifts/assignments`|HR_MANAGER, SCHOOL_PRINCIPAL, ADMINISTRATIVE_STAFF, SUPER_ADMIN|

*Request Example (`POST /api/v1/hr/attendance/shifts`)*
{code:language=json}
{
  "shiftCode": "SHIFT-DPS-MORNING",
  "shiftName": "DPS North Regular Morning Shift",
  "startTime": "07:30:00",
  "endTime": "14:00:00",
  "graceMinutes": 15,
  "halfDayThresholdMinutes": 210
}
{code}
{expand}

{expand:title=US-07003: Submit digital leave applications via mobile apps with proxy coverage notes}
*Primary Actor*: Faculty Member / Staff (`ROLE_FACULTY_MEMBER`)

Enables teachers and staff to apply for digital leaves with date range calculations and proxy substitution notes.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/leaves`|All staff & faculty roles|
|GET|`/api/v1/hr/leaves/{applicationId}`|All authenticated staff|
|GET|`/api/v1/hr/leaves`|All authenticated staff|

*Request Example (`POST /api/v1/hr/leaves`)*
{code:language=json}
{
  "staffId": "5eed0007-0001-4000-8000-000000000001",
  "leaveType": "CASUAL",
  "startDate": "2026-11-10",
  "endDate": "2026-11-11",
  "reason": "Attending university graduation ceremony",
  "proxyNotes": "Period 2 & 4 covered by Ms. Kavya Nair"
}
{code}
{expand}

{expand:title=US-07004: Approve or reject staff leave requests through automated administrative queues}
*Primary Actor*: HR Manager (`ROLE_HR_MANAGER`), Department Head

Workflow queue for reviewing pending leave requests. Approving a non-LWP leave automatically decrements the staff member's annual quota balance.

||Method||Path||Allowed Roles||
|PUT|`/api/v1/hr/leaves/{applicationId}/review`|HR_MANAGER, DEPARTMENT_HEAD, SCHOOL_PRINCIPAL, SUPER_ADMIN|

*Request Example (`PUT /api/v1/hr/leaves/{id}/review`)*
{code:language=json}
{
  "status": "APPROVED",
  "reviewComments": "Approved with confirmed timetable proxy"
}
{code}
{expand}

{expand:title=US-07005: Calculate monthly gross-to-net salary figures with automated statutory deductions}
*Primary Actor*: Payroll Accountant (`ROLE_PAYROLL_ACCOUNTANT`)

Computes monthly salary: Gross = Basic + Allowances. Automated deductions: PF (12% of basic capped at 1800), ESI (0.75% if gross <= 21,000), TDS, and LWP deductions.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/payroll/calculate`|PAYROLL_ACCOUNTANT, HR_MANAGER, SUPER_ADMIN|
|GET|`/api/v1/hr/payroll/{payrollId}`|All authenticated staff|
|GET|`/api/v1/hr/payroll`|All authenticated staff|

*Response Example (`POST /api/v1/hr/payroll/calculate`)*
{code:language=json}
{
  "success": true,
  "message": "Payroll calculated successfully",
  "data": {
    "payrollId": "5eed0007-0007-4000-8000-000000000001",
    "staffId": "5eed0007-0001-4000-8000-000000000001",
    "payMonth": 10,
    "payYear": 2026,
    "grossSalary": 60000.00,
    "pfDeduction": 1800.00,
    "esiDeduction": 0.00,
    "tdsDeduction": 2500.00,
    "lwpDeduction": 0.00,
    "netSalary": 55700.00,
    "disbursementStatus": "PROCESSED"
  }
}
{code}
{expand}

{expand:title=US-07006: Deduct unexcused absence leave without pay (LWP) automatically from monthly pay}
*Primary Actor*: Payroll Accountant (`ROLE_PAYROLL_ACCOUNTANT`)

Reconciles unexcused absences and LWP leaves, computing per-day salary rate `basicSalary / daysInMonth` and calculating the total deduction.

||Method||Path||Allowed Roles||
|GET|`/api/v1/hr/payroll/lwp-deduction/{staffId}`|PAYROLL_ACCOUNTANT, HR_MANAGER, DEPARTMENT_HEAD, SUPER_ADMIN|
{expand}

{expand:title=US-07007: Generate statutory provident fund (PF) and ESI monthly return files}
*Primary Actor*: Auditor (`ROLE_AUDITOR`), Payroll Accountant

Exports compliance return data in government portal formats (EPFO Electronic Challan Return ECR and ESIC monthly contribution return).

||Method||Path||Allowed Roles||
|GET|`/api/v1/hr/payroll/statutory-returns`|PAYROLL_ACCOUNTANT, AUDITOR, HR_MANAGER, COMPLIANCE_OFFICER, SUPER_ADMIN|
{expand}

{expand:title=US-07008: Email password-protected PDF payslips directly to staff email addresses}
*Primary Actor*: HR Manager (`ROLE_HR_MANAGER`), Payroll Accountant

Generates encrypted, password-protected PDF salary slips and tracks email dispatch delivery records.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/payroll/payslips/dispatch`|HR_MANAGER, PAYROLL_ACCOUNTANT, SUPER_ADMIN|
|GET|`/api/v1/hr/payroll/payslips/{payrollId}/pdf`|All authenticated staff|
{expand}

{expand:title=US-07009: Log staff continuous professional development (CPD) training hours and credits}
*Primary Actor*: Faculty Member / Teacher (`ROLE_FACULTY_MEMBER`)

Tracks teacher training hours and credits completed under NEP 2020 annual professional development mandates.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/development/cpd`|FACULTY_MEMBER, TEACHER, STAFF, HR_MANAGER, SUPER_ADMIN|
|GET|`/api/v1/hr/development/cpd/{staffId}`|All authenticated staff|
{expand}

{expand:title=US-07010: Track employee probation completion dates and schedule appraisal reviews}
*Primary Actor*: School Principal (`ROLE_SCHOOL_PRINCIPAL`), HR Manager

Conducts formal probation appraisals, records performance ratings, and updates employee probation status to `CONFIRMED` or `EXTENDED`.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/employees/appraisals`|HR_MANAGER, SCHOOL_PRINCIPAL, DEPARTMENT_HEAD, SUPER_ADMIN|
|GET|`/api/v1/hr/employees/{staffId}/appraisals`|All authenticated staff|
{expand}

{expand:title=US-07011: Calculate gratuity and encashment values during staff offboarding}
*Primary Actor*: Payroll Accountant (`ROLE_PAYROLL_ACCOUNTANT`), HR Manager

Computes Full and Final Settlement (FnF): Gratuity `(15 * basic * completedYears) / 26` for staff with >= 5 years of service, plus earned leave encashment and notice pay adjustments.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/development/settlement`|PAYROLL_ACCOUNTANT, HR_MANAGER, SUPER_ADMIN|
|GET|`/api/v1/hr/development/settlement/{staffId}`|PAYROLL_ACCOUNTANT, HR_MANAGER, SUPER_ADMIN|
{expand}

{expand:title=US-07012: Export monthly salary disbursement statements to bank bulk payout portals}
*Primary Actor*: Payroll Accountant (`ROLE_PAYROLL_ACCOUNTANT`)

Generates bank-ready salary payout batch statements in standard NEFT/RTGS CSV format for automated bulk transfers.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/payroll/bank-payouts`|PAYROLL_ACCOUNTANT, ADMINISTRATIVE_STAFF, HR_MANAGER, SUPER_ADMIN|
|GET|`/api/v1/hr/payroll/bank-payouts`|PAYROLL_ACCOUNTANT, ADMINISTRATIVE_STAFF, HR_MANAGER, SUPER_ADMIN|
{expand}

{expand:title=US-07013: Monitor department-wise staff attendance summaries on principal dashboards}
*Primary Actor*: School Principal (`ROLE_SCHOOL_PRINCIPAL`), Compliance Officer

Provides real-time attendance analytics: total active staff, present count from biometric scans, attendance percentage, and department-wise headcounts.

||Method||Path||Allowed Roles||
|GET|`/api/v1/hr/attendance/summary`|SCHOOL_PRINCIPAL, VICE_PRINCIPAL, COMPLIANCE_OFFICER, HR_MANAGER, SUPER_ADMIN|
{expand}

{expand:title=US-07014: Configure income tax TDS slab rates and process employee tax declaration forms}
*Primary Actor*: Payroll Accountant (`ROLE_PAYROLL_ACCOUNTANT`), Staff

Submits Section 80C, 80D, and HRA tax declarations, computes estimated annual tax liability, and sets monthly TDS deduction amounts.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/payroll/tax-declarations`|All staff, HR, Accountant, SUPER_ADMIN|
|GET|`/api/v1/hr/payroll/tax-declarations/{staffId}`|All authenticated staff|
{expand}

{expand:title=US-07015: Flag staff biometric punch anomalies for HR verification and regularisation}
*Primary Actor*: HR Manager (`ROLE_HR_MANAGER`), Staff

Identifies punch anomalies (`MISSING_OUT_PUNCH`, `LATE_ARRIVAL`, `MISSED_SCAN`), allows staff to submit regularisation reasons, and tracks HR approvals.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/attendance/anomalies/{staffId}`|HR_MANAGER, AUDITOR, SUPER_ADMIN|
|PUT|`/api/v1/hr/attendance/anomalies/{anomalyId}/regularise`|HR_MANAGER, AUDITOR, SUPER_ADMIN|
|GET|`/api/v1/hr/attendance/anomalies`|All authenticated staff|
{expand}

{expand:title=US-07016: Manage staff resignation notices and generate official clearance NOCs}
*Primary Actor*: HR Manager (`ROLE_HR_MANAGER`)

Tracks resignation submissions, notice period dates, and multi-department clearance sign-offs (Library, Accounts, IT, Lab) leading to automated clearance NOC generation.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/employees/resignations`|HR_MANAGER, SCHOOL_PRINCIPAL, Staff, SUPER_ADMIN|
|PUT|`/api/v1/hr/employees/resignations/{resignationId}/clearance`|HR_MANAGER, SCHOOL_PRINCIPAL, SUPER_ADMIN|
|GET|`/api/v1/hr/employees/resignations/{resignationId}`|All authenticated staff|
{expand}

{expand:title=US-07017: Track staff maternity and medical leave quotas compliant with labor laws}
*Primary Actor*: HR Manager (`ROLE_HR_MANAGER`), Compliance Officer

Configures and tracks statutory leave quotas (Maternity 26 weeks / 180 days, Medical, Casual, Earned) per staff member and academic year.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/leaves/quotas`|HR_MANAGER, PAYROLL_ACCOUNTANT, COMPLIANCE_OFFICER, SUPER_ADMIN|
|GET|`/api/v1/hr/leaves/quotas`|All authenticated staff|
{expand}

{expand:title=US-07018: Generate annual staff cost and payroll budget forecasts for management}
*Primary Actor*: School Principal (`ROLE_SCHOOL_PRINCIPAL`), HR Manager

Simulates department-wise staff cost projections for upcoming fiscal years based on annual salary increment percentages and planned new faculty hires.

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/payroll/forecasts`|SCHOOL_PRINCIPAL, HR_MANAGER, PAYROLL_ACCOUNTANT, SUPER_ADMIN|
|GET|`/api/v1/hr/payroll/forecasts`|SCHOOL_PRINCIPAL, HR_MANAGER, PAYROLL_ACCOUNTANT, SUPER_ADMIN|
{expand}

{expand:title=US-07019: Archive retired staff employment records for statutory audit compliance}
*Primary Actor*: HR Manager (`ROLE_HR_MANAGER`), Compliance Officer

Stores immutable digital compliance archival dossiers of retired/relieved employees with configured statutory retention periods (e.g. 10 years).

||Method||Path||Allowed Roles||
|POST|`/api/v1/hr/employees/archives`|HR_MANAGER, COMPLIANCE_OFFICER, AUDITOR, SUPER_ADMIN|
|GET|`/api/v1/hr/employees/archives`|HR_MANAGER, COMPLIANCE_OFFICER, AUDITOR, SUPER_ADMIN|
{expand}
