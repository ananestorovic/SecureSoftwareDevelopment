package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.domain.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PersonRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PersonRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private DataSource dataSource;

    public PersonRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Person> getAll() {
        List<Person> personList = new ArrayList<>();
        String query = "SELECT id, firstName, lastName, email FROM persons";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                personList.add(createPersonFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.warn("Failed to fetch persons.", e);
        }
        return personList;
    }

    public List<Person> search(String searchTerm) throws SQLException {
        List<Person> personList = new ArrayList<>();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            String query = "SELECT id, firstName, lastName, email FROM persons WHERE UPPER(firstName) like UPPER(CONCAT( '%',?,'%'))" +
                    " OR UPPER(lastName) like UPPER(CONCAT( '%',?,'%'))";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, searchTerm);
                preparedStatement.setString(2, searchTerm);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        personList.add(createPersonFromResultSet(rs));
                    }
                }
            }
        }
        return personList;
    }

    public Person get(String personId) {
        String query = "SELECT id, firstName, lastName, email FROM persons WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, personId);
            try( ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    return createPersonFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOG.warn("Failed to get person with ID " + personId + ".", e);
        }

        return null;
    }

    public void delete(int personId) {
        String query = "DELETE FROM persons WHERE id = " + personId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to delete person with ID " + personId + ".", e);
        }
    }

    private Person createPersonFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String firstName = rs.getString(2);
        String lastName = rs.getString(3);
        String email = rs.getString(4);
        return new Person("" + id, firstName, lastName, email);
    }

    public void update(Person personUpdate) {
        Person personFromDb = get(personUpdate.getId());
        String query = "UPDATE persons SET firstName = ?, lastName = ?, email = ? where id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            String firstName = personUpdate.getFirstName() != null ? personUpdate.getFirstName() : personFromDb.getFirstName();
            String email = personUpdate.getEmail() != null ? personUpdate.getEmail() : personFromDb.getEmail();
            statement.setString(1, firstName);
            statement.setString(2, personUpdate.getLastName());
            statement.setString(3, email);
            statement.setString(4, personUpdate.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            LOG.warn("Failed to update person with ID " + personUpdate.getId() + ".", e);
        }
    }
}
