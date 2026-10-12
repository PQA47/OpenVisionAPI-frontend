# OpenVisionAPI Frontend

A React + Vite landing page starter with reusable UI components and a dark navy/purple/blue theme.

## Requirements
- Node.js (LTS recommended)
- npm

## Run locally
```powershell
npm install
npm run dev
```

If PowerShell blocks `npm.ps1`, use `npm.cmd install` and `npm.cmd run dev`.

## Production build
```powershell
npm run build
```

## Structure
- `src/components/Button.jsx` — reusable button
- `src/components/Navbar.jsx` — responsive navigation
- `src/components/FeatureCard.jsx` — reusable feature card
- `src/pages/LandingPage.jsx` — landing page sections and image preview
- `src/index.css` — global styles and responsive theme

The image upload currently provides a local preview only. It does not call an AI endpoint. Connect it to the team's backend once the endpoint and request/response contract are known.
