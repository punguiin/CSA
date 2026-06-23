package org.example.warehouse;

public class DuplicateProductNameException extends WarehouseException {

    public DuplicateProductNameException(String name) {
        super("A product named '" + name + "' already exists");
    }
}
