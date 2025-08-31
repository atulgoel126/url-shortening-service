### 1\. Product Overview & Goal

**LinkSplit** is a URL shortening service that allows content creators to monetize their shared links. It works by showing a brief interstitial advertisement to the end-user before redirecting them to the final destination.

The goal of this POC is to build a functional, single-server application that validates the core user journey for both creators and end-users, tracking ad views and estimated earnings.

  * **Target Users:** Bloggers, social media influencers, forum posters, and content creators.
  * **Key Differentiator:** Simplicity and transparency in tracking ad-view-based earnings.
  * **Monetization:** Google AdSense on the interstitial page.

-----

### 2\. User Personas & Stories

#### a. The Creator (e.g., "Priya, the Blogger")

  * **As a Creator,** I want to paste a long URL and receive a short, shareable link instantly, so I can post it on my blog and social media.
  * **As a Creator,** I want to create an account and log in, so I can manage all my links in one place.
  * **As a Creator,** I want to view a dashboard that shows each of my links, the number of ad views they've received, and my estimated earnings, so I can see which links are performing best.
  * **As a Creator,** I want assurance that my original URL, including any affiliate tags, remains unchanged.

#### b. The End-User (e.g., "Ravi, the Reader")

  * **As an End-User,** when I click a LinkSplit URL, I want to understand that I'll be shown an ad before continuing to my destination.
  * **As an End-User,** I want the ad page to be fast, clear, and have a non-intrusive countdown timer.
  * **As an End-User,** I want a clear "Skip Ad" or "Continue" button to appear quickly so I can proceed to the content I wanted to see.

-----

### 3\. Core Features & Functional Requirements

#### a. Public Homepage (No Login Required)

  * A clean, mobile-friendly landing page.
  * A prominent input field to paste a long URL.
  * A "Shorten Link" button.
  * Anonymous users can create links, but they won't be tied to an account for earnings tracking.

#### b. URL Shortening & Redirection

  * **Input:** A valid long URL (e.g., `https://www.amazon.in/product?tag=priya-21`).
  * **Process:** The system generates a unique, short, non-sequential code (e.g., `LnkS.pl/g8xT1`). The generation will use a Base62 encoding of an auto-incrementing database ID.
  * **Output:** The shortened URL is displayed for the user to copy.

#### c. Interstitial Ad Page

  * This page is shown to the End-User after they click a short link.
  * **Layout:**
      * A simple header: "You are being redirected to your destination..."
      * A large, centered **Google AdSense ad unit** (e.g., a 320x250 or responsive unit).
      * A message: "Please wait **5** seconds..." where the number counts down.
      * A "Skip Ad" button, which is **disabled** by default and becomes **active** after the 5-second countdown.
  * **Logic:** Once the "Skip Ad" button is clicked, the page redirects to the original `long_url`.

#### d. User Authentication & Dashboard

  * **Auth:** Simple email and password registration and login. Passwords must be securely hashed.
  * **Dashboard (Login Required):**
      * A summary view showing **Total Views** and **Total Estimated Earnings** across all links.
      * A table listing all links created by the user:
          * Short URL
          * Original URL (truncated for display)
          * View Count
          * Estimated Earnings
          * Created Date
          * A "Copy" button for the short URL.

#### e. Analytics & Earnings Calculation

  * **View Tracking:** A "view" is counted **once** when the interstitial ad page is successfully loaded by a unique user.
  * **Fraud Prevention (Basic):** To keep it robust, the system will only log one view per link, per IP address, per hour to prevent simple refresh spam.
  * **Earnings Logic:**
      * The system will have a configurable admin setting for the **Average CPM** (e.g., `$1.50`). This is an estimate since AdSense payouts vary.
      * The system will have a configurable **Revenue Share** (e.g., Creator gets 70%).
      * Formula: `Link Earnings = (Link Views / 1000) * $1.50 * 0.70`
      * This calculation will be run periodically (e.g., every few hours by a background job) to update the dashboard.

-----

