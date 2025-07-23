// File: cas-client-core/src/test/java/org/apereo/cas/client/util/ReflectUtilsTest.java
package org.apereo.cas.client.util;

import org.junit.jupiter.api.Test;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReflectUtilsTests {

    @Test
    public void testLoadClassSuccess() {
        Class<?> cls = ReflectUtils.loadClass("java.lang.String");
        assertEquals(String.class, cls);
    }

    @Test
    public void testLoadClassFailure() {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectUtils.loadClass("no.such.Class"));
    }

    @Test
    public void testNewInstanceNoArgsByClass() {
        List<?> list = ReflectUtils.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewInstanceWithArgsByClass() {
        StringBuilder sb = ReflectUtils.newInstance(StringBuilder.class, "abc");
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testNewInstanceWithArgsByName() {
        StringBuilder sb = ReflectUtils.newInstance("java.lang.StringBuilder", "def");
        assertEquals("def", sb.toString());
    }

    @Test
    public void testSetPropertyAndGetField() {
        Person p = new Person();
        ReflectUtils.setProperty("name", "Alice", p);
        assertEquals("Alice", p.getName());
        // getField deve recuperar o valor mesmo de campo privado
        assertEquals("Alice", ReflectUtils.getField("name", p));
    }

    @Test
    public void testGetFieldSuperclass() {
        Employee e = new Employee();
        e.setName("Charlie");
        e.setSalary(5000);
        assertEquals("Charlie", ReflectUtils.getField("name", e));
        assertEquals(5000, ReflectUtils.getField("salary", e));
    }

    @Test
    public void testGetPropertyDescriptor() throws Exception {
        BeanInfo info = Introspector.getBeanInfo(Person.class);
        PropertyDescriptor pd = ReflectUtils.getPropertyDescriptor(info, "name");
        assertNotNull(pd);
        assertEquals("name", pd.getName());
        // propriedade inexistente retorna null
        assertNull(ReflectUtils.getPropertyDescriptor(info, "unknown"));
    }

    // Classe auxiliar para testes de JavaBean
    public static class Person {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // Subclasse para testar busca de campo em superclasse
    public static class Employee extends Person {
        private int salary;
        public int getSalary() { return salary; }
        public void setSalary(int salary) { this.salary = salary; }
    }
}
