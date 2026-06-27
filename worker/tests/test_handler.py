import json
import unittest
from unittest.mock import patch, MagicMock
from app.handler import handle_appointment_booked


def make_event(**overrides):
    event = {
        "appointmentId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        "patientEmail": "patient@example.com",
        "patientName": "John Doe",
        "doctorName": "Dr. Anjali Sharma",
        "hospitalName": "Apollo Hospital",
        "slotStart": "2026-07-01T09:00:00Z",
        "slotEnd": "2026-07-01T09:30:00Z",
    }
    event.update(overrides)
    return json.dumps(event).encode()


class TestHandleAppointmentBooked(unittest.TestCase):

    @patch("app.handler.confirm_appointment", return_value=True)
    def test_happy_path_calls_confirm_with_correct_args(self, mock_confirm):
        handle_appointment_booked(make_event())

        mock_confirm.assert_called_once_with(
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "patient@example.com",
            "Dr. Anjali Sharma",
        )

    @patch("app.handler.confirm_appointment", return_value=True)
    def test_happy_path_logs_notification_sent(self, mock_confirm):
        with self.assertLogs("app.handler", level="INFO") as log:
            handle_appointment_booked(make_event())

        messages = "\n".join(log.output)
        self.assertIn("Notification sent to patient@example.com", messages)
        self.assertIn("Dr. Anjali Sharma", messages)

    @patch("app.handler.confirm_appointment", return_value=True)
    def test_happy_path_logs_confirmed(self, mock_confirm):
        with self.assertLogs("app.handler", level="INFO") as log:
            handle_appointment_booked(make_event())

        messages = "\n".join(log.output)
        self.assertIn("CONFIRMED", messages)
        self.assertIn("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", messages)

    @patch("app.handler.confirm_appointment", return_value=False)
    def test_cancelled_appointment_logs_warning_and_skips(self, mock_confirm):
        with self.assertLogs("app.handler", level="WARNING") as log:
            handle_appointment_booked(make_event())

        messages = "\n".join(log.output)
        self.assertIn("Skipped", messages)
        self.assertIn("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", messages)

    @patch("app.handler.confirm_appointment", return_value=False)
    def test_cancelled_appointment_does_not_log_notification_sent(self, mock_confirm):
        with self.assertLogs("app.handler", level="WARNING") as log:
            handle_appointment_booked(make_event())

        messages = "\n".join(log.output)
        self.assertNotIn("Notification sent", messages)

    @patch("app.handler.confirm_appointment", return_value=True)
    def test_parses_all_fields_from_event_correctly(self, mock_confirm):
        event = make_event(
            appointmentId="11111111-2222-3333-4444-555555555555",
            patientEmail="other@example.com",
            doctorName="Dr. Ravi Kumar",
            hospitalName="Manipal Hospital",
        )
        handle_appointment_booked(event)

        mock_confirm.assert_called_once_with(
            "11111111-2222-3333-4444-555555555555",
            "other@example.com",
            "Dr. Ravi Kumar",
        )


class TestHandleAppointmentBookedEdgeCases(unittest.TestCase):

    @patch("app.handler.confirm_appointment", side_effect=Exception("DB connection failed"))
    def test_db_failure_raises_exception(self, mock_confirm):
        with self.assertRaises(Exception) as ctx:
            handle_appointment_booked(make_event())

        self.assertIn("DB connection failed", str(ctx.exception))

    def test_invalid_json_raises_exception(self):
        with self.assertRaises(Exception):
            handle_appointment_booked(b"not valid json")

    def test_missing_field_raises_key_error(self):
        incomplete = json.dumps({"appointmentId": "abc"}).encode()
        with self.assertRaises(KeyError):
            handle_appointment_booked(incomplete)


if __name__ == "__main__":
    unittest.main()
