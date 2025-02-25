import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class EmployeeScheduler {
    private JFrame frame;
    private JTextField nameField;
    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private HashMap<String, List<String>> employeeShifts;

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final String[] SHIFTS = {"Morning", "Afternoon", "Evening"};

    public EmployeeScheduler() {
        employeeShifts = new HashMap<>();

        // Setup main frame
        frame = new JFrame("Employee Scheduling System");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(3, 1));

        JLabel nameLabel = new JLabel("Employee Name:");
        nameField = new JTextField(15);
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);

        // Shift selection panel
        JPanel shiftPanel = new JPanel();
        shiftPanel.setLayout(new GridLayout(0, 3));
        JCheckBox[][] shiftCheckboxes = new JCheckBox[DAYS.length][SHIFTS.length];

        for (int i = 0; i < DAYS.length; i++) {
            for (int j = 0; j < SHIFTS.length; j++) {
                shiftCheckboxes[i][j] = new JCheckBox(DAYS[i] + " - " + SHIFTS[j]);
                shiftPanel.add(shiftCheckboxes[i][j]);
            }
        }

        inputPanel.add(shiftPanel);

        JButton submitButton = new JButton("Submit Preferences");
        JButton generateButton = new JButton("Generate Schedule");
        inputPanel.add(submitButton);
        inputPanel.add(generateButton);

        frame.add(inputPanel, BorderLayout.NORTH);

        // Schedule Table
        tableModel = new DefaultTableModel();
        tableModel.addColumn("Employee");
        tableModel.addColumn("Assigned Shifts");
        scheduleTable = new JTable(tableModel);
        frame.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);

        // Button Actions
        submitButton.addActionListener(e -> savePreferences(shiftCheckboxes));
        generateButton.addActionListener(e -> generateSchedule());

        frame.setVisible(true);
    }

    private void savePreferences(JCheckBox[][] checkboxes) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter an employee name.");
            return;
        }

        List<String> selectedShifts = new ArrayList<>();
        for (JCheckBox[] day : checkboxes) {
            for (JCheckBox shift : day) {
                if (shift.isSelected()) {
                    selectedShifts.add(shift.getText());
                }
            }
        }

        if (selectedShifts.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select at least one shift.");
            return;
        }

        employeeShifts.put(name, selectedShifts);
        JOptionPane.showMessageDialog(frame, "Shift preferences saved for " + name);
        nameField.setText("");
    }

    private void generateSchedule() {
        tableModel.setRowCount(0);
        HashMap<String, List<String>> finalSchedule = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : employeeShifts.entrySet()) {
            String employee = entry.getKey();
            List<String> shifts = entry.getValue();
            Collections.shuffle(shifts); // Randomize to handle conflicts

            List<String> assignedShifts = new ArrayList<>();
            int daysWorked = 0;

            for (String shift : shifts) {
                if (daysWorked < 5 && !isShiftFull(finalSchedule, shift)) {
                    assignedShifts.add(shift);
                    finalSchedule.computeIfAbsent(shift, k -> new ArrayList<>()).add(employee);
                    daysWorked++;
                }
            }

            tableModel.addRow(new Object[]{employee, String.join(", ", assignedShifts)});
        }
    }

    private boolean isShiftFull(HashMap<String, List<String>> schedule, String shift) {
        return schedule.getOrDefault(shift, new ArrayList<>()).size() >= 2;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EmployeeScheduler::new);
    }
}
