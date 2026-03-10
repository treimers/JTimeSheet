# Reminder Test Cases (JSON Specification)

The test cases for the reminder logic are stored in JSON files. Each file contains `_fileDescription` and `_structure` at the top for self-documentation. The tests load these files at runtime; keys with an underscore prefix (`_*`) are used for documentation only.

| File | Method / Logic Under Test | Brief Description |
|------|---------------------------|-------------------|
| **isReminderDueTestCases.json** | `ReminderService.isReminderDue(now, settings)` | When is a reminder **triggered**? (Window + interval boundary) |
| **isWithinWindowTestCases.json** | `ReminderSuggestionLogic.isNowWithinReminderWindow(now, settings)` | Is `now` within the reminder **window**? (Weekday + time, without interval) |
| **reminderSuggestionLogicTestCases.json** | `ReminderSuggestionLogic.compute(…)` | **What** is suggested? (Customer, project, task, time range, blocked, SuggestionType) |

---

## 1. isReminderDueTestCases.json

**Contents:** `defaultSettings` + `cases[]`

A reminder is *due* when all of the following apply: weekday in `weekdays`, time between `windowStart` and `windowEnd`, minute on interval boundary (e.g. :00, :15, :30, :45 for 15 min).

| # | description (from JSON) | expected |
|---|-------------------------|----------|
| 1 | On interval boundary and within window (e.g. Mon 16:15) → due | true |
| 2 | Not on interval boundary (e.g. 16:05 with 15-min) → not due | false |
| 3 | Before window start → not due | false |
| 4 | After window end → not due | false |
| 5 | Not a reminder weekday (Saturday) → not due | false |
| 6 | On :00 boundary within window → due | true |
| 7 | On :15 boundary within window → due | true |
| 8 | On :30 boundary within window → due | true |
| 9 | On :45 boundary within window → due | true |
| 10 | Sunday with custom window (settings override: `weekdays: ["SUNDAY"]`, custom window) | true |

Case 10 uses a per-case `settings` override in the JSON; the default weekdays are replaced by `["SUNDAY"]`.

---

## 2. isWithinWindowTestCases.json

**Contents:** `defaultSettings` + `cases[]`

Checks only: weekday in `weekdays` and time between `windowStart` and `windowEnd`. No interval check (difference from isReminderDue).

| # | description (from JSON) | expected |
|---|-------------------------|----------|
| 1 | Scenario 2: Before window start → not within window | false |
| 2 | Scenario 2: After window end → not within window | false |
| 3 | Scenario 2: Not a reminder weekday (e.g. Saturday) → not within window | false |
| 4 | Scenario 2: Within window and weekday → within window | true |

---

## 3. reminderSuggestionLogicTestCases.json (computeCases)

**Contents:** `defaultSettings` + `computeCases[]`

Each entry: `setup` (customers, activities), `input` (now, lastActivityIndex, contextCustomerId, contextProjectId, fromReminder), `expected` (blockedForReminder, suggestionType, customerId, projectId, taskId, range, …).

