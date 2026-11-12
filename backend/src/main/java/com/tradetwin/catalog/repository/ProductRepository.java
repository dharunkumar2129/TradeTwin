package com.tradetwin.catalog.repository;

import com.tradetwin.catalog.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByActiveTrue();
    List<Product> findBySellerId(String sellerId);
    List<Product> findByCategoryIgnoreCaseAndActiveTrue(String category);

    @Query("{ 'active': true, $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } }, { 'tags': { $regex: ?0, $options: 'i' } } ] }")
    List<Product> searchProducts(String regex);
}
