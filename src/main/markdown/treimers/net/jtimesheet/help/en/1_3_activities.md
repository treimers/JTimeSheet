# Activities

Activities are time entries with a customer, project, task, and start/end time.

## Adding an activity

- **Menu:** Activity → Add Activity  
- **Toolbar:** Click the “Add” button  
- **Context menu:** Right-click in the table → Add Activity  

In the dialog, select customer, project, and task, then set **From** and **To**. Duration is calculated automatically. Save with **Save**.

## Editing and deleting

- **Edit:** Select a row and use Activity → Edit Activity (or double-click / context menu).
- **Delete:** Select a row and use Activity → Delete Activity (or context menu). Confirm the deletion.

## Add Break

If an activity should be split (e.g. to reflect a break), select the activity and use **Activity → Add Break**. In the dialog, set the break **From** and **To** within the activity’s time range. The activity is then split into two: one ending at the break start and one starting at the break end. The break period itself is not stored as a separate entry; it is simply the gap between the two resulting activities.

## Time overlap (conflict)

When you **add** or **edit** an activity, the app checks whether the chosen time range overlaps with any existing activity (across all customers). If it does, a **Time overlap** dialog appears:

- It lists all activities that overlap with your chosen time.
- You can **Apply anyway** to keep the entered time (overlaps remain).
- If free slots exist, **Move behind** or **Move before** suggest a conflict-free slot (same duration) and show the exact time range. Choosing one applies that slot; the dialog is not shown again.
- **Cancel** closes the overlap dialog and returns you to the Add or Edit dialog with your last values so you can change the time or cancel there.

Overlap detection runs only when saving from the Add or Edit dialog. Suggested times when opening Add (or when changing customer/project) are not checked in advance.

## Consolidate

If you have several short activities that belong together (e.g. same task, one after the other), select the range and use **Consolidate**. The activities are merged into one (first start, last end). This is useful before writing a timesheet.
