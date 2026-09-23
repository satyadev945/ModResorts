package com.acme.modres.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ModResortsCustomerInformation class.
 */
@ExtendWith(MockitoExtension.class)
public class ModResortsCustomerInformationTest {

    @InjectMocks
    private ModResortsCustomerInformation customerInformation;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Test
    void testGetCustomerInformation_withNullDataSource_returnsEmptyList() {
        // When dataSource is null (not injected), should return empty list
        ModResortsCustomerInformation info = new ModResortsCustomerInformation();
        ArrayList<String> result = info.getCustomerInformation();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerInformation_withDataSource_returnsCustomers() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("info")).thenReturn("Customer1", "Customer2");

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Customer1", result.get(0));
        assertEquals("Customer2", result.get(1));
    }

    @Test
    void testGetCustomerInformation_withEmptyResultSet_returnsEmptyList() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerInformation_withSQLException_returnsEmptyList() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerInformation_withSingleCustomer_returnsOneItem() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("info")).thenReturn("SingleCustomer");

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SingleCustomer", result.get(0));
    }

    @Test
    void testGetCustomerInformation_doesNotThrow() {
        ModResortsCustomerInformation info = new ModResortsCustomerInformation();
        assertDoesNotThrow(() -> info.getCustomerInformation());
    }
}
