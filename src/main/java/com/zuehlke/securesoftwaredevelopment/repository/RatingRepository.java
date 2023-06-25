package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.domain.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RatingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RatingRepository.class);

    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(RatingRepository.class);

    private DataSource dataSource;

    public RatingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createOrUpdate(Rating rating) {
        String query1 = "SELECT movieId, userId, rating FROM ratings WHERE movieId = " + rating.getMovieId() + " AND userID = " + rating.getUserId();
        String query2 = "update ratings SET rating = ? WHERE movieId = ? AND userId = ?";
        String query3 = "insert into ratings(movieId, userId, rating) values (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query1)
        ) {
            if (rs.next()) {
                try(PreparedStatement preparedStatement = connection.prepareStatement(query2)){
                    Rating ratingFromDb = new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3));
                    preparedStatement.setInt(1, rating.getRating());
                    preparedStatement.setInt(2, rating.getMovieId());
                    preparedStatement.setInt(3, rating.getUserId());
                    preparedStatement.executeUpdate();
                    auditLogger
                            .auditChange(new Entity(
                                    "rating.update",
                                    String.valueOf(rating.getMovieId()),
                                    ratingFromDb.toString(),
                                    rating.toString()
                            ));
                }
            } else {
                try(PreparedStatement preparedStatement = connection.prepareStatement(query3)){
                    preparedStatement.setInt(1, rating.getMovieId());
                    preparedStatement.setInt(2, rating.getUserId());
                    preparedStatement.setInt(3, rating.getRating());
                    preparedStatement.executeUpdate();
                    auditLogger.audit("Rating " + rating + " created.");
                }
            }
        } catch (SQLException e) {
            LOG.warn("Failed to update rating for movie with ID " + rating.getMovieId() + ".", e);
        }
    }

    public List<Rating> getAll(String movieId) {
        List<Rating> ratingList = new ArrayList<>();
        String query = "SELECT movieId, userId, rating FROM ratings WHERE movieId = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, movieId);
            try( ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    ratingList.add(new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
                }
            }
        } catch (SQLException e) {
            LOG.warn("Failed to fetch all ratings.", e);
        }
        return ratingList;
    }
}
