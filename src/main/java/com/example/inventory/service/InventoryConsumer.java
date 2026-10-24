package com.example.inventory.service;

import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryConsumer.class);

    private final InventoryRepository inventoryRepository;
    private final ObjectMapper objectMapper;

    public InventoryConsumer(InventoryRepository inventoryRepository, ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-events", groupId = "inventory-group")
    @Transactional
    public void handlePaymentSuccess(String message) {
        try {
            // 1. Parse JSON message nhận được từ Kafka topic "payment-events"
            // (Giả định message có cấu trúc JSON chứa "skuCode" và "quantity")
            JsonNode jsonNode = objectMapper.readTree(message);
            String skuCode = jsonNode.path("skuCode").asText();
            int quantityToReduce = jsonNode.path("quantity").asInt(1); // Mặc định trừ 1 nếu không truyền

            // 2. Tìm sản phẩm trong bảng inventories theo skuCode
            Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với skuCode: " + skuCode));

            // 3. Kiểm tra số lượng tồn kho
            if (inventory.getStockQuantity() < quantityToReduce) {
                log.error("Không đủ hàng trong kho cho skuCode: {}. Tồn kho hiện tại: {}, Yêu cầu: {}", 
                        skuCode, inventory.getStockQuantity(), quantityToReduce);
                throw new RuntimeException("Out of stock for skuCode: " + skuCode);
            }

            // 4. Thực hiện trừ kho
            inventory.setStockQuantity(inventory.getStockQuantity() - quantityToReduce);
            inventoryRepository.save(inventory);

            // 5. Phản hồi / Log message thông báo trừ hàng thành công kèm skuCode
            log.info(">>> TRỪ HÀNG THÀNH CÔNG: skuCode = {}, Số lượng đã trừ = {}, Tồn kho còn lại = {}", 
                    skuCode, quantityToReduce, inventory.getStockQuantity());

        } catch (Exception e) {
            log.error("Lỗi xử lý sự kiện payment-events cho kho: {}", e.getMessage(), e);
        }
    }
}