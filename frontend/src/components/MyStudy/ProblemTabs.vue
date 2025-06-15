<script>
export default {
    name: 'ProblemTabs',
    props: {
        activeTab: {
            type: String,
            required: true
        },
        ongoingCount: {
            type: Number,
            default: 0
        },
        completedCount: {
            type: Number,
            default: 0
        }
    },
    setup(props, { emit }) {
        const changeTab = (tab) => {
            emit('change-tab', tab);
        };

        return {
            changeTab
        };
    }
};
</script>

<template>
    <div class="problem-tabs">
        <button
            :class="{ active: activeTab === 'ongoing' }"
            @click="changeTab('ongoing')"
        >
            현재 진행 중인 학습 ({{ ongoingCount }}개)
        </button>
        <button
            :class="{ active: activeTab === 'completed' }"
            @click="changeTab('completed')"
        >
            완료한 학습 ({{ completedCount }}개)
        </button>
    </div>
</template>

<style scoped>
.problem-tabs {
    display: flex;
    justify-content: center;
    margin-bottom: 20px;
    gap: 10px;
    /* This component is only shown on small screens, so no need to hide by default */
}

.problem-tabs button {
    padding: 10px 20px;
    border: 1px solid #a471ff;
    border-radius: 8px;
    background-color: #f0e6ff;
    color: #5a2e87;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.problem-tabs button:hover {
    background-color: #e6d6ff;
}

.problem-tabs button.active {
    background-color: #a471ff;
    color: white;
    border-color: #a471ff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* This component is specifically for mobile/tablet view (max-width: 768px) */
@media (min-width: 769px) {
    .problem-tabs {
        display: none; /* Hide on larger screens */
    }
}
</style>