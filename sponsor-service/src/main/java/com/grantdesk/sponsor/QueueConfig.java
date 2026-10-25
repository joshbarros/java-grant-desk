package com.grantdesk.sponsor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class QueueConfig {

    @Bean
    public Queue rateDlq() {
        return QueueBuilder.durable(Channels.RATE_DLQ).build();
    }

    @Bean
    @DependsOn("rateDlq")
    public Queue rateQueue() {
        return QueueBuilder.durable(Channels.RATE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RATE_DLQ)
                .build();
    }

    @Bean
    public Queue reserveDlq() {
        return QueueBuilder.durable(Channels.RESERVE_DLQ).build();
    }

    @Bean
    @DependsOn("reserveDlq")
    public Queue reserveQueue() {
        return QueueBuilder.durable(Channels.RESERVE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RESERVE_DLQ)
                .build();
    }

    @Bean
    public Queue decideDlq() {
        return QueueBuilder.durable(Channels.DECIDE_DLQ).build();
    }

    @Bean
    @DependsOn("decideDlq")
    public Queue decideQueue() {
        return QueueBuilder.durable(Channels.DECIDE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.DECIDE_DLQ)
                .build();
    }

    @Bean
    public Queue releaseDlq() {
        return QueueBuilder.durable(Channels.RELEASE_DLQ).build();
    }

    @Bean
    @DependsOn("releaseDlq")
    public Queue releaseQueue() {
        return QueueBuilder.durable(Channels.RELEASE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RELEASE_DLQ)
                .build();
    }

    @Bean
    public Queue releaseHoldDlq() {
        return QueueBuilder.durable(Channels.RELEASE_HOLD_DLQ).build();
    }

    @Bean
    @DependsOn("releaseHoldDlq")
    public Queue releaseHoldQueue() {
        return QueueBuilder.durable(Channels.RELEASE_HOLD)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RELEASE_HOLD_DLQ)
                .build();
    }

    @Bean
    public Queue cutDlq() {
        return QueueBuilder.durable(Channels.CUT_DLQ).build();
    }

    @Bean
    @DependsOn("cutDlq")
    public Queue cutQueue() {
        return QueueBuilder.durable(Channels.CUT)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.CUT_DLQ)
                .build();
    }

    @Bean
    public Queue payDlq() {
        return QueueBuilder.durable(Channels.PAY_DLQ).build();
    }

    @Bean
    @DependsOn("payDlq")
    public Queue payQueue() {
        return QueueBuilder.durable(Channels.PAY)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.PAY_DLQ)
                .build();
    }

    @Bean
    public Queue wireDlq() {
        return QueueBuilder.durable(Channels.WIRE_DLQ).build();
    }

    @Bean
    @DependsOn("wireDlq")
    public Queue wireQueue() {
        return QueueBuilder.durable(Channels.WIRE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.WIRE_DLQ)
                .build();
    }

    @Bean
    public Queue ackDlq() {
        return QueueBuilder.durable(Channels.ACK_DLQ).build();
    }

    @Bean
    @DependsOn("ackDlq")
    public Queue ackQueue() {
        return QueueBuilder.durable(Channels.ACK)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.ACK_DLQ)
                .build();
    }

    @Bean
    public Queue returnDlq() {
        return QueueBuilder.durable(Channels.RETURN_DLQ).build();
    }

    @Bean
    @DependsOn("returnDlq")
    public Queue returnQueue() {
        return QueueBuilder.durable(Channels.RETURN)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RETURN_DLQ)
                .build();
    }

    @Bean
    public Queue restoreDlq() {
        return QueueBuilder.durable(Channels.RESTORE_DLQ).build();
    }

    @Bean
    @DependsOn("restoreDlq")
    public Queue restoreQueue() {
        return QueueBuilder.durable(Channels.RESTORE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RESTORE_DLQ)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
