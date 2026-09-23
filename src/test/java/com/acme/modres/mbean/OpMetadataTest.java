package com.acme.modres.mbean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpMetadata class.
 */
public class OpMetadataTest {

    private OpMetadata opMetadata;

    @BeforeEach
    void setUp() {
        opMetadata = new OpMetadata();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(opMetadata);
    }

    @Test
    void testDefaultConstructor_fieldsAreNull() {
        assertNull(opMetadata.getName());
        assertNull(opMetadata.getDescription());
        assertNull(opMetadata.getType());
        assertEquals(0, opMetadata.getImpact());
    }

    @Test
    void testParameterizedConstructor_setsAllFields() {
        OpMetadata op = new OpMetadata("testOp", "Test operation", "void", 1);
        assertEquals("testOp", op.getName());
        assertEquals("Test operation", op.getDescription());
        assertEquals("void", op.getType());
        assertEquals(1, op.getImpact());
    }

    @Test
    void testSetName_setsCorrectly() {
        opMetadata.setName("myOperation");
        assertEquals("myOperation", opMetadata.getName());
    }

    @Test
    void testSetDescription_setsCorrectly() {
        opMetadata.setDescription("My description");
        assertEquals("My description", opMetadata.getDescription());
    }

    @Test
    void testSetType_setsCorrectly() {
        opMetadata.setType("java.lang.String");
        assertEquals("java.lang.String", opMetadata.getType());
    }

    @Test
    void testSetImpact_setsCorrectly() {
        opMetadata.setImpact(2);
        assertEquals(2, opMetadata.getImpact());
    }

    @Test
    void testSetName_withNull() {
        opMetadata.setName(null);
        assertNull(opMetadata.getName());
    }

    @Test
    void testSetDescription_withNull() {
        opMetadata.setDescription(null);
        assertNull(opMetadata.getDescription());
    }

    @Test
    void testSetType_withNull() {
        opMetadata.setType(null);
        assertNull(opMetadata.getType());
    }

    @Test
    void testSetImpact_withZero() {
        opMetadata.setImpact(0);
        assertEquals(0, opMetadata.getImpact());
    }

    @Test
    void testSetImpact_withNegative() {
        opMetadata.setImpact(-1);
        assertEquals(-1, opMetadata.getImpact());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        OpMetadata op = new OpMetadata(null, null, null, 0);
        assertNull(op.getName());
        assertNull(op.getDescription());
        assertNull(op.getType());
        assertEquals(0, op.getImpact());
    }

    @Test
    void testOverwriteName() {
        opMetadata.setName("first");
        opMetadata.setName("second");
        assertEquals("second", opMetadata.getName());
    }

    @Test
    void testOverwriteImpact() {
        opMetadata.setImpact(1);
        opMetadata.setImpact(5);
        assertEquals(5, opMetadata.getImpact());
    }
}
