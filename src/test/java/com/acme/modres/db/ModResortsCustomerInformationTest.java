package com.acme.modres.db;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ModResortsCustomerInformationTest {

    @Test
    void testGetCustomerInformation_Success() throws Exception {
        ModResortsCustomerInformation infoService = new ModResortsCustomerInformation();
        DataSource dataSource = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        
        java.lang.reflect.Field field = ModResortsCustomerInformation.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(infoService, dataSource);
        
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("INFO")).thenReturn("Customer 1", "Customer 2");
        
        ArrayList<String> result = infoService.getCustomerInformation();
        
        assertEquals(2, result.size());
        assertEquals("Customer 1", result.get(0));
        assertEquals("Customer 2", result.get(1));
    }

    @Test
    void testGetCustomerInformation_SQLException() throws Exception {
        ModResortsCustomerInformation infoService = new ModResortsCustomerInformation();
        DataSource dataSource = mock(DataSource.class);
        
        java.lang.reflect.Field field = ModResortsCustomerInformation.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(infoService, dataSource);
        
        when(dataSource.getConnection()).thenThrow(new SQLException("DB Error"));
        
        ArrayList<String> result = infoService.getCustomerInformation();
        
        assertTrue(result.isEmpty());
    }
}
