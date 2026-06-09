package org.example.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public int create(Product product) {
        validate(product);
        return repository.create(product);
    }

    public Optional<Product> getById(int id) {
        return repository.findById(id);
    }

    public List<Product> getAll() {
        return repository.findAll();
    }

    public boolean update(Product product) {
        if (product.getId() == null) {
            throw new IllegalArgumentException("Can't update a product without an id");
        }
        validate(product);
        return repository.update(product);
    }

    public boolean delete(int id) {
        return repository.delete(id);
    }

    public Page<Product> search(ProductFilter filter) {
        return repository.search(filter);
    }

    public int count() {
        return repository.count();
    }

    public OptionalInt getQuantity(int productId) {
        return repository.findById(productId)
                .map(p -> OptionalInt.of(p.getQuantity()))
                .orElseGet(OptionalInt::empty);
    }

    public OptionalInt credit(int productId, int amount) {
        requirePositive(amount, "credit amount");
        Optional<Product> found = repository.findById(productId);
        if (found.isEmpty()) {
            return OptionalInt.empty();
        }
        Product p = found.get();
        p.setQuantity(p.getQuantity() + amount);
        repository.update(p);
        return OptionalInt.of(p.getQuantity());
    }

    public OptionalInt writeOff(int productId, int amount) {
        requirePositive(amount, "write-off amount");
        Optional<Product> found = repository.findById(productId);
        if (found.isEmpty() || found.get().getQuantity() < amount) {
            return OptionalInt.empty();
        }
        Product p = found.get();
        p.setQuantity(p.getQuantity() - amount);
        repository.update(p);
        return OptionalInt.of(p.getQuantity());
    }

    public boolean setPrice(int productId, long priceMinor) {
        if (priceMinor < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
        Optional<Product> found = repository.findById(productId);
        if (found.isEmpty()) {
            return false;
        }
        Product p = found.get();
        p.setPriceMinor(priceMinor);
        return repository.update(p);
    }

    private static void validate(Product p) {
        if (p == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        if (p.getName() == null || p.getName().isBlank()) {
            throw new IllegalArgumentException("product name must not be blank");
        }
        if (p.getCategory() == null || p.getCategory().isBlank()) {
            throw new IllegalArgumentException("product category must not be blank");
        }
        if (p.getQuantity() < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        if (p.getPriceMinor() < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
    }

    private static void requirePositive(int amount, String what) {
        if (amount <= 0) {
            throw new IllegalArgumentException(what + " must be > 0");
        }
    }
}
