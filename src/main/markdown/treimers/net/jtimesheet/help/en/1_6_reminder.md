# Reminder and Time Grid

The **Reminder** opens a dialog at configurable intervals so you can record an activity. The **Time Grid** defines which minute values can be used for times in the app (rounding of start/end). Both are configured in **Settings**.

---

## Settings Overview

| Setting | Meaning |
|---------|---------|
| **Reminder interval** | Time between two reminder popups (e.g. 15 min → :00, :15, :30, :45). Allowed: 1–60 minutes, must divide 60 (1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60). |
| **Reminder start / end** | Time-of-day window when the reminder is active (e.g. 9:00–17:00). No reminder outside this window. |
| **Reminder weekdays** | Days of the week when the reminder is active (e.g. Mon–Fri). |
| **Time grid** | Grid for time entry (e.g. 15 min → only :00, :15, :30, :45). Allowed: 1–60 minutes, must divide 60. |

**Reminder interval and time grid are independent:** The reminder interval only controls **when** the dialog appears. The time grid controls **which times** you can choose in the dialog and in suggestions (and how the end time is rounded while the dialog is open). You can e.g. use a 15-minute reminder interval with a 5-minute time grid.

---

## When Does What Apply?

| Situation | Behaviour |
|-----------|------------|
| **Application startup** | If the current time is inside the reminder window (start–end, weekday), the reminder dialog appears **immediately** – you do not wait until the next interval boundary. |
| **While the app is running** | The next reminder is scheduled for the **next interval boundary** (e.g. with 15 min: 9:00, 9:15, 9:30, …). Only within the window and on configured weekdays. |
| **After closing the dialog** | The next popup is again at the next interval boundary (e.g. dialog closed at 10:15 → next reminder at 10:30). |
| **Dialog stays open** | The **end time** in the dialog is updated automatically: at each **reminder interval boundary** (e.g. every quarter hour), “To” is set to the current time (aligned to the time grid) and the duration is recalculated. Only when the selected date is **today**. |
| **Outside the window / other weekday** | No reminder; the next run is scheduled for the next valid time (next day or window start). |

---

## Behaviour on Startup (Example)

- **Settings:** Reminder 9:00–17:00, Mon–Fri, interval 15 min.  
- **Start at 11:04:** You are inside the window on a reminder weekday → the reminder dialog appears **immediately** (not at 11:15).  
- **Start at 08:30:** Before window start → first reminder at 9:00.  
- **Start on Saturday 10:00:** No reminder on that weekday; first reminder on Monday at the next interval boundary inside the window.

---

## Dialog Stays Open (End Time Moves Forward)

If you **leave the reminder dialog open**, the end time is kept in sync with the current time:

- The dialog opens e.g. at 10:15 with suggestion “To 10:15”.  
- You leave it open. At **10:30** (next interval boundary), **“To” is automatically set to 10:30** and the duration is updated.  
- This only happens when the date selected in the dialog is **today**.  
- The first update is at the **next interval boundary** after opening (not simply “interval minutes after open”). Then at every following boundary until the dialog is closed.

---

## Reminder Interval vs Time Grid

| Aspect | Reminder interval | Time grid |
|--------|-------------------|-----------|
| **Purpose** | When the reminder dialog is shown. | Grid for start/end times in the dialog and for suggestions (e.g. “From–To”). |
| **Example 15 min** | Reminder at :00, :15, :30, :45. | Only :00, :15, :30, :45 selectable/suggested. |
| **Independent?** | Yes. You can e.g. use a 15-minute reminder with a 5-minute time grid (reminder on quarter hours, time entry in 5-minute steps). |

---

## Short: Timeline Example

- **Window:** 9:00–17:00, **interval:** 15 min.  
- Reminder times on a weekday: 9:00, 9:15, 9:30, … 16:45, 17:00.  
- **Start at 11:04** → dialog immediately.  
- **Dialog open from 10:15 to 10:50** → “To” is automatically updated to the current time at 10:30 and 10:45 (when date = today).
