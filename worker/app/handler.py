import json
import logging
from app.db import confirm_appointment

logger = logging.getLogger(__name__)


def handle_appointment_booked(body: bytes):
    event = json.loads(body)

    appointment_id = event["appointmentId"]
    patient_email = event["patientEmail"]
    patient_name = event["patientName"]
    doctor_name = event["doctorName"]
    hospital_name = event["hospitalName"]
    slot_start = event["slotStart"]

    logger.info(
        "Processing appointment.booked | id=%s | patient=%s | doctor=%s | slot=%s",
        appointment_id,
        patient_email,
        doctor_name,
        slot_start,
    )

    confirmed = confirm_appointment(appointment_id, patient_email, doctor_name)

    if confirmed:
        logger.info(
            "Notification sent to %s for appointment with %s at %s (%s)",
            patient_email,
            doctor_name,
            slot_start,
            hospital_name,
        )
        logger.info("Appointment %s status updated to CONFIRMED", appointment_id)
    else:
        logger.warning(
            "Skipped appointment %s — status is not PENDING (likely cancelled before processing)",
            appointment_id,
        )
