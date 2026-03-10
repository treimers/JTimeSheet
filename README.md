# JTimeSheet

<img src="src/main/deploy/icon-build/JTimeSheet.png" alt="Project logo" width="200">

JTimeSheet is a program for recording working hours and writing timesheets.

# Releases

[![Latest release](https://img.shields.io/github/v/release/treimers/JTimeSheet?label=Download)](https://github.com/treimers/JTimeSheet/releases/latest)

Downloads: **[Latest release](https://github.com/treimers/JTimeSheet/releases/latest)**

## Security Warnings on Startup (Windows / macOS)

After downloading from GitHub, Windows or macOS may block execution because the application is not signed. Here's how you can still start JTimeSheet:

### macOS

On macOS you may see.

<img src="images/macOS-Security.png" alt="Security Warning" width="300">

- **Right-click** on the app → select **"Open"** → confirm **"Open"** again in the dialog.
- Or in Terminal (adjust path):
  ```bash
  xattr -cr /path/to/JTimeSheet.app
  ```

### Windows

Under Windows you might get.

<img src="images/Windows-Security1.png" alt="Security Warning" width="300">

- For the SmartScreen warning, click **"More info"**.
- Then select **"Run anyway"**.

<img src="images/Windows-Security2.png" alt="Security Warning" width="300">