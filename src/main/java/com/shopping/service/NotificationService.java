package com.shopping.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.shopping.entity.Orders;
import com.shopping.entity.Product;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNewProductAlert(Product product) {
        messagingTemplate.convertAndSend("/topic/products", product);
    }

    public void sendCartUpdate(Long userId) {
        messagingTemplate.convertAndSend("/topic/cart/" + userId, "Cart updated");
    }

    public void sendOrderUpdate(Orders order) {
        messagingTemplate.convertAndSend("/topic/orders/user/" + order.getUser().getId(), order);
        messagingTemplate.convertAndSend("/topic/orders/admin", order);
    }

    public void sendStockAlert(Product product) {
        if (product.getStock() < 5) {
            messagingTemplate.convertAndSend("/topic/stock/low", product);
        }
    }

public void sendCartCountUpdate(Long userId, int count) {
    // Send via WebSocket, Redis, or in-memory
    // Example: SimpMessagingTemplate
    // messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/cart-count", count);
}
}