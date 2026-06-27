import unittest
from unittest.mock import patch, MagicMock, call


class TestGetAppointmentStatus(unittest.TestCase):

    def test_returns_pending_status(self):
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = ("PENDING",)

        from app.db import get_appointment_status
        result = get_appointment_status("some-uuid", mock_cursor)

        self.assertEqual(result, "PENDING")
        mock_cursor.execute.assert_called_once_with(
            "SELECT status FROM appointments WHERE id = %s", ("some-uuid",)
        )

    def test_returns_cancelled_status(self):
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = ("CANCELLED",)

        from app.db import get_appointment_status
        result = get_appointment_status("some-uuid", mock_cursor)

        self.assertEqual(result, "CANCELLED")

    def test_returns_none_when_appointment_not_found(self):
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = None

        from app.db import get_appointment_status
        result = get_appointment_status("nonexistent-uuid", mock_cursor)

        self.assertIsNone(result)


class TestConfirmAppointment(unittest.TestCase):

    def _make_mock_conn(self, status):
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = (status,) if status else None
        mock_cursor.__enter__ = lambda s: s
        mock_cursor.__exit__ = MagicMock(return_value=False)

        mock_conn = MagicMock()
        mock_conn.cursor.return_value = mock_cursor
        mock_conn.__enter__ = lambda s: s
        mock_conn.__exit__ = MagicMock(return_value=False)

        return mock_conn, mock_cursor

    @patch("app.db.get_connection")
    def test_pending_appointment_gets_confirmed(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("PENDING")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        result = confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        self.assertTrue(result)

    @patch("app.db.get_connection")
    def test_pending_appointment_inserts_notification_sent_log(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("PENDING")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        insert_calls = [str(c) for c in mock_cursor.execute.call_args_list]
        self.assertTrue(any("NOTIFICATION_SENT" in c for c in insert_calls))

    @patch("app.db.get_connection")
    def test_pending_appointment_updates_status_to_confirmed(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("PENDING")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        all_calls = [str(c) for c in mock_cursor.execute.call_args_list]
        self.assertTrue(any("CONFIRMED" in c and "UPDATE" in c for c in all_calls))

    @patch("app.db.get_connection")
    def test_pending_appointment_inserts_confirmed_log(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("PENDING")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        insert_calls = [str(c) for c in mock_cursor.execute.call_args_list]
        confirmed_inserts = [c for c in insert_calls if "CONFIRMED" in c and "INSERT" in c]
        self.assertTrue(len(confirmed_inserts) >= 1)

    @patch("app.db.get_connection")
    def test_cancelled_appointment_returns_false(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("CANCELLED")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        result = confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        self.assertFalse(result)

    @patch("app.db.get_connection")
    def test_cancelled_appointment_does_not_update_status(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("CANCELLED")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        all_calls = [str(c) for c in mock_cursor.execute.call_args_list]
        self.assertFalse(any("UPDATE" in c for c in all_calls))

    @patch("app.db.get_connection")
    def test_cancelled_appointment_does_not_insert_logs(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("CANCELLED")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        all_calls = [str(c) for c in mock_cursor.execute.call_args_list]
        self.assertFalse(any("NOTIFICATION_SENT" in c for c in all_calls))

    @patch("app.db.get_connection")
    def test_confirmed_appointment_is_skipped(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("CONFIRMED")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        result = confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        self.assertFalse(result)

    @patch("app.db.get_connection")
    def test_connection_closed_even_on_success(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("PENDING")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        mock_conn.close.assert_called_once()

    @patch("app.db.get_connection")
    def test_connection_closed_even_when_skipped(self, mock_get_conn):
        mock_conn, mock_cursor = self._make_mock_conn("CANCELLED")
        mock_get_conn.return_value = mock_conn

        from app.db import confirm_appointment
        confirm_appointment("appt-id", "patient@example.com", "Dr. Anjali Sharma")

        mock_conn.close.assert_called_once()


if __name__ == "__main__":
    unittest.main()
