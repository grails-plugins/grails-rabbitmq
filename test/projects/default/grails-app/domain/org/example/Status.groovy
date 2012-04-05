package org.example

class Status {
    String message
    Date dateCreated

    static constraints = {
        message blank: false
    }
}
