package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import javax.management.*;
import static org.junit.jupiter.api.Assertions.*;

class AppInfoTest {

    @Test
    void testGetMBeanInfo() {
        AppInfo appInfo = new AppInfo();
        MBeanInfo info = appInfo.getMBeanInfo();
        assertNotNull(info);
        assertEquals("Configurable App Info", info.getDescription());
    }

    @Test
    void testInvoke_IncreaseMaxLimit() throws MBeanException, ReflectionException {
        AppInfo appInfo = new AppInfo();
        Object result = appInfo.invoke("increaseMaxLimit", null, null);
        assertEquals("Max limit increased", result);
    }

    @Test
    void testInvoke_ResetMaxLimit() throws MBeanException, ReflectionException {
        AppInfo appInfo = new AppInfo();
        Object result = appInfo.invoke("resetMaxLimit", null, null);
        assertEquals("Max limit reset", result);
    }

    @Test
    void testInvoke_UnsupportedOperation() {
        AppInfo appInfo = new AppInfo();
        assertThrows(MBeanException.class, () -> {
            appInfo.invoke("unknownOp", null, null);
        });
    }

    @Test
    void testGetAttribute() throws AttributeNotFoundException, MBeanException, ReflectionException {
        AppInfo appInfo = new AppInfo();
        assertNull(appInfo.getAttribute("anyAttr"));
    }

    @Test
    void testSetAttribute() {
        AppInfo appInfo = new AppInfo();
        Attribute attr = new Attribute("anyAttr", "anyValue");
        assertDoesNotThrow(() -> appInfo.setAttribute(attr));
    }

    @Test
    void testGetAttributes() {
        AppInfo appInfo = new AppInfo();
        assertNull(appInfo.getAttributes(new String[]{"attr1"}));
    }

    @Test
    void testSetAttributes() {
        AppInfo appInfo = new AppInfo();
        assertNull(appInfo.setAttributes(new AttributeList()));
    }
}
