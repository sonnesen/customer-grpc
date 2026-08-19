package com.sonnesen;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class CustomerRowMapper implements RowMapper<Customer> {

    @Override
    public Customer map(ResultSet rs, StatementContext ctx) throws SQLException {
        Address address = new Address(
            rs.getString("street"),
            rs.getString("city"),
            rs.getString("state"),
            rs.getString("zip_code")
        );

        return new Customer(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("phone"),
            address,
            CustomerStatus.valueOf(rs.getString("status"))
        );
    }
}
