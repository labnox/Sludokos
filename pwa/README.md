# Remix Sludoko - Progressive Web App (PWA)

Dit is de complete, standalone **Progressive Web App (PWA)** versie van **Remix Sludoko**.

## ✨ PWA Kenmerken
- **100% Offline Functioneel:** Dankzij de Service Worker (`sw.js`) en Web App Manifest (`manifest.webmanifest`) werkt het spel zonder actieve internetverbinding.
- **Installeerbaar:** Kan op elk apparaat (Android, iOS Safari, Windows, macOS, Linux) direct op het startscherm of bureaublad worden geïnstalleerd met een eigen venster en app-icoon.
- **Puzzelgenerator & Solver:** Directe puzzelgeneratie op 4 niveaus (Makkelijk, Gemiddeld, Moeilijk, Expert) met unieke oplossingen.
- **Web Audio API & Web Speech API:** Ingebouwde geluidseffecten en Nederlandse spraakondersteuning voor toegankelijkheid.
- **Potloodnotities & Magie:** Ondersteunt handmatige notities en automatische kandidaat-invulling.
- **Statistieken & Opslag:** Automatische opslag van je actieve spel, winstpercentage en beste tijden via `localStorage`.

## 🚀 Hoe te implementeren (Web Hosting)
Je kunt deze map direct publiceren op elke statische hostingdienst:
1. **GitHub Pages:** Plaats de bestanden in de `gh-pages` branch of `/docs` map.
2. **Vercel / Netlify:** Koppel je repository of sleep deze map naar Netlify Drop.
3. **Firebase Hosting:** Voer `firebase deploy` uit.
