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
    void shouldLoadExistingClass() {
        Class<?> cls = ReflectUtils.loadClass("java.lang.String");
        assertEquals(String.class, cls);
    }

    @Test
    void shouldThrowWhenClassNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.loadClass("com.xxx.NonExistent"));
        assertTrue(ex.getMessage().contains("class not found"));
    }

    @Test
    void shouldCreateInstanceWithoutArgs() {
        List<?> list = ReflectUtils.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void shouldCreateInstanceWithArgsByClass() {
        StringBuilder sb = ReflectUtils.newInstance(StringBuilder.class, "hello");
        assertEquals("hello", sb.toString());
    }

    @Test
    void shouldCreateInstanceWithArgsByName() {
        StringBuilder sb = ReflectUtils.newInstance("java.lang.StringBuilder", "world");
        assertEquals("world", sb.toString());
    }

    @Test
    void shouldThrowWhenNoMatchingConstructor() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.newInstance(StringBuilder.class, 123));
        assertTrue(ex.getCause() instanceof NoSuchMethodException
                || ex.getMessage().contains("Error creating new instance"));
    }

    @Test
    void shouldFindPropertyDescriptorByClass() throws IntrospectionException {
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(Person.class, "name");
        assertNotNull(pd);
        assertEquals("name", pd.getName());
    }

    @Test
    void shouldFindPropertyDescriptorByBeanInfo() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(info, "name");
        assertNotNull(pd);
    }

    @Test
    void shouldReturnNullForMissingPropertyDescriptor() throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        assertNull(ReflectUtils.getPropertyDescriptor(info, "unknown"));
    }

    @Test
    void shouldSetPropertyValue() {
        Person p = new Person();
        ReflectUtils.setProperty("name", "Alice", p);
        assertEquals("Alice", p.getName());
    }

    @Test
    void shouldThrowWhenSettingUnknownProperty() {
        Person p = new Person();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ReflectUtils.setProperty("foo", "bar", p));
        assertTrue(ex.getMessage().contains("Error setting property"));
    }

    @Test
    void shouldGetDeclaredFieldValue() {
        Employee e = new Employee();
        e.setSalary(5000);
        assertEquals(5000, ReflectUtils.getField("salary", e));
    }

    @Test
    void shouldGetInheritedFieldValue() {
        Employee e = new Employee();
        e.setName("Bob");
        assertEquals("Bob", ReflectUtils.getField("name", e));
    }

    @Test
    void shouldThrowWhenFieldMissing() {
        Person p = new Person();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.getField("age", p));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    /* classes auxiliares para os testes */
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
