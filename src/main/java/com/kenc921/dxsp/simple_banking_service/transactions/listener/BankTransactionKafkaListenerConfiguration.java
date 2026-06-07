package com.kenc921.dxsp.simple_banking_service.transactions.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class BankTransactionKafkaListenerConfiguration {

  @Bean
  ConcurrentKafkaListenerContainerFactory<Object, Object>
      bankTransactionKafkaListenerContainerFactory(
          ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
          ConsumerFactory<Object, Object> consumerFactory,
          KafkaTemplate<Object, Object> kafkaTemplate,
          @Value("${banking-service.transactions.kafka.dead-letter-topic}")
              String deadLetterTopic) {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, consumerFactory);
    factory.setCommonErrorHandler(bankTransactionErrorHandler(kafkaTemplate, deadLetterTopic));
    return factory;
  }

  DefaultErrorHandler bankTransactionErrorHandler(
      KafkaTemplate<Object, Object> kafkaTemplate, String deadLetterTopic) {
    return new DefaultErrorHandler(
        bankTransactionRecoverer(kafkaTemplate, deadLetterTopic), new FixedBackOff(0L, 0L));
  }

  ConsumerAwareRecordRecoverer bankTransactionRecoverer(
      KafkaOperations<Object, Object> kafkaOperations, String deadLetterTopic) {
    DeadLetterPublishingRecoverer publisher =
        new DeadLetterPublishingRecoverer(
            kafkaOperations, (record, exception) -> new TopicPartition(deadLetterTopic, -1));
    publisher.setFailIfSendResultIsError(true);

    ConsumerAwareRecordRecoverer recoverer =
        (record, consumer, exception) -> {
          log.error(
              "Publishing rejected bank transaction to dead-letter topic. "
                  + "sourceTopic={}, partition={}, offset={}, key={}, rawPayload={}, "
                  + "targetDlq={}",
              record.topic(),
              record.partition(),
              record.offset(),
              record.key(),
              record.value(),
              deadLetterTopic,
              exception);
          publisher.accept(record, consumer, exception);
          log.info(
              "Rejected bank transaction published to dead-letter topic. "
                  + "sourceTopic={}, partition={}, offset={}, key={}, rawPayload={}, "
                  + "targetDlq={}",
              record.topic(),
              record.partition(),
              record.offset(),
              record.key(),
              record.value(),
              deadLetterTopic);
        };
    return recoverer;
  }
}
