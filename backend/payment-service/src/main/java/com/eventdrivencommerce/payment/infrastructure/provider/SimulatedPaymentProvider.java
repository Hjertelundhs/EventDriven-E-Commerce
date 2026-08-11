package com.eventdrivencommerce.payment.infrastructure.provider;
import com.eventdrivencommerce.payment.application.port.PaymentProvider; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component; import java.math.BigDecimal; import java.nio.charset.StandardCharsets; import java.util.Locale; import java.util.UUID;
@Component public class SimulatedPaymentProvider implements PaymentProvider {
 private final SimulationMode mode; public SimulatedPaymentProvider(@Value("${payment.simulation-mode:SUCCESS}")String mode){this.mode=SimulationMode.valueOf(mode.toUpperCase(Locale.ROOT));}
 public ProviderResult capture(UUID orderId,BigDecimal amount,String currency,String key){return result("sim_cap_",key);}
 public ProviderResult refund(UUID paymentId,UUID refundId,BigDecimal amount,String currency,String key){return result("sim_ref_",key);}
 private ProviderResult result(String prefix,String key){return switch(mode){case SUCCESS->ProviderResult.success(prefix+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString().replace("-",""));case DECLINE->ProviderResult.failure("SIMULATED_DECLINE",false);case TIMEOUT->throw new ProviderTimeoutException();};}
}
