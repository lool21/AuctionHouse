package com.epam.auction.dao.impl;

import com.epam.auction.dao.GenericDAO;
import com.epam.auction.dao.functional.StatementBiConsumer;
import com.epam.auction.dao.functional.StatementConsumer;
import com.epam.auction.db.ProxyConnection;
import com.epam.auction.entity.Entity;
import com.epam.auction.exception.DAOLayerException;
import com.epam.auction.exception.MethodNotSupportedException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class GenericDAOImpl<T extends Entity> implements GenericDAO<T> {

    ProxyConnection connection;

    private final String queryFindAll;
    private final String queryFindById;
    private final String queryDelete;
    private final String queryCreate;
    private final String queryUpdate;

    GenericDAOImpl(String queryFindAll, String queryFindById, String queryDelete, String queryCreate, String queryUpdate) {
        this.queryFindAll = queryFindAll;
        this.queryFindById = queryFindById;
        this.queryDelete = queryDelete;
        this.queryCreate = queryCreate;
        this.queryUpdate = queryUpdate;
    }

    public void setConnection(ProxyConnection connection) {
        this.connection = connection;
    }

    public List<T> findAll() throws DAOLayerException {
        return findList(queryFindAll);
    }

    public T findEntityById(int id) throws DAOLayerException {
        return findEntity(queryFindById, statement -> statement.setInt(1, id));
    }

    public boolean delete(int id) throws DAOLayerException, MethodNotSupportedException {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(queryDelete)) {
            statement.setInt(1, id);
            if (statement.executeUpdate() != 0) {
                result = true;
            }
        } catch (SQLException e) {
            throw new DAOLayerException(e.getMessage(), e);
        }

        return result;
    }

    public boolean create(T entity) throws DAOLayerException {
        return executeCreate(queryCreate, entity, this::defineQueryAttributes);
    }

    public boolean update(T entity) throws DAOLayerException, MethodNotSupportedException {
        return executeUpdate(queryUpdate, entity, (en, st) -> {
            defineQueryAttributes(en, st);
            st.setInt(en.getFieldsNumber(), en.getId());
        });
    }

    abstract T extractEntity(ResultSet resultSet) throws SQLException;

    abstract void defineQueryAttributes(T entity, PreparedStatement statement) throws SQLException;

    List<T> findSpecificList(String query, StatementConsumer statementConsumer) throws DAOLayerException {
        List<T> entities;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statementConsumer.accept(statement);
            ResultSet resultSet = statement.executeQuery();
            entities = new ArrayList<>();
            while (resultSet.next()) {
                entities.add(extractEntity(resultSet));
            }
        } catch (SQLException e) {
            throw new DAOLayerException(e.getMessage(), e);
        }

        return entities;
    }

    List<T> findList(String query) throws DAOLayerException {
        List<T> entities;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            entities = new ArrayList<>();
            while (resultSet.next()) {
                entities.add(extractEntity(resultSet));
            }

        } catch (SQLException e) {
            throw new DAOLayerException(e.getMessage(), e);
        }

        return entities;
    }

    T findEntity(String query, StatementConsumer statementConsumer) throws DAOLayerException {
        T entity = null;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statementConsumer.accept(statement);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                entity = extractEntity(resultSet);
            }

        } catch (SQLException e) {
            throw new DAOLayerException(e.getMessage(), e);
        }

        return entity;
    }

    boolean executeUpdate(String query, StatementConsumer statementConsumer) throws DAOLayerException {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statementConsumer.accept(statement);
            if (statement.executeUpdate() != 0) {
                result = true;
            }
        } catch (SQLException e) {
            throw new DAOLayerException(e.getMessage(), e);
        }

        return result;
    }

    boolean executeUpdate(String query, T entity, StatementBiConsumer<T> statementBiConsumer) throws DAOLayerException {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statementBiConsumer.accept(entity, statement);
            if (statement.executeUpdate() != 0) {
                result = true;
            }
        } catch (SQLException e) {
            throw new DAOLayerException(e.getMessage(), e);
        }

        return result;
    }

    boolean executeCreate(String query, T entity, StatementBiConsumer<T> statementBiConsumer) throws DAOLayerException {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statementBiConsumer.accept(entity, statement);
            if (statement.executeUpdate() != 0) {
                ResultSet keys = statement.getGeneratedKeys();
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                    result = true;
                }
            }
        } catch (SQLException e) {
            throw new DAOLayerException(e.getMessage(), e);
        }

        return result;
    }

}