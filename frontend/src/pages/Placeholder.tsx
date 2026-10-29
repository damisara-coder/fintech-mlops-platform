export default function Placeholder({ title }: { title: string }) {
  return (
    <div className="rounded-xl border border-line bg-panel p-6">
      <h1 className="text-xl font-bold">{title}</h1>
      <p className="mt-1 text-mute">Cet écran sera construit bientôt.</p>
    </div>
  )
}