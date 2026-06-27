import logging
import time
import pika
from app.config import (
    RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASSWORD,
    QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY,
)
from app.handler import handle_appointment_booked

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


def on_message(channel, method, properties, body):
    try:
        handle_appointment_booked(body)
        channel.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as e:
        logger.error("Failed to process message: %s", e, exc_info=True)
        # nack without requeue to avoid poison-message loop
        channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def connect_with_retry(max_retries=10, delay=3):
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
    params = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials,
        heartbeat=60,
    )
    for attempt in range(1, max_retries + 1):
        try:
            connection = pika.BlockingConnection(params)
            logger.info("Connected to RabbitMQ at %s:%s", RABBITMQ_HOST, RABBITMQ_PORT)
            return connection
        except Exception as e:
            logger.warning("RabbitMQ not ready (attempt %d/%d): %s", attempt, max_retries, e)
            time.sleep(delay)
    raise RuntimeError("Could not connect to RabbitMQ after %d attempts" % max_retries)


def main():
    logger.info("Starting healthcare notification worker...")

    connection = connect_with_retry()
    channel = connection.channel()

    channel.exchange_declare(exchange=EXCHANGE_NAME, exchange_type="topic", durable=True)
    channel.queue_declare(queue=QUEUE_NAME, durable=True)
    channel.queue_bind(queue=QUEUE_NAME, exchange=EXCHANGE_NAME, routing_key=ROUTING_KEY)

    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=QUEUE_NAME, on_message_callback=on_message)

    logger.info("Waiting for messages on queue: %s", QUEUE_NAME)
    channel.start_consuming()


if __name__ == "__main__":
    main()
