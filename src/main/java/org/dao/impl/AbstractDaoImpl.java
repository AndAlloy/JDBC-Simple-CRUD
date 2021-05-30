package org.dao.impl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.config.PostgresConnector;
import org.dao.AbstractDao;
import org.dao.annotation.Id;
import org.dao.exception.IdAnnotationNotPresentException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractDaoImpl<T> implements AbstractDao<T> {
    private static final Logger logger = LogManager.getLogger(AbstractDaoImpl.class);

    private final Class currentClass;

    public AbstractDaoImpl(Class clazz) {
        this.currentClass = clazz;
    }

    @Override
    public void save(T obj) {
        logger.info("Saving the object");
        List<String> rowValues = getValues(obj);
        List<Object> rawValues = getRawValues(obj);

        StringBuilder preparedValues = new StringBuilder();
        for (int i = 0; i < rowValues.size(); i++){
            preparedValues.append("?").append(",");
        }
        preparedValues.deleteCharAt(preparedValues.length() - 1);

        String query = "INSERT INTO "
                + getTableName()
                + " ( "
                + toLine(getFieldsNames(currentClass.getDeclaredFields()))
                + " ) VALUES ( "
                + preparedValues
                + ")";
        logger.debug(query);

        try (Connection connection = PostgresConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query))
        {
            int update = setPrepStatement(rawValues, rowValues, statement).executeUpdate();
            logger.info(String.format("%s row added!",  update));
        } catch (SQLException e) {
            logger.error("Error on saving the object", e);
        }
    }

    private String toLine(List<String> list) {
        logger.debug("Making line of vars in SB");
        StringBuilder sb = new StringBuilder();
        list.forEach(s -> sb.append(s).append(","));
        sb.deleteCharAt(sb.length() - 1);
        logger.debug(String.format("Got string line: %s", sb));
        return sb.toString();
    }



    private List<String> getValues(T obj) {
        logger.debug("Getting list of values from user");
        Field[] fields = currentClass.getDeclaredFields();
        return Arrays.stream(fields)
                .peek(f -> f.setAccessible(true))
                .map(f -> {
                    try {
                            return f.get(obj).toString();
                    } catch (IllegalAccessException e) {
                        logger.error("Error on getting values from user");
                    }
                    return null;
                }).collect(Collectors.toList());
    }


    public List<Object> getRawValues(T obj) {
        logger.debug("Getting class objects from user input");
        Field[] fields = currentClass.getDeclaredFields();
        return Arrays.stream(fields)
                .peek(f -> f.setAccessible(true))
                .map(f -> {
                    try {
                        return f.get(obj);
                    } catch (IllegalAccessException e) {
                        logger.error("Error on getting class objects from user input");
                    }
                    return null;
                }).collect(Collectors.toList());
    }

    @Override
    public T findById(long id) {
        logger.info("Looking for an object by ID");
        String query = "SELECT * FROM " + getTableName()
                + " WHERE " + getIdName() + " = ? ";
        try (Connection connection = PostgresConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, Math.toIntExact(id));
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                logger.info("Parsing the object");
                return parseResultSet(resultSet);
            }
            logger.debug("Nothing found");
        } catch (SQLException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException throwable) {
            logger.error("Error: object not found or something went wrong", throwable);
        }
        return null;
    }

    private String getTableName() {
        return currentClass.getSimpleName().toLowerCase();
    }

    private String getIdName() {
        logger.debug("Getting ID name");
        Field[] declaredFields = currentClass.getDeclaredFields();
        for (Field f : declaredFields) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(Id.class)) {
                return f.getName();
            }
        }
        logger.error("Annotation not found");
        throw new IdAnnotationNotPresentException();
    }

    private T parseResultSet(ResultSet resultSet) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, SQLException {
        logger.debug("Parsing database result");
        T instance = (T) currentClass.getDeclaredConstructor().newInstance();
        Field[] fields = currentClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            field.set(instance, resultSet.getObject(field.getName()));
        }
        logger.debug("Got the object");
        return instance;
    }

    public List<String> getFieldsNames(Field[] fields) {
        logger.debug("Getting class` fields names");
        List<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            String name = field.getName();
            fieldNames.add(name);
        }
        return fieldNames;
    }

    @Override
    public void update(T obj) {
        logger.info(String.format("Updating the object: %s", obj.toString()));

        logger.debug("Got table names and values");
        List<String> tablesNames = getFieldsNames(currentClass.getDeclaredFields());
        List<Object> rawValues = getRawValues(obj);
        List<String> rowValues = getValues(obj);

        logger.debug("Removing unnecessary Id field from all arrays");
        int idIndex = tablesNames.indexOf(getIdName());
        String id = rowValues.get(idIndex);
        tablesNames.remove(getIdName());
        rowValues.remove(idIndex);
        rawValues.remove(idIndex);

        logger.debug("Building SQL query");
        String query = "UPDATE " + getTableName()
                + setBuilder(tablesNames)
                + " WHERE " + getIdName() + " = " + id;
        logger.debug(String.format("Got it: %s", query));

        try (Connection connection = PostgresConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query))
        {
            int update = setPrepStatement(rawValues, rowValues, statement).executeUpdate();
            logger.info(String.format("%s row updated!",  update));
        } catch (SQLException e) {
            logger.error("Error while updating the object", e);
        }
    }

    private PreparedStatement setPrepStatement(List<Object> rawValues, List<String> rowValues, PreparedStatement statement) throws SQLException {
        logger.debug("Checking value.class to choose right 'set' method for prepared statement");
        for (int i = 0; i < rowValues.size(); i++) {
            int columnNumber = rawValues.indexOf(rawValues.get(i)) + 1;
            String val = rowValues.get(i);

            switch (rawValues.get(i).getClass().getSimpleName()) {
                case "Integer" -> statement.setInt(columnNumber, Integer.parseInt(val));
                case "Double" -> statement.setDouble(columnNumber, Double.parseDouble(val));
                case "Date" -> statement.setDate(columnNumber, Date.valueOf(val));
                case "String" -> statement.setString(columnNumber, val);
            }
        }
        return statement;
    }

    private String setBuilder(List<String> tablesNames) {
        logger.debug("Building 'SET' query for update() method");
        StringBuilder sb = new StringBuilder();
        sb.append(" SET ");
        for (String tablesName : tablesNames) {
            sb.append(tablesName)
                    .append(" = ")
                    .append(" ? ")
                    .append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public void delete(long id) {
        logger.info("Deleting the object");
        logger.debug("Building 'DELETE' SQL query");
        String query = "DELETE FROM " + getTableName()
                + " WHERE " + getIdName() + " = ? ";
        try (Connection connection = PostgresConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setInt(1, Math.toIntExact(id));
            int deleted = statement.executeUpdate();
            logger.info(String.format("%s row deleted!",  deleted));
        } catch (SQLException e) {
            logger.error("Error while deleting the object", e);
        }
    }
}
