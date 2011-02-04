package org.example

class Person {
    String name
    int age

    static constraints = {
        name blank: false
        age()
    }
}
