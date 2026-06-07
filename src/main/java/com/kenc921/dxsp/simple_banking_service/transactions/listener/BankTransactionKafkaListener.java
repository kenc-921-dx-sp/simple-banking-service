package com.kenc921.dxsp.simple_banking_service.transactions.listener;

import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionIncomingMessageDto;
import com.kenc921.dxsp.simple_banking_service.transactions.service.CustomerBankTransactionIncomingMessageIngestionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransactionKafkaListener {

  private final ObjectMapper objectMapper;
  private final Validator validator;
  private final CustomerBankTransactionIncomingMessageIngestionService ingestionService;

  @KafkaListener(
      topics = "${banking-service.transactions.kafka.topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "bankTransactionKafkaListenerContainerFactory")
  public void listen(ConsumerRecord<String, String> record) throws Exception {
    CustomerBankTransactionIncomingMessageDto message;
    try {
      message =
          objectMapper.readValue(record.value(), CustomerBankTransactionIncomingMessageDto.class);
      Set<ConstraintViolation<CustomerBankTransactionIncomingMessageDto>> violations =
          validator.validate(message);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }
    } catch (Exception exception) {
      log.error(
          "Rejected bank transaction Kafka payload. sourceTopic={}, partition={}, offset={}, "
              + "key={}, rawPayload={}",
          record.topic(),
          record.partition(),
          record.offset(),
          record.key(),
          record.value(),
          exception);
      throw exception;
    }

    try {
      ingestionService.ingest(message);
    } catch (Exception exception) {
      log.error(
          "Bank transaction ingestion failed. sourceTopic={}, partition={}, offset={}, key={}, "
              + "rawPayload={}",
          record.topic(),
          record.partition(),
          record.offset(),
          record.key(),
          record.value(),
          exception);
      throw exception;
    }
    log.info(
        "Ingested transaction {} from Kafka topic {} partition {} offset {}",
        message.transactionId(),
        record.topic(),
        record.partition(),
        record.offset());
  }
}
