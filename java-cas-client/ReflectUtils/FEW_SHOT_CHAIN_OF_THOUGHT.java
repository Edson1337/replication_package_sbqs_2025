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

    @Test
    void testLoadClassSuccess() {
        Class<?> cls = ReflectUtils.loadClass("java.lang.Integer");
        assertEquals(Integer.class, cls);
    }

    @Test
    void testLoadClassFailure() {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.loadClass("com.nonexistent.Class"));
    }

    @Test
    void testNewInstanceNoArgs() {
        List<?> list = ReflectUtils.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void testNewInstanceWithArgsByClass() {
        StringBuilder sb = ReflectUtils.newInstance(StringBuilder.class, "abc");
        assertEquals("abc", sb.toString());
    }

    @Test
    void testNewInstanceWithArgsByName() {
        StringBuilder sb = ReflectUtils.newInstance("java.lang.StringBuilder", "xyz");
        assertEquals("xyz", sb.toString());
    }

    @Test
    void testNewInstanceFailure() {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.newInstance(StringBuilder.class, 123)); // construtor não existe
    }

    @Test
    void testGetPropertyDescriptorSuccess() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(info, "name");
        assertNotNull(pd);
        assertEquals("name", pd.getName());
    }

    @Test
    void testGetPropertyDescriptorFailure() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        assertNull(ReflectUtils.getPropertyDescriptor(info, "unknown"));
    }

    @Test
    void testSetPropertySuccess() {
        Person p = new Person();
        ReflectUtils.setProperty("name", "Alice", p);
        assertEquals("Alice", p.getName());
    }

    @Test
    void testSetPropertyFailure() {
        Person p = new Person();
        assertThrows(RuntimeException.class,
                () -> ReflectUtils.setProperty("nonexistent", "value", p));
    }

    @Test
    void testGetFieldSuccessAndSuperclass() {
        Employee e = new Employee();
        e.setName("Bob");
        e.setSalary(1000);
        assertEquals("Bob", ReflectUtils.getField("name", e));
        assertEquals(1000, ReflectUtils.getField("salary", e));
    }

    @Test
    void testGetFieldFailure() {
        Person p = new Person();
        assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.getField("nonexistent", p));
    }

    // Classes auxiliares para testes
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
}
