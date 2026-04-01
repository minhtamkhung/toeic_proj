import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import topicApi from '../api/topicApi'

const TOPIC_ICONS = {
    'Business':    'business',
    'Grammar':     'edit_note',
    'Travel':      'flight',
    'Technology':  'computer',
    'Phrasal':     'format_quote',
    'TOEIC':       'workspace_premium',
    'default':     'folder',
}

function getIcon(name = '') {
    const key = Object.keys(TOPIC_ICONS).find(k =>
        name.toLowerCase().includes(k.toLowerCase()))
    return TOPIC_ICONS[key] || TOPIC_ICONS.default
}

export default function TopicsPage() {
    const navigate          = useNavigate()
    const [topics, setTopics]     = useState([])
    const [filter, setFilter]     = useState('all') // all | system | personal
    const [loading, setLoading]   = useState(true)

    useEffect(() => {
        topicApi.getAll()
            .then(r => setTopics(r.data.data || []))
            .finally(() => setLoading(false))
    }, [])

    const systemTopics   = topics.filter(t => t.isSystem)
    const personalTopics = topics.filter(t => !t.isSystem)

    const displayed = filter === 'system'   ? systemTopics
        : filter === 'personal' ? personalTopics
            : topics

    return (
        <Layout>
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-12">
                <div className="max-w-xl">
                    <h2 className="font-headline text-5xl font-extrabold tracking-tight text-on-surface mb-4">
                        Study Topics
                    </h2>
                    <p className="text-on-surface-variant text-lg leading-relaxed">
                        Organize your learning journey with curated vocabulary sets designed for TOEIC mastery.
                    </p>
                </div>
                <button
                    onClick={() => navigate('/topics/new')}
                    className="flex items-center gap-2 bg-primary text-on-primary px-8 py-4 rounded-DEFAULT font-bold shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all ease-out-expo whitespace-nowrap">
                    <span className="material-symbols-outlined">add_circle</span>
                    Create new topic
                </button>
            </div>

            {/* Filter pills */}
            <div className="flex flex-wrap items-center gap-3 mb-8">
                {[
                    { key: 'all',      label: `All Topics (${topics.length})` },
                    { key: 'personal', label: `My topics`, count: personalTopics.length, countColor: 'bg-secondary text-white' },
                    { key: 'system',   label: `System topics`, count: systemTopics.length, countColor: 'bg-primary text-white' },
                ].map(({ key, label, count, countColor }) => (
                    <button key={key} onClick={() => setFilter(key)}
                            className={`px-6 py-2 rounded-full text-sm font-semibold transition-all flex items-center gap-2
              ${filter === key
                                ? key === 'all'
                                    ? 'bg-on-surface text-surface-container-lowest'
                                    : key === 'personal'
                                        ? 'bg-secondary-container text-on-secondary-container'
                                        : 'bg-primary-fixed text-on-primary-fixed'
                                : 'border border-outline-variant/30 hover:bg-surface-container'
                            }`}>
                        {label}
                        {count !== undefined && (
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${countColor}`}>
                {count}
              </span>
                        )}
                    </button>
                ))}
            </div>

            {/* Grid */}
            {loading ? (
                <div className="flex justify-center mt-20">
                    <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {displayed.map(topic => (
                        <TopicCard
                            key={topic.id}
                            topic={topic}
                            onClick={() => navigate(`/flashcards/${topic.id}`)}
                        />
                    ))}
                    {displayed.length === 0 && (
                        <div className="col-span-3 text-center py-16 text-on-surface-variant">
                            <span className="material-symbols-outlined text-5xl mb-3 block opacity-30">folder_open</span>
                            <p>No topics found</p>
                        </div>
                    )}
                </div>
            )}
        </Layout>
    )
}

function TopicCard({ topic, onClick }) {
    const icon = getIcon(topic.name)
    return (
        <button onClick={onClick}
                className="group relative bg-surface-container-lowest rounded-xl p-8 text-left hover:shadow-2xl hover:shadow-primary/10 transition-all duration-500 ease-out-expo border border-outline-variant/10 w-full">
            <div className="flex justify-between items-start mb-6">
                <div className={`w-14 h-14 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform duration-500
          ${topic.isSystem ? 'bg-primary-fixed text-primary' : 'bg-secondary-container text-secondary'}`}>
          <span className="material-symbols-outlined text-3xl"
                style={{ fontVariationSettings: "'FILL' 1" }}>
            {icon}
          </span>
                </div>
                <span className="text-[10px] font-bold tracking-widest uppercase py-1 px-3 bg-surface-container-high rounded-full text-on-surface-variant">
          {topic.isSystem ? 'System' : 'Personal'}
        </span>
            </div>

            <h3 className="font-headline text-xl font-bold text-on-surface mb-2 group-hover:text-primary transition-colors">
                {topic.name}
            </h3>
            {topic.description && (
                <p className="text-sm text-on-surface-variant line-clamp-2 leading-relaxed">
                    {topic.description}
                </p>
            )}

            <div className="flex items-center justify-between mt-6 pt-4 border-t border-outline-variant/10">
        <span className="text-xs font-bold text-outline uppercase tracking-wider">
          Open topic
        </span>
                <span className="material-symbols-outlined text-primary opacity-0 group-hover:opacity-100 transition-opacity transform -translate-x-2 group-hover:translate-x-0 ease-out-expo duration-300">
          arrow_forward
        </span>
            </div>
        </button>
    )
}