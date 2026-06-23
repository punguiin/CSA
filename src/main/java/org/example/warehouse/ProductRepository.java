package org.example.warehouse;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    int create(Product product);

    Optional<Product> findById(int id);

    Optional<Product> findByName(String name);

    List<Product> findAll();

    boolean update(Product product);

    boolean delete(int id);

    Page<Product> search(ProductFilter filter);

    int count();

    int deleteAll();
}
