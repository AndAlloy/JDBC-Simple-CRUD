package org.dao;

import org.dao.impl.StudentDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.model.Student;

import java.lang.reflect.Field;
import java.sql.Date;

class SchoolDaoTest {

    @Test
    @DisplayName("Getting field names")
    void testGetFieldsNames() {
        Student student = new Student(1, "", "", null);
        Field[] declaredFields = student.getClass().getDeclaredFields();

        Assertions.assertNotNull(declaredFields);
    }

    @Test
    @DisplayName("Save and Find by ID")
    void testSave() {
        Student student = new Student(12,"Bohdan Petrov", "I-32", Date.valueOf("2000-12-12"));
        StudentDao studentDao = new StudentDao();
        try {
            studentDao.delete(12);
        } catch (Exception e) {
            studentDao.save(student);
        }
        studentDao.save(student);

        Assertions.assertNotNull(studentDao.findById(12));
    }

    @Test
    @DisplayName("Deleting object")
    void testDelete() {
        Student student = new Student(12,"Bohdan Petrov", "I-32", Date.valueOf("2000-12-12"));
        StudentDao studentDao = new StudentDao();
        try {
            studentDao.delete(12);
            studentDao.save(student);
        } catch (Exception e) {
            studentDao.delete(12);
        }
        studentDao.delete(12);

        Assertions.assertNull(studentDao.findById(12));
    }

    @Test
    void testUpdate() {
        Student student = new Student(12,"Bohdan Petrov", "I-32", Date.valueOf("2000-12-12"));
        StudentDao studentDao = new StudentDao();
        try {
            studentDao.delete(12);
        } catch (Exception e) {
            studentDao.save(student);
        }
        studentDao.save(student);
        student = new Student(12,"Bohdan Petrov", "I-32", Date.valueOf("2000-12-12"));
        studentDao.update(student);

        Assertions.assertNotEquals(student, studentDao.findById(12));

    }
}