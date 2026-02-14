# Manage – Customers, Projects, and Tasks

Open **Manage** (menu or toolbar) to maintain the hierarchy: **Customers** → **Projects** → **Tasks**. There is a **single customer list**; all changes apply immediately to the same data used by filters and Write Timesheet.

## Customers

- **Add** – New customer. You can set name, address, and **timesheet settings**: **Excel template** (path to an .xls or .xlsx file), **timesheet file name** (suggestion when saving), **sheet number** (sheet in the workbook), and **task separator**. Template layout (rows/columns) is no longer configured per customer; it is discovered from the placeholders **$month**, **$start**, **$end**, **$pause**, **$task** in the template.
- **Rename** – Change the customer name.
- **Delete** – Remove the customer (and all projects/tasks/activities). You will be warned about the number of activities.

Click a customer and use **Properties** (or context menu) to edit timesheet settings. **Important:** The Excel template path must point to an **existing file**. When you click **Save**, this is validated; if the file does not exist, an error is shown and the dialog stays open. **Cancel** discards all changes (the application reloads the last saved state from file).

## Projects and tasks

- Under a customer you can add **Projects**, and under a project, **Tasks**.
- **Rename** and **Delete** work the same way; deletion removes all nested items and related activities.

Activities always reference a customer, project, and task from this tree.
