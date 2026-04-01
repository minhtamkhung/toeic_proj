import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import flashcardApi from '../api/flashcardApi'
import progressApi from '../api/progressApi'

export default function FlashcardPage() {
    const { topicId }             = useParams()
    const navigate                = useNavigate()
    const [cards, setCards]       = useState([])
    const [index, setIndex]       = useState(0)
    const [flipped, setFlipped]   = useState(false)
    const [loading, setLoading]   = useState(true)
    const [feedback, setFeedback] = useState('')

    useEffect(() => {
        flashcardApi.getByTopic(topicId)
            .then(r => setCards(r.data.data?.content || []))
            .finally(() => setLoading(false))
    }, [topicId])

    const handleQuality = async (flashcardId, quality) => {
        try {
            await progressApi.review({ flashcardId, quality })
            setFeedback(quality >= 3 ? '✓ Got it' : '↺ Review again')
            setTimeout(() => {
                setFeedback('')
                setFlipped(false)
                setIndex(i => i + 1)
            }, 700)
        } catch { setFeedback('Error') }
    }

    const card = cards[index]

    return (
        <Layout>
            <div className="max-w-3xl mx-auto">
                <button onClick={() => navigate('/topics')}
                        className="flex items-center gap-2 text-primary font-bold text-sm mb-8 hover:underline">
                    <span className="material-symbols-outlined text-sm">arrow_back</span>
                    Back to Topics
                </button>

                {loading ? (
                    <div className="flex justify-center mt-20">
                        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                    </div>
                ) : cards.length === 0 ? (
                    <div className="text-center mt-20">
                        <span className="material-symbols-outlined text-5xl text-outline block mb-3">style</span>
                        <p className="text-on-surface-variant">No flashcards in this topic yet.</p>
                    </div>
                ) : index >= cards.length ? (
                    <div className="text-center mt-20">
                        <div className="w-20 h-20 bg-gradient-to-br from-primary to-primary-container rounded-xl flex items-center justify-center mx-auto mb-6 shadow-xl shadow-primary/30">
              <span className="material-symbols-outlined text-white text-4xl"
                    style={{ fontVariationSettings: "'FILL' 1" }}>done_all</span>
                        </div>
                        <h2 className="font-headline text-3xl font-bold mb-2">All done!</h2>
                        <p className="text-on-surface-variant mb-6">You've reviewed all {cards.length} cards.</p>
                        <button onClick={() => { setIndex(0); setFlipped(false) }}
                                className="bg-primary text-on-primary px-8 py-3 rounded-DEFAULT font-bold shadow-lg hover:scale-105 transition-all">
                            Start Over
                        </button>
                    </div>
                ) : (
                    <>
                        {/* Progress */}
                        <div className="flex justify-between items-center mb-6">
                            <p className="text-sm font-bold text-outline uppercase tracking-wider">
                                {index + 1} / {cards.length} cards
                            </p>
                            {feedback && (
                                <span className="text-sm font-bold text-primary bg-primary-fixed px-3 py-1 rounded-full">
                  {feedback}
                </span>
                            )}
                        </div>
                        <div className="w-full h-1.5 bg-surface-variant rounded-full overflow-hidden mb-10">
                            <div className="h-full bg-gradient-to-r from-primary to-primary-container rounded-full transition-all duration-500"
                                 style={{ width: `${(index / cards.length) * 100}%` }} />
                        </div>

                        {/* Card */}
                        <div className="relative flip-card" style={{ height: '340px' }}>
                            <div className={`flip-inner w-full h-full ${flipped ? 'flipped' : ''}`}>

                                {/* Front */}
                                <div className="flip-front bg-surface-container-lowest rounded-xl shadow-[0_40px_80px_-20px_rgba(0,88,190,0.08)] flex flex-col items-center justify-center text-center p-12 cursor-pointer border border-outline-variant/10 hover:scale-[1.01] transition-transform"
                                     onClick={() => setFlipped(true)}>
                                    <span className="text-xs font-bold text-primary tracking-widest uppercase mb-6 block">Term</span>
                                    <h2 className="font-headline text-5xl font-extrabold text-on-surface mb-3">{card.word}</h2>
                                    {card.pronunciation && (
                                        <p className="italic text-on-surface-variant text-xl">{card.pronunciation}</p>
                                    )}
                                    <p className="text-outline text-sm mt-8">Tap to see definition</p>
                                </div>

                                {/* Back */}
                                <div className="flip-back bg-surface-container-lowest rounded-xl shadow-[0_40px_80px_-20px_rgba(0,88,190,0.08)] flex flex-col items-center justify-center text-center p-12 border border-outline-variant/10">
                                    <span className="text-xs font-bold text-tertiary tracking-widest uppercase mb-4 block">Definition</span>
                                    <p className="text-xl text-on-surface-variant leading-relaxed max-w-md">{card.definition}</p>
                                    {card.exampleSentence && (
                                        <div className="mt-6 bg-surface-container-low rounded-lg p-4 text-left max-w-md w-full">
                                            <p className="italic text-on-surface text-sm">"{card.exampleSentence}"</p>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="absolute -bottom-3 left-4 right-4 h-3 bg-white/40 rounded-b-xl -z-10 blur-[1px]" />
                            <div className="absolute -bottom-6 left-8 right-8 h-3 bg-white/20 rounded-b-xl -z-20 blur-[2px]" />
                        </div>

                        {/* Quality buttons */}
                        {flipped && (
                            <div className="mt-12 grid grid-cols-4 gap-3">
                                {[
                                    { q: 0, label: 'Again',  icon: 'replay',                    color: 'hover:bg-error-container text-error' },
                                    { q: 2, label: 'Hard',   icon: 'sentiment_dissatisfied',    color: 'hover:bg-tertiary-fixed text-tertiary' },
                                    { q: 4, label: 'Good',   icon: 'sentiment_satisfied',       color: 'hover:bg-secondary-container text-secondary' },
                                    { q: 5, label: 'Easy',   icon: 'sentiment_very_satisfied',  color: 'hover:bg-primary-fixed text-primary' },
                                ].map(({ q, label, icon, color }) => (
                                    <button key={q} onClick={() => handleQuality(card.id, q)}
                                            className={`flex flex-col items-center gap-2 p-4 rounded-lg bg-surface-container-high ${color} transition-all duration-300 active:scale-95`}>
                                        <span className="material-symbols-outlined text-xl">{icon}</span>
                                        <span className="text-sm font-bold font-headline">{label}</span>
                                    </button>
                                ))}
                            </div>
                        )}
                    </>
                )}
            </div>
        </Layout>
    )
}