Scenarios (numbered from #1 in this file):

| Scenario | Content |
|----------|---------|
| 1 | No activity today → last customer, now–now, 0:00 (DEFAULT_RANGE, Rules.md Scenario 2) |
| 2 | No past activities → first customer/project/task, now–now, 0:00 (DEFAULT_RANGE, Rules.md Scenario 1); empty customer list → null IDs |
| 3 | Last activity ended in the past → GAP from end until now; end exactly "now" → now–now, 0:00 (DEFAULT_RANGE) |
| 4 | Last activity ends in the future → blockedForReminder, Range [now, now] |
| 5 | Add Activity, end in the future → find gap; "now" in running activity → blocked, start=end=now |
| 6 | Customer change in dialog → last activity of that customer or first project/task |
| 7 | No activity today → now–now, 0:00 (DEFAULT_RANGE); gap → GAP (end time not extended) |
| (Time suggestion) | **Rule 4:** Switch to different customer in dialog → last project/task of that customer, range from end of last activity until now |
| (Time suggestion) | **Rule 5:** Switch to different project (customer unchanged) → last task of project, range from end of last activity until now |
| (Time suggestion) | **Rule 4 (Task):** Customer and project unchanged in dialog → end of last activity until now, last task (when switching task: same logic) |

The exact list of cases is in the JSON under `computeCases[]`; the `description` field of each entry corresponds to the display name in the parameterized test.

---

**Adding new cases:** Add a new entry with `description`, inputs, and `expected` under `cases` or `computeCases` in the respective JSON file – the tests will automatically run over them.

---

## Coverage of docs/Rules.md

Each rule from Rules.md is covered by at least one test:

| Rules.md Section | Rules | Test Class / Test Resource |
|------------------|-------|----------------------------|
| **Reminder Time Window** | 1–4 (before/after window, no weekday, within window) | `ReminderWindowRulesTest`, `isWithinWindowTestCases.json` |
| **Reminder Interval** | 1–10 (due, not due, boundaries, Sunday override) | `ReminderIntervalRulesTest`, `ReminderServiceTest`, `isReminderDueTestCases.json` |
| **Reminder Exceptions** | 1: Program start (no reminder on start) | Implementation in `ReminderService.start()` (no immediate onTick call); Test: `ReminderServiceTest.NoReminderOnStartup` (without start: tick triggers nothing) |
| **Reminder Exceptions** | 2: Entry end now/future | `ReminderExceptionRulesTest.hasActivityEndingNowOrInFuture`, `shouldSuppressReminder` |
| **Reminder Exceptions** | 3: No customer/project/task | `ReminderExceptionRulesTest.hasNoCustomerProjectTask`, `shouldSuppressReminder` |
| **Add-Activity Exceptions** | 1–3 (no customers, no projects, no tasks) + hint dialog (message keys) | `AddActivityDialogRulesTest` |
| **Time Suggestions** | 1–3, 4 (different customer), 5 (different project), 4 (different task) | `ReminderSuggestionLogicTest`, `reminderSuggestionLogicTestCases.json` |

**Mapping Time Suggestions (Rules.md table) → JSON test case:**

| Rules.md # | Scenario | Covered by (description in computeCases) |
|------------|----------|----------------------------------------|
| 1 | No past activities, first customer/project/task, now–now, 0:00 | "Scenario 2: First customer, first project, first task, now–now, 0:00 (Rules.md Scenario 1)"; "Scenario 2: No customers → null IDs, now–now" |
| 2 | No activity today, last customer/project/task, now–now, 0:00 | "Scenario 1: No activity today → last customer, now–now, 0:00 (Rules.md Scenario 2)"; "Scenario 7: No activity today → DEFAULT_RANGE" |
| 3 | Past activity today, end of last activity until now, difference | "Scenario 3: Last end not in future…"; "Scenario 3: GAP from last end until now…"; "Scenario 3: Last activity ends exactly at 'now'…" |
| 4 | Switch, different customer, end of last activity until now | "Time suggestion Rule 4: Switch to different customer in dialog…" |
| 5 | Switch, different project, end of last activity until now | "Time suggestion Rule 5: Switch to different project (customer unchanged)…" |
| 4 (Task) | Switch, different task, end of last activity until now | "Time suggestion Rule 4 (Task): Customer and project unchanged in dialog…" |

The rule "on switch … last activity or first entry" is covered in Scenario 6 (context customer with/without activities).

---

# Testing with Separate Data and Settings

For testing, use a different Preferences node via the `JTIMESHEET_PREFS_NODE` environment variable. This gives you separate settings (including the data directory for `jtimesheet.json`). Set the data directory once in Settings for that profile.

**Example (macOS):**
```bash
JTIMESHEET_PREFS_NODE=net/treimers/jtimesheet_test open JTimeSheet.app
```

**Example (Windows CMD):**
```cmd
set JTIMESHEET_PREFS_NODE=net/treimers/jtimesheet_test
JTimeSheet.exe
```
