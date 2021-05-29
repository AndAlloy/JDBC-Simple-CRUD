package org.dao;

import java.util.List;

public interface AbstractDao<T> {
    void save(T obj);
    T findById(long id) throws IllegalAccessException;
    void update(T obj) throws IllegalAccessException;
    void delete(long id) throws IllegalAccessException;

}
