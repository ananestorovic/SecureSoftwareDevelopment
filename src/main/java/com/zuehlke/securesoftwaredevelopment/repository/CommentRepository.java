package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.domain.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CommentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CommentRepository.class);


    private DataSource dataSource;

    public CommentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(Comment comment) {
        String query = "insert into comments(movieId, userId, comment) values ( ? , ? , ? )";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
        ) {
            preparedStatement.setInt(1, comment.getMovieId());
            preparedStatement.setInt(2, comment.getUserId());
            preparedStatement.setString(3, comment.getComment());
            preparedStatement.execute(query);

        } catch (SQLException e) {
           LOG.error("Error: Comment creation failed.");
        }
    }

    public List<Comment> getAll(String movieId) {
        List<Comment> commentList = new ArrayList<>();
        String query = "SELECT movieId, userId, comment FROM comments WHERE movieId = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, movieId);
            try( ResultSet rs = preparedStatement.executeQuery();) {
                while (rs.next()) {
                    commentList.add(new Comment(rs.getInt(1), rs.getInt(2), rs.getString(3)));
                }
            }

        } catch (SQLException e) {
            LOG.error("Error: Fetching comments failed.");
        }
        return commentList;
    }
}