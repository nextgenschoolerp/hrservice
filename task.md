# Task Execution Record: Module 07 — Staff HR, Biometric Attendance & Payroll

Execution of the `BRD_API_Generate` skill for **Module 07** based on [`story/module7/School_ERP_Module_07_HR_Payroll_1000.md`](../story/module7/School_ERP_Module_07_HR_Payroll_1000.md), [`module7.sql`](../story/module7/module7.sql) and [`module7.md`](../story/module7/module7.md).

---

## 1. Input Specification & Target Microservice Resolution

- **User Stories**: `US-07000` – `US-07019`, all 20 base stories (each scenario 1 of the 50 rotating variations representing the full 1,000 story backlog)
- **Module**: `07` — Staff HR, Biometric Attendance & Payroll Operations
- **Target Microservice**: `hrservice`
- **Base Package**: `com.kvs.hr`
- **Allocated Server Port**: `8088`, context path `/hrservice`, test port `18088`
- **API Base Path**: `/api/v1/hr`
- **Database**: shared PostgreSQL database `authservice`, tables created by Hibernate `ddl-auto: update`
- **Service Lifecycle Status**:
  - [x] New Microservice (scaffolded via Step 0)
- **Reference Architecture**: `references/java-patterns.md`, `references/microservice-scaffolding-guide.md`, `references/seed-data-guide.md`

---

## 2. Requirement Extraction (from BRD)

| Story | Sub-module (BRD) | Primary Actor → Spring Role | Core Action |
|---|---|---|---|
| US-07000 | Employee Master Profile, Qualifications & Document Vault | HR Manager → `ROLE_HR_MANAGER` | Maintain employee master records, qualifications, and contract terms |
| US-07001 | Biometric/RFID Hardware Terminal API Integration Engine | Payroll Accountant → `ROLE_PAYROLL_ACCOUNTANT` | Ingest daily staff entry logs in real time from biometric scanners |
| US-07002 | Shift Roster Planning & Flexible Working Hour Rules | School Principal → `ROLE_SCHOOL_PRINCIPAL` | Configure flexible shift rosters and assign staff working schedules |
| US-07003 | Mobile Staff Leave Application & Multi-Level Approval Workflow | Faculty Member → `ROLE_FACULTY_MEMBER` | Submit digital leave applications with proxy coverage notes |
| US-07004 | Leave Entitlement Quotas (Earned, Casual, Sick, LWP) | Administrative Staff / HR → `ROLE_HR_MANAGER` | Approve or reject staff leave requests through automated administrative queues |
| US-07005 | Gross-to-Net Payroll Calculation Engine | Payroll Accountant → `ROLE_PAYROLL_ACCOUNTANT` | Calculate monthly gross-to-net salary figures with automated statutory deductions |
| US-07006 | Unexcused Absence Leave Without Pay (LWP) Auto-Deduction | Department Head / Accountant → `ROLE_PAYROLL_ACCOUNTANT` | Deduct unexcused absence leave without pay (LWP) automatically from monthly pay |
| US-07007 | Statutory PF, ESI, TDS Tax Deduction & E-Return Reports | Auditor / Accountant → `ROLE_AUDITOR` | Generate statutory Provident Fund (PF) and ESI monthly return files |
| US-07008 | Encrypted PDF Payslip Generation & E-Distribution | HR Manager → `ROLE_HR_MANAGER` | Generate and dispatch password-protected PDF payslips directly to staff |
| US-07009 | Continuous Professional Development (CPD) Credit Tracker | Faculty Member / HR → `ROLE_FACULTY_MEMBER` | Log staff Continuous Professional Development (CPD) training hours and credits |
| US-07010 | Employee Master Profile (Probation & Appraisal) | School Principal / HR → `ROLE_SCHOOL_PRINCIPAL` | Track employee probation completion dates and schedule appraisal reviews |
| US-07011 | Biometric/RFID Integration (Staff Offboarding & Gratuity) | Faculty Member / Accountant → `ROLE_PAYROLL_ACCOUNTANT` | Calculate gratuity and leave encashment values during staff offboarding |
| US-07012 | Shift Roster Planning (Bank Salary Disbursement) | Administrative Staff / Accountant → `ROLE_PAYROLL_ACCOUNTANT` | Export monthly salary disbursement statements to bank bulk payout portals |
| US-07013 | Mobile Staff Leave (Principal Attendance Dashboard) | Compliance Officer / Principal → `ROLE_SCHOOL_PRINCIPAL` | Monitor department-wise staff attendance summaries on executive dashboards |
| US-07014 | Leave Quotas (Income Tax TDS Slabs & Declarations) | Department Head / Accountant → `ROLE_PAYROLL_ACCOUNTANT` | Configure Income Tax TDS slab rates and process employee Tax Declarations |
| US-07015 | Unexcused Absence (Biometric Punch Anomalies) | Auditor / HR Manager → `ROLE_HR_MANAGER` | Flag staff biometric punch anomalies for HR verification and regularisation |
| US-07016 | Gross-to-Net Payroll (Resignation & Clearance NOC) | HR Manager → `ROLE_HR_MANAGER` | Manage staff resignation notices and generate official multi-department clearance NOCs |
| US-07017 | Statutory PF/ESI (Maternity & Medical Leave Quotas) | Payroll Accountant / HR → `ROLE_HR_MANAGER` | Track staff maternity and medical leave quotas compliant with statutory labor laws |
| US-07018 | Encrypted PDF Payslips (Annual Payroll Budget Forecasting) | School Principal / HR → `ROLE_SCHOOL_PRINCIPAL` | Generate annual staff cost and payroll budget forecasts for management |
| US-07019 | CPD Credit Tracker (Retired Staff Archival Vault) | Faculty Member / HR → `ROLE_HR_MANAGER` | Archive retired staff employment records for statutory audit compliance |

---

## 3. Implementation Checklist

- [x] Step 0: Microservice Scaffolding & Initial Build Verification
- [x] Step 1: Domain Entities & Database Constraints
- [x] Step 2: Request & Response DTOs
- [x] Step 3: Repository Interfaces
- [x] Step 4: Service Layer & Business Logic
- [x] Step 5: Controllers & REST Endpoints
- [x] Step 6: Integration Test Suite (0 failures)
- [x] Step 7: Postman Collection (`postman/School_ERP_HrService.postman_collection.json`)
- [x] Step 8: API Wiki Documentation (`wiki/api-docs/School_ERP_Module_07_HR_Payroll_API_Documentation.md`)
- [x] Step 9: Re-runnable Seed Data (`src/main/resources/db/seed/module07_hr_payroll_seed.sql`)
