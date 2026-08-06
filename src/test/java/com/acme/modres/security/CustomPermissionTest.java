package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomPermissionTest {

    @Test
    void testConstructorSingleArg() {
        CustomPermission perm = new CustomPermission("testPerm");
        assertEquals("testPerm", perm.getName());
    }

    @Test
    void testConstructorDoubleArg() {
        CustomPermission perm = new CustomPermission("testPerm", "read,write");
        assertEquals("testPerm", perm.getName());
        assertEquals("read,write", perm.getActions());
    }
}
