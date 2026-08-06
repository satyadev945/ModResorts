package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpMetadataTest {

    @Test
    void testDefaultConstructor() {
        OpMetadata op = new OpMetadata();
        assertNull(op.getName());
        assertNull(op.getDescription());
        assertNull(op.getType());
        assertEquals(0, op.getImpact());
    }

    @Test
    void testParameterizedConstructor() {
        OpMetadata op = new OpMetadata("Name", "Desc", "Type", 10);
        assertEquals("Name", op.getName());
        assertEquals("Desc", op.getDescription());
        assertEquals("Type", op.getType());
        assertEquals(10, op.getImpact());
    }

    @Test
    void testSettersAndGetters() {
        OpMetadata op = new OpMetadata();
        op.setName("NewName");
        op.setDescription("NewDesc");
        op.setType("NewType");
        op.setImpact(20);
        
        assertEquals("NewName", op.getName());
        assertEquals("NewDesc", op.getDescription());
        assertEquals("NewType", op.getType());
        assertEquals(20, op.getImpact());
    }
}
