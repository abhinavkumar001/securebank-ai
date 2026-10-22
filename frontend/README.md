# SecureBank – React frontend

Customer banking and an admin operations desk on top of the Spring Boot API.

## Run
1. Start the backend first (port 8080).
2. In this folder:
   ```
   npm install
   npm run dev
   ```
3. Open http://localhost:5173

Sign in as `admin@securebank.com` / `Admin@123` for the operations desk, or create a customer profile.

To point at another backend URL, create `.env` with `VITE_API_URL=http://your-host:8080`.
