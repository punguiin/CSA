package org.example.warehouse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductServiceTest {

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(SqliteProductRepository.inMemory());
    }

    @Test
    void createAssignsIdAndPersists() {
        int id = service.create(new Product("rice", "grocery", 50, 1200));

        Optional<Product> loaded = service.getById(id);
        assertTrue(loaded.isPresent());
        assertEquals("rice", loaded.get().getName());
        assertEquals("grocery", loaded.get().getCategory());
        assertEquals(50, loaded.get().getQuantity());
        assertEquals(1200, loaded.get().getPriceMinor());
    }

    @Test
    void readMissingReturnsEmpty() {
        assertTrue(service.getById(999).isEmpty());
    }

    @Test
    void updateChangesAllFields() {
        int id = service.create(new Product("rice", "grocery", 50, 1200));

        boolean updated = service.update(new Product(id, "brown rice", "grain", 75, 1500));

        assertTrue(updated);
        Product p = service.getById(id).orElseThrow();
        assertEquals("brown rice", p.getName());
        assertEquals("grain", p.getCategory());
        assertEquals(75, p.getQuantity());
        assertEquals(1500, p.getPriceMinor());
    }

    @Test
    void updateMissingReturnsFalse() {
        assertFalse(service.update(new Product(123, "ghost", "grocery", 1, 1)));
    }

    @Test
    void deleteRemovesProduct() {
        int id = service.create(new Product("salt", "grocery", 10, 300));

        assertTrue(service.delete(id));
        assertTrue(service.getById(id).isEmpty());
        assertFalse(service.delete(id), "deleting twice is a no-op");
    }

    @Test
    void createRejectsBlankNameAndNegativeNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new Product(" ", "grocery", 1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new Product("x", " ", 1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new Product("x", "grocery", -1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new Product("x", "grocery", 1, -1)));
    }

    @Test
    void updateWithoutIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.update(new Product("x", "grocery", 1, 1)));
    }

    @Test
    void creditAndWriteOffMoveStock() {
        int id = service.create(new Product("flour", "grocery", 100, 800));

        assertEquals(130, service.credit(id, 30).getAsInt());
        assertEquals(110, service.writeOff(id, 20).getAsInt());
        assertEquals(110, service.getQuantity(id).getAsInt());
    }

    @Test
    void writeOffBeyondStockFails() {
        int id = service.create(new Product("sugar", "grocery", 5, 400));

        OptionalInt result = service.writeOff(id, 10);
        assertTrue(result.isEmpty(), "should not over-draw stock");
        assertEquals(5, service.getQuantity(id).getAsInt(), "stock unchanged after failed write-off");
    }

    @Test
    void stockHelpersOnMissingProductReturnEmpty() {
        assertTrue(service.getQuantity(404).isEmpty());
        assertTrue(service.credit(404, 1).isEmpty());
        assertTrue(service.writeOff(404, 1).isEmpty());
        assertFalse(service.setPrice(404, 100));
    }

    @Test
    void setPriceUpdatesPrice() {
        int id = service.create(new Product("salt", "grocery", 10, 300));
        assertTrue(service.setPrice(id, 350));
        assertEquals(350, service.getById(id).orElseThrow().getPriceMinor());
    }

    private void seedCatalogue() {
        service.create(new Product("buckwheat", "grain", 100, 3000));
        service.create(new Product("brown rice", "grain", 40, 4500));
        service.create(new Product("white sugar", "sweetener", 200, 2000));
        service.create(new Product("sea salt", "seasoning", 5, 1500));
        service.create(new Product("rice flour", "grain", 0, 5000));
    }

    @Test
    void searchByNameSubstringIsCaseInsensitive() {
        seedCatalogue();
        Page<Product> page = service.search(ProductFilter.builder().name("RICE").build());

        List<String> names = page.getItems().stream().map(Product::getName).toList();
        assertEquals(List.of("brown rice", "rice flour"), names);
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void searchByCategory() {
        seedCatalogue();
        Page<Product> page = service.search(ProductFilter.builder().category("grain").build());
        assertEquals(3, page.getTotalElements());
        assertTrue(page.getItems().stream().allMatch(p -> p.getCategory().equals("grain")));
    }

    @Test
    void searchByPriceFloorOnly() {
        seedCatalogue();
        Page<Product> page = service.search(ProductFilter.builder().minPriceMinor(3000L).build());
        assertEquals(3, page.getTotalElements());
        assertTrue(page.getItems().stream().allMatch(p -> p.getPriceMinor() >= 3000));
    }

    @Test
    void searchByQuantityRange() {
        seedCatalogue();
        Page<Product> page = service.search(
                ProductFilter.builder().minQuantity(1).maxQuantity(100).build());
        List<String> names = page.getItems().stream().map(Product::getName).toList();
        assertEquals(List.of("buckwheat", "brown rice", "sea salt"), names);
    }

    @Test
    void searchCombinesNameAndCategoryAndPrice() {
        seedCatalogue();
        Page<Product> page = service.search(ProductFilter.builder()
                .name("rice")
                .category("grain")
                .minPriceMinor(4000L)
                .build());
        List<String> names = page.getItems().stream().map(Product::getName).toList();
        assertEquals(List.of("brown rice", "rice flour"), names);
    }

    @Test
    void searchWithNoFiltersReturnsEverything() {
        seedCatalogue();
        Page<Product> page = service.search(ProductFilter.builder().build());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void searchPaginates() {
        seedCatalogue();
        ProductFilter.Builder base = ProductFilter.builder().pageSize(2);

        Page<Product> p1 = service.search(base.page(1).build());
        assertEquals(5, p1.getTotalElements());
        assertEquals(3, p1.getTotalPages());
        assertEquals(2, p1.getItems().size());
        assertTrue(p1.hasNext());

        Page<Product> p3 = service.search(ProductFilter.builder().pageSize(2).page(3).build());
        assertEquals(1, p3.getItems().size(), "last page holds the remainder");
        assertFalse(p3.hasNext());
    }

    @Test
    void filterBuilderRejectsInvertedRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> ProductFilter.builder().minQuantity(10).maxQuantity(5).build());
        assertThrows(IllegalArgumentException.class,
                () -> ProductFilter.builder().minPriceMinor(100L).maxPriceMinor(50L).build());
    }
}
