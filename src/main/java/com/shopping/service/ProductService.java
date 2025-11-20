package com.shopping.service;

import com.shopping.dto.*;
import com.shopping.entity.Product;
import com.shopping.entity.ProductImage;
import com.shopping.repository.ProductImageRepository;
import com.shopping.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepo;

    public ProductResponse uploadProduct(ProductDTO dto, MultipartFile mainFile, List<MultipartFile> extraFiles) throws IOException {
        Product product = new Product();
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        product.setCategory(dto.category());
        product.setSize(dto.size());
        product.setColor(dto.color());

        if (mainFile != null && !mainFile.isEmpty()) {
            ProductImage mainImage = createImage(mainFile, true);
            mainImage.setProduct(product);
            product.getImages().add(mainImage);
        }

        if (extraFiles != null && !extraFiles.isEmpty()) {
            for (MultipartFile file : extraFiles) {
                if (!file.isEmpty()) {
                    ProductImage img = createImage(file, false);
                    img.setProduct(product);
                    product.getImages().add(img);
                }
            }
        }

        Product saved = productRepository.save(product);
        return new ProductResponse(saved);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return new ProductResponse(product);
    }

    private ProductImage createImage(MultipartFile file, boolean isMain) throws IOException {
        ProductImage img = new ProductImage();
        img.setImageData(file.getBytes());
        img.setImageName(file.getOriginalFilename());
        img.setMain(isMain);
        return img;
    }

    @Transactional
    public Product updateProduct(Long id, ProductUpdateDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (dto.name() != null) product.setName(dto.name());
        if (dto.description() != null) product.setDescription(dto.description());
        if (dto.price() != null && dto.price() > 0) product.setPrice(dto.price());
        if (dto.stock() != null && dto.stock() >= 0) product.setStock(dto.stock());

        if (dto.imageIdsToDelete() != null && !dto.imageIdsToDelete().isEmpty()) {
            imageRepo.deleteAllById(dto.imageIdsToDelete());
        }

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        imageRepo.deleteAll(product.getImages());
        productRepository.delete(product);
    }
}