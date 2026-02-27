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

## When Does the Reminder Appear?

| Situation | Behaviour |
|-----------|------------|
| **Application startup** | **No reminder** – no dialog is shown on startup. The first reminder is scheduled for the **next interval boundary** inside the window (e.g. start at 11:04 → first reminder at 11:15). |
| **While the app is running** | The next reminder is scheduled for the **next interval boundary** (e.g. with 15 min: 9:00, 9:15, 9:30, …). Only **inside** the window (start–end) and on configured **reminder weekdays**. |
| **Outside the window / other weekday** | **No reminder**; the next run is scheduled for the next valid time (next day or window start). |
| **Last activity ends in the future** | **No reminder** – you are e.g. inside an activity that ends later. The next reminder will appear at the next interval boundary. |
| **After closing the dialog** | The next popup is again at the next interval boundary (e.g. dialog closed at 10:15 → next reminder at 10:30). |

---

## What Does the Reminder Dialog Suggest?

The reminder suggests **customer, project, task** and a **time range (From–To)**. The logic:

| Situation | Suggestion |
|-----------|------------|
| **No activity today** (across all customers) | **Last customer** (or last customer activity) as default; if there are **no past activities at all**: **first customer**, **first project**, **first task**. Time: **From = now − 1 hour**, **To = now**. |
| **Last activity today ended in the past** | That activity as default (customer/project/task). Time: **From = end of last activity**, **To = now** (the “gap” up to the current time). |
| **Changing customer in the dialog** | For the selected customer: **last activity** of that customer (including previous days) as default, otherwise **first project** and **first task** of that customer. Time: same logic as above (gap or now−1h to now). |

**Note:** The same suggestion logic applies when using **Activity → Add activity** (without reminder). If the last activity’s end is in the **future** (e.g. an ongoing activity), “Add activity” will either suggest the **last gap** (if there is free time until now) or open a dialog with **From = To = now** (duration 0) so you can still record an activity. In that case the reminder does not appear.

---

## Behaviour on Startup (Example)

- **Settings:** Reminder 9:00–17:00, Mon–Fri, interval 15 min.  
- **Start at 11:04:** Inside the window on a reminder weekday → **no** dialog immediately; **first reminder at 11:15**.  
- **Start at 08:30:** Before window start → first reminder at 9:00.  
- **Start on Saturday 10:00:** No reminder on that weekday; first reminder on Monday at the next interval boundary inside the window.

---

## Dialog Stays Open (End Time Moves Forward)

If you **leave the reminder dialog open** with the end time at **“To = now”**:

- **Default case** (e.g. no activity today, suggestion “From = now−1h, To = now”):  
  At the **next reminder interval boundary**, **“To” is automatically set to the new current time** and the duration is recalculated. So you can e.g. open the dialog at 10:15 and at 10:30 it appears again with “To 10:30”.
- **When the reminder suggests a gap** (From = end of last activity, To = now):  
  The **end time is not moved forward** – at the next interval the dialog appears again with the same gap (same From–To) until you record an activity or close it.
- This only applies when the selected date is **today**.

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
- **Start at 11:04** → first reminder at 11:15 (no dialog immediately).  
- **Dialog with suggestion “now−1h to now” open from 10:15 to 10:50** → “To” is updated to the current time at 10:30 and 10:45 (when date = today). When a **gap** is suggested, From–To stays unchanged.
