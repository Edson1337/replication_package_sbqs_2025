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
    void loadClassShouldReturnClassWhenExists() {
        Class<?> cls = ReflectUtils.loadClass("java.lang.String");
        assertEquals(String.class, cls);
    }

    @Test
    void loadClassShouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.loadClass("com.xxx.NonExistent"));
    }

    @Test
    void newInstanceWithoutArgsShouldWork() {
        List<?> list = ReflectUtils.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void newInstanceWithArgsByClassShouldWork() {
        StringBuilder sb = ReflectUtils.newInstance(StringBuilder.class, "hello");
        assertEquals("hello", sb.toString());
    }

    @Test
    void newInstanceWithArgsByNameShouldWork() {
        StringBuilder sb = ReflectUtils.newInstance("java.lang.StringBuilder", "world");
        assertEquals("world", sb.toString());
    }

    @Test
    void newInstanceShouldThrowWhenNoMatchingCtor() {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.newInstance(StringBuilder.class, 123));
    }

    @Test
    void getPropertyDescriptorByClassShouldFindDescriptor() throws IntrospectionException {
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(Person.class, "name");
        assertNotNull(pd);
        assertEquals("name", pd.getName());
    }

    @Test
    void getPropertyDescriptorByInfoShouldFindDescriptor() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(info, "name");
        assertNotNull(pd);
    }

    @Test
    void getPropertyDescriptorShouldReturnNullWhenMissing() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        assertNull(ReflectUtils.getPropertyDescriptor(info, "unknown"));
    }

    @Test
    void setPropertyShouldAssignValue() {
        Person p = new Person();
        ReflectUtils.setProperty("name", "Alice", p);
        assertEquals("Alice", p.getName());
    }

    @Test
    void setPropertyShouldThrowWhenNoSuchProperty() {
        Person p = new Person();
        assertThrows(RuntimeException.class,
                () -> ReflectUtils.setProperty("foo", "bar", p));
    }

    @Test
    void getFieldShouldRetrieveDirectField() {
        Employee e = new Employee();
        e.setSalary(5000);
        assertEquals(5000, ReflectUtils.getField("salary", e));
    }

    @Test
    void getFieldShouldRetrieveInheritedField() {
        Employee e = new Employee();
        e.setName("Bob");
        assertEquals("Bob", ReflectUtils.getField("name", e));
    }

    @Test
    void getFieldShouldThrowWhenMissing() {
        Person p = new Person();
        assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.getField("age", p));
    }

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
