# SmartWaste — AI-Powered Waste Management System
> Smart India Hackathon Project

## Tech Stack
| Layer | Technology |
|---|---|
| Backend | Java 17 + Spring Boot 3.2 |
| Auth | JWT (JJWT 0.12) |
| Database | PostgreSQL via Supabase |
| ORM | JPA / Hibernate |
| Frontend | React 18 + TypeScript + Vite |
| Styling | Tailwind CSS |
| State | Zustand + React Query |
| Maps | Leaflet.js |
| Charts | Recharts |

## Project Structure
```
SmartWasteManagement/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/smartwaste/
│   │   ├── controller/         # REST controllers
│   │   ├── service/            # Business logic
│   │   ├── repository/         # JPA repositories
│   │   ├── entity/             # JPA entities
│   │   ├── dto/                # Request/Response DTOs
│   │   ├── security/           # JWT filter + service
│   │   ├── config/             # Security + App config
│   │   └── exception/          # Global error handling
│   └── src/main/resources/
│       └── application.yml
├── frontend/                   # React application
│   └── src/
│       ├── api/                # Axios API clients
│       ├── components/         # Shared components
│       ├── pages/              # Route pages
│       ├── store/              # Zustand state
│       └── types/              # TypeScript types
└── database/
    └── schema.sql              # PostgreSQL schema
```

## User Roles
| Role | Access |
|---|---|
| ADMIN | Full system access, analytics, worker management |
| CITIZEN | File complaints, earn reward points |
| WORKER | View assigned tasks, update route status |
| RECYCLER | Waste marketplace |

## Quick Start

### 1. Database Setup (Supabase)
1. Create a new Supabase project
2. Run `database/schema.sql` in the SQL editor
3. Copy the connection string

### 2. Backend
```bash
cd backend
cp .env.example .env
# Fill in DATABASE_URL, DATABASE_PASSWORD, JWT_SECRET
mvn spring-boot:run
# API available at http://localhost:8080/api
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
# App available at http://localhost:3000
```

## API Endpoints

### Auth (public)
| Method | Endpoint | Description |
|---|---|---|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login + get JWT |
| POST | /api/auth/refresh | Refresh access token |

### Complaints
| Method | Endpoint | Role |
|---|---|---|
| POST | /api/complaints | CITIZEN |
| GET | /api/complaints | ADMIN |
| GET | /api/complaints/my | CITIZEN |
| GET | /api/complaints/worker | WORKER |
| PATCH | /api/complaints/{id}/assign/{workerId} | ADMIN |
| PATCH | /api/complaints/{id}/status | ADMIN, WORKER |

### Analytics
| Method | Endpoint | Role |
|---|---|---|
| GET | /api/analytics/dashboard | ADMIN |

## Default Admin Credentials
```
Email:    admin@smartwaste.com
Password: Admin@123
```
> Change this immediately after first login.
