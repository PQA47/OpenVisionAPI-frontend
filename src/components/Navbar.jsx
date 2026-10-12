import { useState } from 'react'
import Button from './Button.jsx'

export default function Navbar({ onGetStarted }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = () => setMenuOpen(false)

  return (
    <header className="navbar">
      <a className="brand" href="#home" onClick={closeMenu} aria-label="OpenVisionAPI home">
        <span className="brand-icon" aria-hidden="true">◈</span>
        <span>OpenVision<span className="brand-accent">API</span></span>
      </a>

      <button
        className="menu-toggle"
        type="button"
        aria-label={menuOpen ? 'Close menu' : 'Open menu'}
        aria-expanded={menuOpen}
        onClick={() => setMenuOpen((open) => !open)}
      >
        {menuOpen ? '✕' : '☰'}
      </button>

      <nav className={`nav-links ${menuOpen ? 'nav-links--open' : ''}`} aria-label="Main navigation">
        <a href="#features" onClick={closeMenu}>Features</a>
        <a href="#how-it-works" onClick={closeMenu}>How it works</a>
        <a href="#about" onClick={closeMenu}>About</a>
        <Button onClick={() => { closeMenu(); onGetStarted?.() }}>
          Get Started <span aria-hidden="true">→</span>
        </Button>
      </nav>
    </header>
  )
}