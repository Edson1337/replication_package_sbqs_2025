package org.apereo.cas.client.util;

import org.junit.jupiter.api.Test;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReflectUtilsTests {

    public static class Person {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Employee extends Person {
        private int salary;
        public int getSalary() { return salary; }
        public void setSalary(int salary) { this.salary = salary; }
    }

    @Test
    void loadClassValid() {
        Class<?> cls = ReflectUtils.loadClass("java.lang.String");
        assertEquals(String.class, cls);
    }

    @Test
    void loadClassInvalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.loadClass("com.unknown.Foo"));
        assertTrue(ex.getMessage().contains("class not found"));
    }

    @Test
    void newInstanceNoArgs() {
        List<?> list = ReflectUtils.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void newInstanceWithArgsByClass() {
        StringBuilder sb = ReflectUtils.newInstance(StringBuilder.class, "text");
        assertEquals("text", sb.toString());
    }

    @Test
    void newInstanceWithArgsByName() {
        StringBuilder sb = ReflectUtils.newInstance("java.lang.StringBuilder", "demo");
        assertEquals("demo", sb.toString());
    }

    @Test
    void newInstanceNoMatchingConstructor() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.newInstance(StringBuilder.class, 123));
        assertNotNull(ex.getCause());
    }

    @Test
    void getPropertyDescriptorByClass() {
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(Person.class, "name");
        assertNotNull(pd);
        assertEquals("name", pd.getName());
    }

    @Test
    void getPropertyDescriptorMissingByClass() {
        assertNull(ReflectUtils.getPropertyDescriptor(Person.class, "unknown"));
    }

    @Test
    void getPropertyDescriptorByBeanInfo() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(info, "name");
        assertNotNull(pd);
    }

    @Test
    void getPropertyDescriptorMissingByBeanInfo() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        assertNull(ReflectUtils.getPropertyDescriptor(info, "foo"));
    }

    @Test
    void setPropertyValid() {
        Person p = new Person();
        ReflectUtils.setProperty("name", "Alice", p);
        assertEquals("Alice", p.getName());
    }

    @Test
    void setPropertyInvalid() {
        Person p = new Person();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ReflectUtils.setProperty("age", 30, p));
        assertTrue(ex.getMessage().contains("Error setting property"));
    }

    @Test
    void getFieldDeclared() {
        Employee e = new Employee();
        e.setSalary(5000);
        assertEquals(5000, ReflectUtils.getField("salary", e));
    }

    @Test
    void getFieldInherited() {
        Employee e = new Employee();
        e.setName("Bob");
        assertEquals("Bob", ReflectUtils.getField("name", e));
    }

    @Test
    void getFieldMissing() {
        Person p = new Person();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.getField("age", p));
        assertTrue(ex.getMessage().contains("does not exist"));
    }
}
