package controller;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/employee")
public class EmployeeServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "list";

        switch(action) {
            case "add":
                request.getRequestDispatcher("addEmployee.jsp").forward(request, response);
                break;
            case "update":
                request.getRequestDispatcher("updateEmployee.jsp").forward(request, response);
                break;
            case "delete":
                deleteEmployee(request);
                response.sendRedirect("employee");
                break;
            case "restore":
                restoreEmployee(request);
                response.sendRedirect("employee?action=history");
                break;
            case "history":
                showDeletedHistory(request);
                request.getRequestDispatcher("deletedHistory.jsp").forward(request, response);
                break;
            default: // list
                listEmployees(request);
                request.getRequestDispatcher("dashboard.jsp").forward(request, response);
                break;
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("add".equals(action)) {
            addEmployee(request);
        } else if ("update".equals(action)) {
            updateEmployee(request);
        } else if ("restore".equals(action)) {
            restoreEmployee(request);  // <-- call restore method
        }
        response.sendRedirect("employee");
    }

    private void listEmployees(HttpServletRequest request) {
        List<String[]> employees = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM employees");
             ResultSet rs = ps.executeQuery()) {

//            while(rs.next()) {
                String[] row = new String[5];
                row[0] = String.valueOf(rs.getInt("EmpID"));
                row[1] = rs.getString("Name");
                row[2] = String.valueOf(rs.getDouble("Salary"));
                row[3] = rs.getString("Department");
                row[4] = rs.getDate("JoiningDate").toString();
                employees.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
        request.setAttribute("employeeData", employees);
    }

    private void deleteEmployee(HttpServletRequest request) {
        int empId = Integer.parseInt(request.getParameter("id"));
        try (Connection con = DBConnection.getConnection()) {
            // Move to deleted_employees with timestamp
            PreparedStatement ps1 = con.prepareStatement(
                    "INSERT INTO deleted_employees (EmpID, Name, Salary, Department, JoiningDate, DeletedAt) " +
                            "SELECT EmpID, Name, Salary, Department, JoiningDate, NOW() FROM employees WHERE EmpID=?"
            );
            ps1.setInt(1, empId);
            ps1.executeUpdate();

            // Delete from active employees
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM employees WHERE EmpID=?");
            ps2.setInt(1, empId);
            ps2.executeUpdate();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void restoreEmployee(HttpServletRequest request) {
        int empId = Integer.parseInt(request.getParameter("id"));
        try (Connection con = DBConnection.getConnection()) {

            // Check if EmpID already exists in employees table
            PreparedStatement psCheck = con.prepareStatement(
                    "SELECT COUNT(*) FROM employees WHERE EmpID=?"
            );
            psCheck.setInt(1, empId);
            ResultSet rsCheck = psCheck.executeQuery();
            rsCheck.next();
            int count = rsCheck.getInt(1);

            if(count == 0) { // Safe to restore
                // Get deleted employee
                PreparedStatement psSelect = con.prepareStatement(
                        "SELECT * FROM deleted_employees WHERE EmpID=?"
                );
                psSelect.setInt(1, empId);
                ResultSet rs = psSelect.executeQuery();

                if(rs.next()) {
                    // Insert back into employees with same EmpID
                    PreparedStatement psInsert = con.prepareStatement(
                            "INSERT INTO employees (EmpID, Name, Salary, Department, JoiningDate) VALUES (?, ?, ?, ?, ?)"
                    );
                    psInsert.setInt(1, rs.getInt("EmpID"));
                    psInsert.setString(2, rs.getString("Name"));
                    psInsert.setDouble(3, rs.getDouble("Salary"));
                    psInsert.setString(4, rs.getString("Department"));
                    psInsert.setDate(5, rs.getDate("JoiningDate"));
                    psInsert.executeUpdate();

                    // Remove from deleted_employees
                    PreparedStatement psDelete = con.prepareStatement(
                            "DELETE FROM deleted_employees WHERE EmpID=?"
                    );
                    psDelete.setInt(1, empId);
                    psDelete.executeUpdate();
                }
            } else {
                System.out.println("Cannot restore: Employee with ID " + empId + " already exists.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showDeletedHistory(HttpServletRequest request) {
        List<String[]> deleted = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM deleted_employees");
             ResultSet rs = ps.executeQuery()) {

            while(rs.next()) {
                String[] row = new String[6];
                row[0] = String.valueOf(rs.getInt("EmpID"));
                row[1] = rs.getString("Name");
                row[2] = String.valueOf(rs.getDouble("Salary"));
                row[3] = rs.getString("Department");
                row[4] = rs.getDate("JoiningDate").toString();
                row[5] = rs.getTimestamp("DeletedAt").toString();
                deleted.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
        request.setAttribute("deletedData", deleted);
    }

    private void addEmployee(HttpServletRequest request) {
        String name = request.getParameter("name");
        double salary = Double.parseDouble(request.getParameter("salary"));
        String dept = request.getParameter("department");
        Date joining = Date.valueOf(request.getParameter("joiningDate"));
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO employees (Name, Salary, Department, JoiningDate) VALUES (?,?,?,?)")) {

            ps.setString(1, name);
            ps.setDouble(2, salary);
            ps.setString(3, dept);
            ps.setDate(4, joining);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateEmployee(HttpServletRequest request) {
        int empId = Integer.parseInt(request.getParameter("id"));
        String name = request.getParameter("name");
        double salary = Double.parseDouble(request.getParameter("salary"));
        String dept = request.getParameter("department");
        Date joining = Date.valueOf(request.getParameter("joiningDate"));

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE employees SET Name=?, Salary=?, Department=?, JoiningDate=? WHERE EmpID=?")) {

            ps.setString(1, name);
            ps.setDouble(2, salary);
            ps.setString(3, dept);
            ps.setDate(4, joining);
            ps.setInt(5, empId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
