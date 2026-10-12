export default function FeatureCard({ icon, title, description, number }) {
  return (
    <article className="feature-card">
      <div className="feature-card__top">
        <span className="feature-card__icon" aria-hidden="true">{icon}</span>
        <span className="feature-card__number">{number}</span>
      </div>
      <h3>{title}</h3>
      <p>{description}</p>
      <span className="feature-card__arrow" aria-hidden="true">↗</span>
    </article>
  )
}