package com.kenc921.dxsp.simple_banking_service.transactions.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;

class BankTransactionKafkaListenerConfigurationTest {

  private static final String DEAD_LETTER_TOPIC = "account-transactions-dlq";

  private final BankTransactionKafkaListenerConfiguration configuration =
      new BankTransactionKafkaListenerConfiguration();

  @Test
  void publishesOriginalKeyAndRawPayloadToTransactionDeadLetterTopic() {
    AtomicReference<ProducerRecord<Object, Object>> publishedRecord = new AtomicReference<>();
    KafkaOperations<Object, Object> kafkaOperations =
        kafkaOperations(
            record -> {
              publishedRecord.set(record);
              return CompletableFuture.completedFuture(null);
            });
    ConsumerAwareRecordRecoverer recoverer =
        configuration.bankTransactionRecoverer(kafkaOperations, DEAD_LETTER_TOPIC);
    ConsumerRecord<String, String> sourceRecord =
        new ConsumerRecord<>("account-transactions", 2, 41L, "transaction-key", "{bad-json}");

    recoverer.accept(sourceRecord, null, new IllegalArgumentException("rejected"));

    assertThat(publishedRecord.get().topic()).isEqualTo(DEAD_LETTER_TOPIC);
    assertThat(publishedRecord.get().key()).isEqualTo("transaction-key");
    assertThat(publishedRecord.get().value()).isEqualTo("{bad-json}");
  }

  @Test
  void propagatesDeadLetterPublicationFailure() {
    CompletableFuture<?> failedSend = new CompletableFuture<>();
    failedSend.completeExceptionally(new IllegalStateException("Kafka unavailable"));
    KafkaOperations<Object, Object> kafkaOperations =
        kafkaOperations(record -> (CompletableFuture) failedSend);
    ConsumerAwareRecordRecoverer recoverer =
        configuration.bankTransactionRecoverer(kafkaOperations, DEAD_LETTER_TOPIC);
    ConsumerRecord<String, String> sourceRecord =
        new ConsumerRecord<>("account-transactions", 2, 41L, "transaction-key", "{bad-json}");

    assertThatThrownBy(
            () ->
                recoverer.accept(
                    sourceRecord, null, new IllegalArgumentException("rejected transaction")))
        .isInstanceOf(RuntimeException.class)
        .hasRootCauseMessage("Kafka unavailable");
  }

  @SuppressWarnings("unchecked")
  private static KafkaOperations<Object, Object> kafkaOperations(Sender sender) {
    return (KafkaOperations<Object, Object>)
        Proxy.newProxyInstance(
            KafkaOperations.class.getClassLoader(),
            new Class<?>[] {KafkaOperations.class},
            (proxy, method, args) -> {
              if (method.getName().equals("send") && args[0] instanceof ProducerRecord<?, ?>) {
                return sender.send((ProducerRecord<Object, Object>) args[0]);
              }
              if (method.getName().equals("isTransactional")) {
                return false;
              }
              if (method.getName().equals("getProducerFactory")) {
                return null;
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }

  @FunctionalInterface
  private interface Sender {
    CompletableFuture<?> send(ProducerRecord<Object, Object> record);
  }
}
