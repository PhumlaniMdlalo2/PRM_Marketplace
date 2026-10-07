# How to run PRM Marketplace (complete beginner's guide)

You install 4 programs once. After that, running the project is two commands. Written for Windows.

## Part 1 — One-time setup

### Step 1: Install Git

1. Go to **https://git-scm.com/download/win** — the download starts automatically
2. Run the downloaded `.exe` and click **Next** on every screen (all defaults are fine) until **Install**, then **Finish**

### Step 2: Install Java

1. Go to **https://adoptium.net/temurin/releases/?version=21**
2. Under **JDK**, pick **Windows**, **x64**, and download the **`.msi`** file
3. Run it, click **Next** through everything (leave the default checkboxes on), **Install**, **Finish**

### Step 3: Install MySQL (the database)

1. Go to **https://dev.mysql.com/downloads/installer/** and download the largest file (`mysql-installer-community...`, about 400 MB)
2. Run it. If it asks to add products, choose **Custom** → click the arrow to add **MySQL Server** → **Next**
3. Choose **Standalone MySQL Server** → **Next**, keep clicking **Next** with defaults (port stays **3306**)
4. On the password screen: set the root password to exactly `password` (the project expects it)
5. Make sure **"Configure MySQL as a Windows Service"** and **"Start at system startup"** are checked → **Next** → **Execute** → **Finish**

### Step 4: Install Node.js

1. Go to **https://nodejs.org** — click the big **LTS** download
2. Run the `.msi`, click **Next** through everything, **Install**, **Finish**

## Part 2 — Get the project (once)

1. Press the **Windows key**, type `powershell`, press **Enter** (a blue/black window opens — this is the "terminal")
2. Type these lines one at a time, pressing **Enter** after each:

```powershell
git clone https://github.com/PhumlaniMdlalo2/PRM_Marketplace.git
cd PRM_Marketplace
```

You now have the project at `C:\Users\<your name>\PRM_Marketplace`.

Everything is already configured in the project (email settings etc.) — you don't need to edit anything.

## Part 3 — Run it (do this every time)

You need **two terminal windows open at the same time**.

### Window 1 — the backend

Open PowerShell (Windows key → type `powershell` → Enter), then:

```powershell
cd PRM_Marketplace\backend
.\mvnw.cmd spring-boot:run
```

- The **first time** it downloads things for a few minutes — wait
- Success looks like: **`Started PrmMarketplaceApplication`**
- Leave this window open. If it ever closes, the app is off.

### Window 1 alternative — the backend in IntelliJ

Prefer a click over a command? Use IntelliJ IDEA for Window 1 instead:

1. **File → Open** → select the `PRM_Marketplace` folder → **OK**
2. Wait for the import to finish (progress bar at the bottom) — the first time it downloads dependencies
3. If it asks for a **JDK/SDK**, pick the Java you installed in Part 1, Step 2
4. Open `backend/src/main/java/za/ac/cput/prm_marketplace/PrmMarketplaceApplication.java` and click the **green play ▶** button
5. Success is the same: **`Started PrmMarketplaceApplication`**. Stop it with the **red ■** button

The project's settings are picked up automatically — nothing extra to configure in IntelliJ.

### Window 2 — the frontend

Open a **second** PowerShell window, then:

```powershell
cd PRM_Marketplace\frontend
npm install
npm run dev
```

- `npm install` only does anything the first time (a few minutes)
- Success looks like: **`Local: http://localhost:5173/`**

No separate terminal needed if you use IntelliJ: the **Terminal** tab at the bottom of the window works for these commands too.

### Open the app

In your browser, go to **http://localhost:5173**

**First-time test:** click Sign Up, register with your CPUT email (`@mycput.ac.za`). You should receive a **verification-code email within a minute** — sender is `vendra.market.sa@gmail.com`. Check your spam folder if not. Enter the code and you're in.

### Stop the app

Go to each terminal window and press **Ctrl + C** (or click the red **■** in IntelliJ). To start again later, just repeat the two window commands (skip `npm install` — it never needs repeating).

## When something goes wrong

| Problem | Fix |
| --- | --- |
| Backend says `Port 8080 was already in use` | An old copy is still running — close extra PowerShell windows, or restart the PC |
| Backend errors about the database (`Access denied` / `communications link failure`) | MySQL isn't running: press **Win+R**, type `services.msc`, find **MySQL80**, right-click → **Start**. The password must be `password` |
| No verification email | Wait a minute and check spam. Still nothing, open `http://localhost:8080/actuator/health/readiness` in a browser — if it's not `UP`, mail is down |
| PowerShell script errors | You opened the wrong window — it must be **PowerShell**, and the path must be `PRM_Marketplace\frontend` (check with `pwd`) |
