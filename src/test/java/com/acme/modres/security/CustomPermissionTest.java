package com.acme.modres.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomPermission class.
 */
public class CustomPermissionTest {

    @Test
    void testConstructorWithName_createsInstance() {
        CustomPermission permission = new CustomPermission("my-permission");
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithName_correctName() {
        CustomPermission permission = new CustomPermission("my-permission");
        assertEquals("my-permission", permission.getName());
    }

    @Test
    void testConstructorWithNameAndActions_createsInstance() {
        CustomPermission permission = new CustomPermission("my-permission", "read");
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithNameAndActions_correctName() {
        CustomPermission permission = new CustomPermission("my-permission", "read,write");
        assertEquals("my-permission", permission.getName());
    }

    @Test
    void testConstructorWithNameAndActions_nullActions() {
        CustomPermission permission = new CustomPermission("my-permission", null);
        assertNotNull(permission);
        assertEquals("my-permission", permission.getName());
    }

    @Test
    void testExtendsBasicPermission() {
        CustomPermission permission = new CustomPermission("test");
        assertTrue(permission instanceof java.security.BasicPermission);
    }

    @Test
    void testImplies_samePermission_returnsTrue() {
        CustomPermission p1 = new CustomPermission("my-permission");
        CustomPermission p2 = new CustomPermission("my-permission");
        assertTrue(p1.implies(p2));
    }

    @Test
    void testImplies_differentPermission_returnsFalse() {
        CustomPermission p1 = new CustomPermission("permission-a");
        CustomPermission p2 = new CustomPermission("permission-b");
        assertFalse(p1.implies(p2));
    }

    @Test
    void testConstructorWithWildcard_createsInstance() {
        CustomPermission permission = new CustomPermission("*");
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithDotNotation_createsInstance() {
        CustomPermission permission = new CustomPermission("com.acme.modres.*");
        assertNotNull(permission);
    }

    @Test
    void testGetActions_returnsEmptyString() {
        CustomPermission permission = new CustomPermission("my-permission");
        // BasicPermission.getActions() returns empty string
        assertEquals("", permission.getActions());
    }
}
