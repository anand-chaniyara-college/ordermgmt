package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.OrderItem;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderMapperImpl {

    private final OrderItemRepository orderItemRepository;

    public OrderDTO convertToDTO(Orders order, List<OrderItemDTO> items, BigDecimal total) {
        return new OrderDTO(
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getStatus().getStatusName(),
                order.getCreatedTimestamp(),
                order.getUpdatedTimestamp(),
                items,
                total);
    }

    @Transactional(readOnly = true)
    public OrderDTO convertToDTO(Orders order) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
        List<OrderItemDTO> itemDTOs = items.stream().map(item -> new OrderItemDTO(
                item.getInventoryItem().getItemId(),
                item.getInventoryItem().getItemName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).collect(Collectors.toList());
        BigDecimal total = calculateTotal(itemDTOs);
        return convertToDTO(order, itemDTOs, total);
    }

    public BigDecimal calculateTotal(List<OrderItemDTO> items) {
        return items.stream()
                .map(item -> {
                    if (item.getSubTotal() == null) {
                        throw new InvalidOperationException(
                            "SubTotal is null for item: " + item.getItemId());
                    }
                    return item.getSubTotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
