from flask import Flask, render_template, request, redirect, url_for
import sqlite3
import random

app = Flask(__name__)

# Database setup
def init_db():
    with sqlite3.connect("schedule.db") as conn:
        cursor = conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS preferences (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id INTEGER,
                day TEXT,
                shift TEXT,
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS schedule (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id INTEGER,
                day TEXT,
                shift TEXT,
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
        """)
        conn.commit()

init_db()

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/submit', methods=['POST'])
def submit():
    name = request.form['name']
    preferences = request.form.getlist('preferences')
    
    with sqlite3.connect("schedule.db") as conn:
        cursor = conn.cursor()
        cursor.execute("INSERT INTO employees (name) VALUES (?)", (name,))
        employee_id = cursor.lastrowid
        
        for pref in preferences:
            day, shift = pref.split('-')
            cursor.execute("INSERT INTO preferences (employee_id, day, shift) VALUES (?, ?, ?)", (employee_id, day, shift))
        
        conn.commit()
    
    return redirect(url_for('index'))

@app.route('/generate_schedule')
def generate_schedule():
    with sqlite3.connect("schedule.db") as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT employee_id, day, shift FROM preferences")
        preferences = cursor.fetchall()
        
        schedule = {}
        assigned_counts = {}
        for emp_id, day, shift in preferences:
            if assigned_counts.get(emp_id, 0) < 5:
                if schedule.get((day, shift), []).count(emp_id) == 0:
                    schedule.setdefault((day, shift), []).append(emp_id)
                    assigned_counts[emp_id] = assigned_counts.get(emp_id, 0) + 1
        
        # Ensure minimum 2 employees per shift
        for day in ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]:
            for shift in ["Morning", "Afternoon", "Evening"]:
                if len(schedule.get((day, shift), [])) < 2:
                    available_employees = [emp_id for emp_id in assigned_counts if assigned_counts[emp_id] < 5]
                    while len(schedule.get((day, shift), [])) < 2 and available_employees:
                        chosen = random.choice(available_employees)
                        schedule.setdefault((day, shift), []).append(chosen)
                        assigned_counts[chosen] += 1

        # Save schedule to DB
        cursor.execute("DELETE FROM schedule")  # Clear old schedule
        for (day, shift), employees in schedule.items():
            for emp_id in employees:
                cursor.execute("INSERT INTO schedule (employee_id, day, shift) VALUES (?, ?, ?)", (emp_id, day, shift))
        conn.commit()
    
    return redirect(url_for('view_schedule'))

@app.route('/view_schedule')
def view_schedule():
    with sqlite3.connect("schedule.db") as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT e.name, s.day, s.shift
            FROM schedule s
            JOIN employees e ON s.employee_id = e.id
            ORDER BY s.day, s.shift
        """)
        schedule = cursor.fetchall()
    
    return render_template('schedule.html', schedule=schedule)

if __name__ == '__main__':
    app.run(debug=True)
