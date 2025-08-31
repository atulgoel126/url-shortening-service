
### **Phase 1: Foundation & Authentication with Supabase**

**Goal:** Set up your project environments and implement the complete user sign-up/login flow. At the end of this phase, a user can create an account and your Java backend can verify their identity.

1.  **Set Up Supabase Project (1-2 hours)**

      * Go to [supabase.com](https://supabase.com), create a new project.
      * Familiarize yourself with the dashboard. Note your Project URL and `anon` public key. You will need these for the frontend.
      * Navigate to the "Table Editor".

2.  **Create Database Schema in Supabase (1-2 hours)**

      * The `users` table is handled by Supabase Auth automatically. **Do not create a `users` table.**
      * Create the `links` and `link_views` tables using the SQL editor in Supabase, with one crucial change: the `user_id` in the `links` table should reference Supabase's auth users.

    <!-- end list -->

    ```sql
    -- The user_id now references the built-in auth.users table
    -- Supabase user IDs are of type UUID
    CREATE TABLE links (
        id BIGSERIAL PRIMARY KEY,
        user_id UUID REFERENCES auth.users(id), -- Can be NULL for anonymous links
        short_code VARCHAR(10) UNIQUE NOT NULL,
        long_url TEXT NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE link_views (
        id BIGSERIAL PRIMARY KEY,
        link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
        viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        ip_address VARCHAR(45) NOT NULL,
        user_agent TEXT
    );
    ```

3.  **Initialize Java Backend & Connect to Supabase DB (2-3 hours)**

      * Use `start.spring.io` to generate a new Spring Boot project.
      * **Dependencies:** `Spring Web`, `Spring Data JPA`, `PostgreSQL Driver`, `Lombok`.
      * In your `application.properties`, configure the connection to your Supabase PostgreSQL database. You can find the connection details in Supabase under `Project Settings` \> `Database`.
      * Create the JPA Entity classes (`Link.java`, `LinkView.java`) that map to your tables.

4.  **Implement the Authentication Flow (4-6 hours)**

      * **Frontend (Thymeleaf & JavaScript):**
          * Create simple HTML pages for Login and Sign Up.
          * Include the `supabase-js` library via a CDN.
          * Write JavaScript that uses `supabase.auth.signUp()` and `supabase.auth.signInWithPassword()`.
          * On successful login/signup, Supabase returns a JWT (JSON Web Token). **Store this token in the browser's `localStorage`**.
      * **Backend (Java Security):**
          * Your Java app will be a **stateless API**. It will **not** manage sessions. It will simply validate the JWT sent from the frontend.
          * Add a JWT validation library (e.g., `com.auth0:java-jwt`).
          * Create a Security Filter in Spring Boot. For any protected endpoint (like `/api/dashboard`), this filter will:
            1.  Extract the JWT from the `Authorization: Bearer <token>` header.
            2.  Validate the token's signature using Supabase's public JWT signing secret (found in your project's JWT settings).
            3.  If valid, extract the `user_id` (the `sub` claim) from the token and allow the request to proceed.
            4.  If invalid, return a `401 Unauthorized` error.

-----

### **Phase 2: The Core Product Loop (Anonymous)**

**Goal:** Get the primary function—shortening and redirecting—working perfectly, even for users who are not logged in.

1.  **Implement URL Shortening Logic (3-4 hours)**

      * Create the `POST /api/url` endpoint.
      * Implement the Base62 encoding logic to generate a `short_code` from the database `id`.
      * When a long URL is received, save it to the `links` table in Supabase (with `user_id` as `NULL` for now).
      * Return the full short URL (e.g., `https://yourdomain.com/g8xT1`).
      * Build the simple public homepage UI for this.

2.  **Implement the Redirection Service (3-4 hours)**

      * Create the `GET /{shortCode}` endpoint. This is the most performance-critical part.
      * Implement caching (using an in-memory solution like Caffeine).
      * **Logic:**
        1.  Check the cache for the `shortCode`.
        2.  If cache miss, query the Supabase DB for the `long_url`.
        3.  Put the result in the cache with a reasonable TTL (Time To Live, e.g., 24 hours).
        4.  Perform a `302 Found` redirect to the interstitial page: `/ad-page?id={shortCode}`.

-----

### **Phase 3: Monetization & User Journey Completion**

**Goal:** Integrate the ad page and view tracking to complete the end-to-end flow.

1.  **Build the Interstitial Page (2-3 hours)**

      * Create the `GET /ad-page` endpoint and its corresponding Thymeleaf template.
      * This endpoint fetches the `long_url` from the database to pass to the template.
      * Add placeholders for the Google AdSense ad unit.
      * Write the frontend JavaScript for the 5-second countdown and enabling the "Skip Ad" button, which then redirects to the `long_url`.

2.  **Implement View Tracking (2-3 hours)**

      * Modify the `GET /{shortCode}` redirection logic.
      * Before redirecting, asynchronously log the view. Use Spring Boot's `@Async` annotation to do this in a separate thread so it doesn't delay the user's redirect.
      * The async method will save the `link_id`, `ip_address`, and `user_agent` to the `link_views` table.

3.  **Apply for Google AdSense (Business Task)**

      * **Do this in parallel with development.** The approval process can take days or even weeks. You need a live, functional site with some content (like a privacy policy, terms of service, and contact page) before you apply.

-----

### **Phase 4: The Creator's Dashboard**

**Goal:** Deliver the core value to your registered users.

1.  **Link URLs to Users (1 hour)**

      * Modify the `POST /api/url` endpoint. If the request is authenticated (i.e., has a valid JWT), extract the `user_id` and save it with the link.

2.  **Create the Dashboard API (3-4 hours)**

      * Create a protected `GET /api/dashboard` endpoint.
      * This endpoint will query the `links` table for the authenticated `user_id`.
      * It will then perform a second query or a JOIN to count the views for each link from the `link_views` table.
      * Calculate estimated earnings and return all this data as JSON.

3.  **Build the Dashboard UI (3-4 hours)**

      * Create the protected `/dashboard` page.
      * Use JavaScript to fetch data from your `/api/dashboard` endpoint on page load.
      * Dynamically populate the summary cards and the table with the user's links and stats.

-----

### **Phase 5: Deployment & Go-Live**

**Goal:** Get your POC running on a public server.

1.  **Package & Deploy (2-4 hours)**

      * Package your Spring Boot app as an executable JAR.
      * Provision a small cloud server (e.g., DigitalOcean, Linode, AWS Lightsail).
      * Deploy your JAR and run it as a service (`systemd`).

2.  **Domain & HTTPS (1-2 hours)**

      * Point your domain name to the server.
      * Install a reverse proxy like Nginx.
      * Use Certbot to configure a free SSL certificate from Let's Encrypt. This is crucial for user trust and for Google AdSense.