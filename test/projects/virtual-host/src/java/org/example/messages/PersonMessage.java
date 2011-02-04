package org.example.messages;

public class PersonMessage implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int age;

    public PersonMessage() {}
    public PersonMessage(String name, int age) {
        this.name = name;
        this.age = age;
    }

    String getName() { return name; }
    int getAge() { return age; }
}
