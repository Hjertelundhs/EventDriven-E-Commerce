package com.eventdrivencommerce.order.api;
import com.eventdrivencommerce.order.domain.model.Order;
import com.eventdrivencommerce.order.api.dto.OrderResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component public class OrderStatusStream {
    private final Map<UUID,List<SseEmitter>> emitters=new ConcurrentHashMap<>();
    public SseEmitter subscribe(Order order){var emitter=new SseEmitter(0L);emitters.computeIfAbsent(order.id(),ignored->new java.util.concurrent.CopyOnWriteArrayList<>()).add(emitter);Runnable remove=()->emitters.getOrDefault(order.id(),List.of()).remove(emitter);emitter.onCompletion(remove);emitter.onTimeout(remove);send(emitter,order);return emitter;}
    public void publish(Order order){emitters.getOrDefault(order.id(),List.of()).forEach(e->send(e,order));}
    private void send(SseEmitter emitter,Order order){try{emitter.send(SseEmitter.event().id(Long.toString(order.version())).name("order-status").data(OrderResponse.from(order)));}catch(IOException ex){emitter.completeWithError(ex);}}
}
