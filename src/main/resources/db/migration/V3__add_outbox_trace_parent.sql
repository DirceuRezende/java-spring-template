-- W3C traceparent da requisição que gerou a mensagem: o relay continua o mesmo trace ao publicar
ALTER TABLE outbox ADD COLUMN trace_parent VARCHAR(55);
