import { useEffect, useState } from 'react'
import Layout from '../components/Layout'
import quizApi from '../api/quizApi'
import topicApi from '../api/topicApi'

const OPTION_LABELS = ['A', 'B', 'C', 'D']

export default function QuizPage() {
    const [step, setStep]         = useState('setup')   // setup | playing | result
    const [topics, setTopics]     = useState([])
    const [config, setConfig]     = useState({ topicId: '', questionCount: 10 })
    const [attempt, setAttempt]   = useState(null)
    const [qIndex, setQIndex]     = useState(0)
    const [selected, setSelected] = useState(null)
    const [answerRes, setAnswerRes] = useState(null)
    const [result, setResult]     = useState(null)
    const [loading, setLoading]   = useState(false)

    useEffect(() => {
        topicApi.getAll().then(r => setTopics(r.data.data || []))
    }, [])

    const handleStart = async () => {
        if (!config.topicId) return
        setLoading(true)
        try {
            const res = await quizApi.start({
                topicId: Number(config.topicId),
                questionCount: config.questionCount
            })
            setAttempt(res.data.data)
            setStep('playing')
            setQIndex(0)
        } catch (err) {
            alert(err.response?.data?.message || 'Cannot start quiz')
        } finally {
            setLoading(false)
        }
    }

    const handleAnswer = async (option) => {
        if (answerRes) return
        setSelected(option)
        const q = attempt.questions[qIndex]
        const res = await quizApi.answer(attempt.attemptId, {
            flashcardId: q.flashcardId,
            selectedAnswer: option,
            timeSpentSeconds: 0
        })
        setAnswerRes(res.data.data)
    }

    const handleNext = () => {
        setSelected(null)
        setAnswerRes(null)
        if (qIndex + 1 >= attempt.questions.length) {
            handleFinish()
        } else {
            setQIndex(i => i + 1)
        }
    }

    const handleFinish = async () => {
        const res = await quizApi.finish(attempt.attemptId)
        setResult(res.data.data)
        setStep('result')
    }

    const currentQ   = attempt?.questions?.[qIndex]
    const progressPct = attempt ? Math.round((qIndex / attempt.questions.length) * 100) : 0

    return (
        <Layout>
            {/* SETUP */}
            {step === 'setup' && (
                <div className="max-w-xl">
                    <h2 className="font-headline text-5xl font-extrabold tracking-tight text-on-surface mb-3">
                        Start a Quiz
                    </h2>
                    <p className="text-on-surface-variant text-lg mb-10">
                        Test your vocabulary knowledge across different topics.
                    </p>
                    <div className="bg-surface-container-lowest rounded-xl p-8 shadow-sm border border-outline-variant/10 space-y-6">
                        <div>
                            <label className="block text-sm font-bold font-label text-on-surface-variant mb-2 uppercase tracking-wider">
                                Select Topic
                            </label>
                            <select value={config.topicId}
                                    onChange={e => setConfig({ ...config, topicId: e.target.value })}
                                    className="w-full border border-outline-variant/20 rounded-DEFAULT px-4 py-3 bg-surface-container-low text-on-surface focus:ring-2 focus:ring-primary outline-none font-body">
                                <option value="">— Choose a topic —</option>
                                {topics.map(t => (
                                    <option key={t.id} value={t.id}>{t.name}</option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-bold font-label text-on-surface-variant mb-2 uppercase tracking-wider">
                                Number of Questions
                            </label>
                            <div className="flex gap-3">
                                {[5, 10, 20, 30].map(n => (
                                    <button key={n} onClick={() => setConfig({ ...config, questionCount: n })}
                                            className={`flex-1 py-3 rounded-DEFAULT font-bold text-sm transition-all
                      ${config.questionCount === n
                                                ? 'bg-primary text-on-primary shadow-lg shadow-primary/20'
                                                : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container'
                                            }`}>
                                        {n}
                                    </button>
                                ))}
                            </div>
                        </div>
                        <button onClick={handleStart} disabled={loading || !config.topicId}
                                className="w-full py-4 bg-gradient-to-r from-primary to-primary-container text-on-primary font-bold rounded-DEFAULT shadow-xl shadow-primary/30 hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50">
                            {loading ? 'Setting up...' : 'Begin Quiz →'}
                        </button>
                    </div>
                </div>
            )}

            {/* PLAYING */}
            {step === 'playing' && currentQ && (
                <div className="max-w-4xl mx-auto">
                    {/* Progress bar */}
                    <div className="flex items-center justify-between gap-6 mb-12">
                        <div className="flex-1">
                            <div className="flex justify-between items-end mb-2">
                <span className="text-sm font-bold text-primary font-headline tracking-wide uppercase">
                  Question {qIndex + 1} of {attempt.questions.length}
                </span>
                                <span className="text-sm font-medium text-outline">
                  {progressPct}% Completed
                </span>
                            </div>
                            <div className="h-2 w-full bg-surface-variant rounded-full overflow-hidden">
                                <div className="h-full bg-gradient-to-r from-primary to-primary-container rounded-full transition-all duration-700"
                                     style={{ width: `${progressPct}%` }} />
                            </div>
                        </div>
                        <div className="flex items-center gap-3 bg-primary-fixed px-5 py-3 rounded-lg flex-shrink-0">
              <span className="material-symbols-outlined text-primary"
                    style={{ fontVariationSettings: "'FILL' 1" }}>stars</span>
                            <div>
                <span className="text-[10px] uppercase font-bold tracking-widest text-on-primary-fixed-variant block">
                  Score
                </span>
                                <span className="text-sm font-bold tabular-nums text-on-primary-fixed">
                  {attempt.correctAnswers * 10} pts
                </span>
                            </div>
                        </div>
                    </div>

                    {/* Question card */}
                    <div className="bg-surface-container-lowest rounded-xl p-10 md:p-16 shadow-sm relative overflow-hidden mb-6">
                        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16 blur-3xl" />
                        <div className="relative z-10 flex flex-col items-center text-center">
              <span className="px-4 py-1.5 rounded-full bg-tertiary-fixed text-on-tertiary-fixed-variant text-[10px] font-bold uppercase tracking-widest mb-8">
                Vocabulary Focus
              </span>
                            <h1 className="text-3xl md:text-5xl font-extrabold text-on-surface font-headline leading-tight tracking-tight mb-4">
                                {currentQ.word}
                            </h1>
                            {currentQ.pronunciation && (
                                <p className="text-on-surface-variant italic text-xl">{currentQ.pronunciation}</p>
                            )}
                            <p className="text-outline text-lg font-medium mt-4">
                                Which definition is correct?
                            </p>
                        </div>
                    </div>

                    {/* Options */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-10">
                        {currentQ.options.map((option, i) => {
                            let borderClass = 'border-transparent hover:border-primary/20 hover:bg-blue-50'
                            if (answerRes) {
                                if (option === answerRes.correctAnswer)
                                    borderClass = 'border-emerald-400 bg-emerald-50'
                                else if (option === selected && !answerRes.isCorrect)
                                    borderClass = 'border-error bg-error-container'
                                else
                                    borderClass = 'border-transparent opacity-50'
                            }
                            return (
                                <button key={i} onClick={() => handleAnswer(option)} disabled={!!answerRes}
                                        className={`group flex items-center justify-between p-6 bg-surface-container-lowest rounded-lg border-2 ${borderClass} transition-all duration-300 text-left active:scale-[0.98] disabled:cursor-default`}>
                                    <div className="flex items-center gap-5">
                                        <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-colors
                      ${answerRes && option === answerRes.correctAnswer
                                            ? 'bg-emerald-500 text-white'
                                            : answerRes && option === selected && !answerRes.isCorrect
                                                ? 'bg-error text-white'
                                                : 'bg-surface-container-high group-hover:bg-primary-fixed text-on-surface-variant group-hover:text-primary'
                                        }`}>
                                            {OPTION_LABELS[i]}
                                        </div>
                                        <span className="text-base font-semibold text-on-surface">{option}</span>
                                    </div>
                                    {!answerRes && (
                                        <span className="material-symbols-outlined opacity-0 group-hover:opacity-100 text-primary transition-opacity">
                      chevron_right
                    </span>
                                    )}
                                    {answerRes && option === answerRes.correctAnswer && (
                                        <span className="material-symbols-outlined text-emerald-500">check_circle</span>
                                    )}
                                    {answerRes && option === selected && !answerRes.isCorrect && (
                                        <span className="material-symbols-outlined text-error">cancel</span>
                                    )}
                                </button>
                            )
                        })}
                    </div>

                    {/* Next button */}
                    {answerRes && (
                        <div className="flex items-center justify-between pt-6 border-t border-surface-variant">
                            <div className={`flex items-center gap-2 font-bold ${answerRes.isCorrect ? 'text-emerald-600' : 'text-error'}`}>
                <span className="material-symbols-outlined"
                      style={{ fontVariationSettings: "'FILL' 1" }}>
                  {answerRes.isCorrect ? 'check_circle' : 'cancel'}
                </span>
                                {answerRes.isCorrect ? 'Correct!' : 'Incorrect'}
                            </div>
                            <button onClick={handleNext}
                                    className="px-12 py-4 bg-primary text-on-primary rounded-DEFAULT font-bold shadow-xl shadow-primary/20 hover:bg-primary-container transition-all active:scale-95">
                                {qIndex + 1 >= attempt.questions.length ? 'View Results' : 'Next Question →'}
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* RESULT */}
            {step === 'result' && result && (
                <div className="max-w-xl mx-auto text-center">
                    <div className="bg-surface-container-lowest rounded-xl p-12 shadow-sm border border-outline-variant/10">
                        <div className={`w-24 h-24 rounded-xl flex items-center justify-center mx-auto mb-6 shadow-xl
              ${result.score >= 70
                            ? 'bg-gradient-to-br from-primary to-primary-container shadow-primary/30'
                            : 'bg-gradient-to-br from-tertiary to-tertiary-container shadow-tertiary/30'
                        }`}>
              <span className="material-symbols-outlined text-white text-5xl"
                    style={{ fontVariationSettings: "'FILL' 1" }}>
                {result.score >= 70 ? 'emoji_events' : 'school'}
              </span>
                        </div>

                        <p className="text-[10px] font-bold uppercase tracking-widest text-outline mb-2">
                            Quiz Complete
                        </p>
                        <h2 className="font-headline text-5xl font-extrabold text-on-surface mb-1">
                            {result.score}
                            <span className="text-2xl text-outline">/100</span>
                        </h2>
                        <p className="text-on-surface-variant mb-8">
                            {result.correctAnswers} of {result.totalQuestions} correct
                            {result.durationSeconds && ` • ${result.durationSeconds}s`}
                        </p>

                        {/* Score breakdown */}
                        <div className="w-full bg-surface-variant h-3 rounded-full overflow-hidden mb-8">
                            <div className={`h-full rounded-full transition-all duration-1000
                ${result.score >= 70
                                ? 'bg-gradient-to-r from-primary to-primary-container'
                                : 'bg-gradient-to-r from-tertiary to-tertiary-container'
                            }`}
                                 style={{ width: `${result.score}%` }} />
                        </div>

                        <div className="flex gap-3 justify-center">
                            <button onClick={() => quizApi.review(result.attemptId).then(() => {})}
                                    className="border-2 border-primary text-primary px-6 py-3 rounded-DEFAULT font-bold hover:bg-primary-fixed transition-all">
                                Review Errors
                            </button>
                            <button onClick={() => {
                                setStep('setup'); setAttempt(null); setResult(null)
                                setQIndex(0); setSelected(null); setAnswerRes(null)
                            }}
                                    className="bg-primary text-on-primary px-6 py-3 rounded-DEFAULT font-bold shadow-lg shadow-primary/20 hover:scale-105 transition-all">
                                Play Again
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </Layout>
    )
}