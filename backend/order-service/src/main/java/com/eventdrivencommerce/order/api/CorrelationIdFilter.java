package com.eventdrivencommerce.order.api;
import jakarta.servlet.*;import jakarta.servlet.http.*;import org.slf4j.MDC;import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;import java.io.IOException;import java.util.UUID;
@Component public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER="X-Correlation-ID";
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{String raw=request.getHeader(HEADER);String id;try{id=raw==null?UUID.randomUUID().toString():UUID.fromString(raw).toString();}catch(IllegalArgumentException ex){id=UUID.randomUUID().toString();}response.setHeader(HEADER,id);request.setAttribute(HEADER,id);try(MDC.MDCCloseable ignored=MDC.putCloseable("correlationId",id)){chain.doFilter(request,response);}}
}
