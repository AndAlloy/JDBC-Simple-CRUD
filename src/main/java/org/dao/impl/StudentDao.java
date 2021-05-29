package org.dao.impl;

import org.model.Student;

public class StudentDao extends AbstractDaoImpl<Student> {
    public StudentDao() {
        super(Student.class);
    }
}
