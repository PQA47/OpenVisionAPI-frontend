import { useEffect, useRef, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import Button from '../components/Button.jsx'
import FeatureCard from '../components/FeatureCard.jsx'

const features = [
  {
    number: '01',
    icon: '⌕',
    title: 'Image Recognition',
    description: 'Identify objects and understand the visual content of an image.',
  },
  {
    number: '02',
    icon: '✳',
    title: 'AI-Powered Analysis',
    description: 'Turn image data into meaningful information through computer vision.',
  },
  {
    number: '03',
    icon: '⌘',
    title: 'Developer Friendly',
    description: 'A simple interface designed to make vision tools easier to explore.',
  },
]

export default function LandingPage() {
  const demoRef = useRef(null)
  const [selectedFile, setSelectedFile] = useState(null)
  const [preview, setPreview] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    return () => {
      if (preview) URL.revokeObjectURL(preview)
    }
  }, [preview])

  const scrollToDemo = () => {
    demoRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  const handleFileChange = (event) => {
    const file = event.target.files?.[0]
    if (!file) return

    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      setMessage('Please select a PNG, JPG, or WEBP image.')
      event.target.value = ''
      return
    }

    if (file.size > 10 * 1024 * 1024) {
      setMessage('Please choose an image smaller than 10 MB.')
      event.target.value = ''
      return
    }

    setSelectedFile(file)
    setPreview(URL.createObjectURL(file))
    setMessage('Image ready to preview. AI analysis is not connected yet.')
  }

  const resetDemo = () => {
    setSelectedFile(null)
    setPreview('')
    setMessage('')
  }

  return (
    <div className="app-shell">
      <Navbar onGetStarted={scrollToDemo} />

      <main>
        <section id="home" className="hero section-container">
          <div className="hero-content">
            <div className="eyebrow"><span className="status-dot" /> COMPUTER VISION, SIMPLIFIED</div>
            <h1>Give your images<br /><span className="gradient-text">a new perspective.</span></h1>
            <p className="hero-description">
              Explore the power of computer vision with OpenVisionAPI.
              Turn visual information into insights through an intuitive,
              developer-friendly experience.
            </p>
            <div className="hero-actions">
              <Button onClick={scrollToDemo}>Explore the demo <span aria-hidden="true">→</span></Button>
              <a className="text-link" href="#features">Discover features <span aria-hidden="true">↓</span></a>
            </div>
            <div className="hero-note"><span>✦</span> Built for curious minds and AI developers</div>
          </div>

          <div className="hero-visual" aria-label="Abstract computer vision illustration">
            <div className="visual-grid" />
            <div className="orb orb--outer" />
            <div className="orb orb--middle" />
            <div className="orb orb--inner"><span>◈</span></div>
            <div className="floating-label floating-label--top"><span className="label-dot" /> Vision intelligence</div>
            <div className="floating-label floating-label--bottom"><span className="label-symbol">✳</span> See beyond pixels</div>
          </div>
        </section>

        <section id="features" className="features section-container">
          <div className="section-heading">
            <span className="section-kicker">WHAT YOU CAN EXPLORE</span>
            <h2>Vision meets <span className="gradient-text">intelligence.</span></h2>
            <p>Discover the building blocks of a smarter visual experience.</p>
          </div>
          <div className="feature-grid">
            {features.map((feature) => <FeatureCard key={feature.number} {...feature} />)}
          </div>
        </section>

        <section id="how-it-works" className="workflow section-container">
          <div>
            <span className="section-kicker">A SIMPLE WORKFLOW</span>
            <h2>From image to insight.</h2>
            <p>Select an image, connect it to your vision endpoint, and present the results in a clear interface.</p>
          </div>
          <div className="workflow-steps">
            <div className="workflow-step"><span>01</span><div><h3>Choose an image</h3><p>Start with an image from your device.</p></div></div>
            <div className="workflow-step"><span>02</span><div><h3>Analyze the content</h3><p>Send the image to the configured vision API.</p></div></div>
            <div className="workflow-step"><span>03</span><div><h3>Explore the results</h3><p>Display the analysis in a reusable results component.</p></div></div>
          </div>
        </section>

        <section id="demo" className="demo-section section-container" ref={demoRef}>
          <div className="demo-panel">
            <span className="section-kicker">YOUR VISUAL WORKSPACE</span>
            <h2>Start with an image.</h2>
            <p>Choose an image to preview the upload experience. Live AI analysis will be added when the backend endpoint is connected.</p>

            <label className="upload-area">
              {preview ? (
                <img className="upload-preview" src={preview} alt="Selected image preview" />
              ) : (
                <>
                  <span className="upload-icon" aria-hidden="true">↑</span>
                  <strong>Choose an image to preview</strong>
                  <span>PNG, JPG, WEBP · Maximum 10 MB</span>
                </>
              )}
              <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleFileChange} />
            </label>

            {selectedFile && <p className="selected-file">Selected: {selectedFile.name}</p>}
            {message && <p className="demo-message" role="status">{message}</p>}
            <div className="demo-actions">
              <Button variant="secondary" onClick={resetDemo}>Reset demo</Button>
            </div>
          </div>
        </section>

        <section id="about" className="closing section-container">
          <span className="section-kicker">OPENVISIONAPI</span>
          <h2>Make visual AI more approachable.</h2>
          <p>A clean starting point for exploring computer vision and building useful AI-powered applications.</p>
          <Button onClick={scrollToDemo}>Try the image preview <span aria-hidden="true">→</span></Button>
        </section>
      </main>

      <footer className="footer">
        <a className="brand" href="#home"><span className="brand-icon" aria-hidden="true">◈</span><span>OpenVision<span className="brand-accent">API</span></span></a>
        <p>Built to make computer vision easier to explore.</p>
        <a href="#home">Back to top ↑</a>
      </footer>
    </div>
  )
}