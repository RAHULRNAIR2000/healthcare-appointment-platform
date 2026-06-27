import os

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "rabbituser")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "rabbitpass")

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "5432"))
DB_NAME = os.getenv("DB_NAME", "healthcare")
DB_USER = os.getenv("DB_USER", "healthuser")
DB_PASSWORD = os.getenv("DB_PASSWORD", "healthpass")

QUEUE_NAME = "appointment.notification.queue"
EXCHANGE_NAME = "appointment.events"
ROUTING_KEY = "appointment.booked"
