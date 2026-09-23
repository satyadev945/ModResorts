package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.management.MBeanOperationInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for DMBeanUtils class.
 */
public class DMBeanUtilsTest {

    @Test
    void testGetOps_withNullOpList_returnsNull() {
        MBeanOperationInfo[] result = DMBeanUtils.getOps(null);
        assertNull(result);
    }

    @Test
    void testGetOps_withEmptyOpList_returnsNull() {
        OpMetadataList emptyList = new OpMetadataList();
        MBeanOperationInfo[] result = DMBeanUtils.getOps(emptyList);
        assertNull(result);
    }

    @Test
    void testGetOps_withNullInternalList_returnsNull() {
        OpMetadataList opList = new OpMetadataList();
        opList.setOpMetadatList(null);
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withSingleOp_returnsArray() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("testOp", "Test operation", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void testGetOps_withSingleOp_correctName() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("myOperation", "My operation", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("myOperation", result[0].getName());
    }

    @Test
    void testGetOps_withSingleOp_correctDescription() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "My description", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("My description", result[0].getDescription());
    }

    @Test
    void testGetOps_withSingleOp_correctReturnType() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "desc", "java.lang.String", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("java.lang.String", result[0].getReturnType());
    }

    @Test
    void testGetOps_withMultipleOps_returnsCorrectCount() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("op2", "desc2", "String", MBeanOperationInfo.INFO));
        opList.add(new OpMetadata("op3", "desc3", "int", MBeanOperationInfo.ACTION_INFO));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void testGetOps_withMultipleOps_correctOrder() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("firstOp", "first desc", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("secondOp", "second desc", "String", MBeanOperationInfo.INFO));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("firstOp", result[0].getName());
        assertEquals("secondOp", result[1].getName());
    }

    @Test
    void testGetOps_withImpactValue_correctImpact() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "desc", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(MBeanOperationInfo.ACTION, result[0].getImpact());
    }
}
