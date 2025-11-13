import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LeaveManagementSystem {

    // 1. Merged fields from LeaveManager and main class
    private static Map<String, Employee> employees = new HashMap<>();
    private static Map<Integer, LeaveRequest> allRequests = new HashMap<>();
    private static AtomicInteger nextRequestId = new AtomicInteger(1);
    private static Scanner scanner = new Scanner(System.in);
    private static Employee currentEmployee = null;
    private static final String ADMIN_ID = "M201";

    // 2. Main method
    public static void main(String[] args) {
        setupInitialData();
        System.out.println("Welcome to the Leave Management System");
        while (true) {
            if (currentEmployee == null) showLoginMenu();
            else showMainMenu();
        }
    }

    // 3. Nested Data Classes
    static class Employee {
        String id, name;
        Map<LeaveType, Integer> balances = new HashMap<>();
        public Employee(String id, String name, int annual, int sick) {
            this.id = id; this.name = name;
            balances.put(LeaveType.ANNUAL, annual); balances.put(LeaveType.SICK, sick);
            balances.put(LeaveType.UNPAID, 999); balances.put(LeaveType.MATERNITY, 0);
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public int getLeaveBalance(LeaveType type) { return balances.getOrDefault(type, 0); }
        public Map<LeaveType, Integer> getLeaveBalances() { return new HashMap<>(balances); }
        public void deductLeave(LeaveType type, int days) {
            if (type != LeaveType.UNPAID) balances.put(type, getLeaveBalance(type) - days);
        }
        @Override public String toString() { return "Employee[ID=" + id + ", Name=" + name + "]"; }
    }

    static class LeaveRequest {
        int id, durationDays;
        Employee employee;
        LocalDate startDate, endDate;
        LeaveType type;
        RequestStatus status = RequestStatus.PENDING;
        String reason, reviewedBy, managerComment;
        public LeaveRequest(int id, Employee emp, LocalDate start, LocalDate end, int duration, LeaveType type, String reason) {
            this.id = id; this.employee = emp; this.startDate = start; this.endDate = end;
            this.durationDays = duration; this.type = type; this.reason = reason;
        }
        public int getId() { return id; }
        public Employee getEmployee() { return employee; }
        public LocalDate getStartDate() { return startDate; }
        public int getDurationDays() { return durationDays; }
        public LeaveType getType() { return type; }
        public RequestStatus getStatus() { return status; }
        public void setStatus(RequestStatus s, String mId, String c) { status = s; reviewedBy = mId; managerComment = c; }
        @Override public String toString() {
            String s = String.format("Req ID: %d (%s) | %s | %s to %s (%d days) | Status: %s",
                id, employee.getName(), type, startDate, endDate, durationDays, status);
            if (status != RequestStatus.PENDING) s += " | Reviewed by: " + reviewedBy;
            return s;
        }
    }

    // 4. Enums
    enum LeaveType { ANNUAL, SICK, UNPAID, MATERNITY }
    enum RequestStatus { PENDING, APPROVED, REJECTED }

    // 5. Core Logic Methods (merged from LeaveManager)
    private static void addEmployee(String id, String name, int annual, int sick) {
        employees.put(id, new Employee(id, name, annual, sick));
    }
    private static Employee getEmployee(String id) { return employees.get(id); }

    private static LeaveRequest submitLeaveRequest(Employee emp, LocalDate start, LocalDate end, LeaveType type, String reason) {
        int duration = (int) ChronoUnit.DAYS.between(start, end) + 1;
        if (duration <= 0) { System.out.println("Error: Invalid date range."); return null; }
        if (type != LeaveType.UNPAID && emp.getLeaveBalance(type) < duration) {
            System.out.printf("Error: Insufficient %s balance. Have: %d, Need: %d\n", type, emp.getLeaveBalance(type), duration);
            return null;
        }
        int id = nextRequestId.getAndIncrement();
        LeaveRequest req = new LeaveRequest(id, emp, start, end, duration, type, reason);
        allRequests.put(id, req);
        return req;
    }

    private static boolean reviewRequest(int reqId, boolean approve, String managerId) {
        LeaveRequest req = allRequests.get(reqId);
        if (req == null || req.getStatus() != RequestStatus.PENDING) return false;
        if (approve) {
            if (req.getType() != LeaveType.UNPAID && req.getEmployee().getLeaveBalance(req.getType()) < req.getDurationDays()) {
                req.setStatus(RequestStatus.REJECTED, managerId, "Insufficient balance at approval.");
            } else {
                if (req.getType() != LeaveType.UNPAID) req.getEmployee().deductLeave(req.getType(), req.getDurationDays());
                req.setStatus(RequestStatus.APPROVED, managerId, "Approved");
            }
        } else {
            req.setStatus(RequestStatus.REJECTED, managerId, "Rejected");
        }
        return true;
    }

    private static List<LeaveRequest> getRequestsForEmployee(String empId) {
        return allRequests.values().stream()
            .filter(r -> r.getEmployee().getId().equals(empId))
            .sorted(Comparator.comparing(LeaveRequest::getStartDate).reversed())
            .collect(Collectors.toList());
    }

    private static List<LeaveRequest> getPendingRequests() {
        return allRequests.values().stream()
            .filter(r -> r.getStatus() == RequestStatus.PENDING)
            .sorted(Comparator.comparing(LeaveRequest::getStartDate))
            .collect(Collectors.toList());
    }

    // 6. UI/CLI Methods
    private static void setupInitialData() {
        addEmployee("E101", "Alice Smith", 20, 10);
        addEmployee(ADMIN_ID, "Bob Johnson", 20, 10);
        addEmployee("E102", "Charlie Brown", 15, 5);
        try {
            LeaveRequest req1 = submitLeaveRequest(getEmployee("E101"), LocalDate.now().minusDays(20), LocalDate.now().minusDays(18), LeaveType.ANNUAL, "Vacation");
            if (req1 != null) reviewRequest(req1.getId(), true, ADMIN_ID);
            submitLeaveRequest(getEmployee("E102"), LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), LeaveType.SICK, "Flu");
        } catch (Exception e) { System.err.println("Error setting up data: " + e.getMessage()); }
    }

    private static void showLoginMenu() {
        System.out.print("\nPlease log in. Enter Employee ID (or 'exit'): ");
        String id = scanner.nextLine().trim();
        if (id.equalsIgnoreCase("exit")) System.exit(0);
        currentEmployee = getEmployee(id);
        if (currentEmployee == null) System.out.println("Error: Employee ID not found.");
        else System.out.println("Welcome, " + currentEmployee.getName() + "!");
    }

    private static void showMainMenu() {
        System.out.println("\n[Main Menu] 1.Submit Leave | 2.My History | 3.My Balances");
        if (currentEmployee.getId().equals(ADMIN_ID)) {
            System.out.println("--- Admin --- 4.Review Pending | 5.All Employees | 6.All Requests");
        }
        System.out.println("0. Log Out");
        try {
            int choice = Integer.parseInt(scanner.nextLine());
            boolean isAdmin = currentEmployee.getId().equals(ADMIN_ID);
            switch (choice) {
                case 1: handleSubmitLeave(); break;
                case 2: handleViewMyHistory(); break;
                case 3: handleViewMyBalances(); break;
                case 4: if (isAdmin) handleReviewRequests(); else System.out.println("Invalid choice."); break;
                case 5: if (isAdmin) handleViewAllEmployees(); else System.out.println("Invalid choice."); break;
                case 6: if (isAdmin) handleViewAllLeaveRequests(); else System.out.println("Invalid choice."); break;
                case 0: currentEmployee = null; System.out.println("Logging out..."); break;
                default: System.out.println("Invalid choice.");
            }
        } catch (NumberFormatException e) { System.out.println("Invalid input."); }
    }

    private static void handleSubmitLeave() {
        try {
            System.out.print("Enter Start Date (YYYY-MM-DD): ");
            LocalDate start = LocalDate.parse(scanner.nextLine());
            System.out.print("Enter End Date (YYYY-MM-DD): ");
            LocalDate end = LocalDate.parse(scanner.nextLine());
            System.out.print("Enter Type (ANNUAL, SICK, UNPAID, MATERNITY): ");
            LeaveType type = LeaveType.valueOf(scanner.nextLine().toUpperCase());
            System.out.print("Enter Reason (optional): ");
            String reason = scanner.nextLine();
            LeaveRequest req = submitLeaveRequest(currentEmployee, start, end, type, reason);
            if (req != null) System.out.println("Successfully submitted request ID: " + req.getId());
        } catch (Exception e) { System.out.println("Error submitting request: " + e.getMessage()); }
    }

    private static void handleViewMyHistory() {
        System.out.println("\n[My Leave History]");
        List<LeaveRequest> reqs = getRequestsForEmployee(currentEmployee.getId());
        if (reqs.isEmpty()) System.out.println("No leave requests found.");
        else reqs.forEach(System.out::println);
    }

    private static void handleViewMyBalances() {
        System.out.println("\n[My Leave Balances]");
        currentEmployee.getLeaveBalances().forEach((type, bal) -> System.out.printf("- %s: %d days\n", type, bal));
    }

    private static void handleReviewRequests() {
        System.out.println("\n[Pending Requests]");
        List<LeaveRequest> pending = getPendingRequests();
        if (pending.isEmpty()) { System.out.println("No pending requests."); return; }
        pending.forEach(System.out::println);
        try {
            System.out.print("\nEnter Request ID to review (0 to cancel): ");
            int reqId = Integer.parseInt(scanner.nextLine());
            if (reqId == 0) return;
            System.out.print("Do you (A)pprove or (R)eject? ");
            String action = scanner.nextLine().toUpperCase();
            if (action.equals("A") || action.equals("R")) {
                boolean success = reviewRequest(reqId, action.equals("A"), currentEmployee.getId());
                System.out.println("Request " + reqId + " processed: " + (success ? "Success" : "Failed"));
            } else System.out.println("Invalid action.");
        } catch (Exception e) { System.out.println("Invalid input."); }
    }

    private static void handleViewAllEmployees() {
        System.out.println("\n[All Employees]");
        employees.values().forEach(emp -> System.out.println(emp + " | Balances: " + emp.getLeaveBalances()));
    }

    private static void handleViewAllLeaveRequests() {
        System.out.println("\n[All Leave Requests]");
        if (allRequests.isEmpty()) System.out.println("No requests found.");
        else allRequests.values().stream()
            .sorted(Comparator.comparing(LeaveRequest::getStartDate))
            .forEach(System.out::println);
    }
}