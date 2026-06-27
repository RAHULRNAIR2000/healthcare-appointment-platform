import psycopg2
import psycopg2.extras
from app.config import DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD


def get_connection():
    return psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
    )


def get_appointment_status(appointment_id: str, cursor) -> str:
    cursor.execute("SELECT status FROM appointments WHERE id = %s", (appointment_id,))
    row = cursor.fetchone()
    return row[0] if row else None


def confirm_appointment(appointment_id: str, patient_email: str, doctor_name: str) -> bool:
    """Returns True if confirmed, False if skipped (e.g. already cancelled)."""
    conn = get_connection()
    try:
        with conn:
            with conn.cursor() as cur:
                current_status = get_appointment_status(appointment_id, cur)

                if current_status != "PENDING":
                    return False

                cur.execute(
                    """
                    INSERT INTO appointment_logs (appointment_id, event_type, message)
                    VALUES (%s, %s, %s)
                    """,
                    (
                        appointment_id,
                        "NOTIFICATION_SENT",
                        f"Notification sent to {patient_email} for appointment with {doctor_name}",
                    ),
                )

                cur.execute(
                    "UPDATE appointments SET status = 'CONFIRMED', updated_at = NOW() WHERE id = %s",
                    (appointment_id,),
                )

                cur.execute(
                    """
                    INSERT INTO appointment_logs (appointment_id, event_type, message)
                    VALUES (%s, %s, %s)
                    """,
                    (
                        appointment_id,
                        "CONFIRMED",
                        f"Appointment confirmed for {patient_email} with {doctor_name}",
                    ),
                )
                return True
    finally:
        conn.close()
