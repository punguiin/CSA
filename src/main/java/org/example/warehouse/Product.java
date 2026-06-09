package org.example.warehouse;

import java.util.Objects;

public class Product {

    private Integer id;
    private String name;
    private String category;
    private int quantity;
    private long priceMinor;

    public Product(String name, String category, int quantity, long priceMinor) {
        this(null, name, category, quantity, priceMinor);
    }

    public Product(Integer id, String name, String category, int quantity, long priceMinor) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.priceMinor = priceMinor;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getPriceMinor() {
        return priceMinor;
    }

    public void setPriceMinor(long priceMinor) {
        this.priceMinor = priceMinor;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return quantity == product.quantity
                && priceMinor == product.priceMinor
                && Objects.equals(id, product.id)
                && Objects.equals(name, product.name)
                && Objects.equals(category, product.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, category, quantity, priceMinor);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", priceMinor=" + priceMinor +
                '}';
    }
}
