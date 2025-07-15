package com.boutique_catalogue_produits.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeignRequestInterceptor implements RequestInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        logger.debug("Feign request: {} {}", template.method(), template.url());
        template.headers().forEach((key, values) ->
                logger.debug("Header: {} = {}", key, values));
    }
}