### 4\. Non-Functional Requirements

  * **Performance:** The initial redirect from the short URL to the interstitial page must be fast (\< 200ms). The database lookup should be cached.
  * **Reliability:** The application should be packaged as a self-contained executable JAR and run as a system service (e.g., using `systemd` on Linux) to ensure it restarts on failure. The database should be configured for regular backups.
  * **Security:**
      * All traffic must be served over **HTTPS**.
      * Passwords must be hashed using **bcrypt**.
      * Use prepared statements to prevent SQL injection.
  * **Usability:** The entire interface must be mobile-first, clean, and intuitive.

-----

### 5\. Technical Specification (POC)

This POC is designed for simplicity and robustness on a single machine.

#### a. Tech Stack

  * **Backend:** **Java 17+** with **Spring Boot 3+**. This provides a built-in web server (Tomcat), dependency injection, and robust database integration.
  * **Database:** **PostgreSQL 15+**. It's reliable, open-source, and works great with Java/Spring.
  * **Frontend:** **Thymeleaf** (Server-Side Templating). It integrates seamlessly with Spring Boot and is perfect for building simple, mobile-friendly UIs without a complex JavaScript framework. We'll use a lightweight CSS framework like **Bootstrap 5** for responsiveness.
  * **Cache:** **Caffeine** (In-memory cache). We will cache the `short_code -> long_url` mapping to reduce database hits on the critical redirect path.

#### b. System Architecture (Single Machine)

```
+-------------------------------------------------------------+
|                        YOUR SERVER (VM)                     |
|                                                             |
|   +-----------------------------------------------------+   |
|   |         LinkSplit Application (Spring Boot JAR)     |   |
|   |                                                     |   |
|   |  +-----------------+      +-----------------------+ |   |
|   |  | Web Controller  |----->|   URL Service Logic   | |   |
|   |  | (API Endpoints) |      | (Shorten, Redirect)   | |   |
|   |  +-----------------+      +-----------------------+ |   |
|   |         ^                            |              |   |
|   |         |                            v              |   |
|   |  +------------------------------------------------+ |   |
|   |  |    In-Memory Cache (Caffeine) for hot links    | |   |
|   |  +------------------------------------------------+ |   |
|   |         |                            |              |   |
|   |         v                            v              |   |
|   |  +------------------------------------------------+ |   |
|   |  |     Spring Data JPA (Repository Layer)         | |   |
|   |  +------------------------------------------------+ |   |
|   |                                                     |   |
|   +--------------------------|--------------------------+   |
|                              |                              |
|                              v                              |
|   +-----------------------------------------------------+   |
|   |                  PostgreSQL Database                |   |
|   |  (users, links, link_views tables)                  |   |
|   +-----------------------------------------------------+   |
|                                                             |
+-------------------------------------------------------------+
```

#### c. Database Schema

```sql
-- For storing user accounts
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- For storing the URL mappings and linking to a user
CREATE TABLE links (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id), -- Can be NULL for anonymous links
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- For tracking each valid ad view for analytics
CREATE TABLE link_views (
    id BIGSERIAL PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES links(id),
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT
);
```

#### d. API Endpoints (Core Logic)

  * `GET /{shortCode}`: The main redirection endpoint. It looks up the code, logs the view attempt, and redirects to the interstitial ad page.
  * `GET /ad-page?id={shortCode}`: Renders the interstitial page with the AdSense script.
  * `POST /api/url`: Creates a new short URL. Takes `{ "longUrl": "..." }` as JSON body.
  * `GET /dashboard`: (Authenticated) Renders the user's dashboard page.
  * `POST /register`, `POST /login`: Standard authentication endpoints.

-----

### 6\. POC Limitations & Future Scope

  * **No Payout System:** This POC tracks *estimated* earnings only. A real payment and withdrawal system (e.g., via PayPal API) is out of scope.
  * **Basic Admin Panel:** A simple database viewer can be used for admin tasks like disabling a malicious link. A full admin dashboard is not included.
  * **Single Point of Failure:** The entire system runs on one machine. A production system would require a separate database, load balancers, and redundant app servers.
  * **Future Scope:**
      * Implement a real payout system.
      * Develop an admin dashboard for user and link management.
      * Integrate ad mediation (e.g., Google Ad Manager) to increase revenue.
      * Provide custom domain options for creators.