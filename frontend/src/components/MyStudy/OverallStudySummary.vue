// OverallStudySummary.vue
<script>
import { computed } from 'vue'; // defineProps is globally available in setup, no need to import it explicitly here in Vue 3 (though importing doesn't hurt)

export default {
    name: 'OverallStudySummary',
    // props option is defined here, OUTSIDE of setup()
    props: {
        overallPerfectCount: {
            type: Number,
            default: 0
        },
        overallVagueCount: {
            type: Number,
            default: 0
        },
        overallForgottenCount: {
            type: Number,
            default: 0
        },
        overallTotalCards: {
            type: Number,
            default: 0
        }
    },
    setup(props) { // props are passed as the first argument to setup()
        // Define Computed properties
        const overallPerfectPercentage = computed(() =>
            props.overallTotalCards > 0 ? ((props.overallPerfectCount / props.overallTotalCards) * 100).toFixed(1) : 0
        );
        const overallVaguePercentage = computed(() =>
            props.overallTotalCards > 0 ? ((props.overallVagueCount / props.overallTotalCards) * 100).toFixed(1) : 0
        );
        const overallForgottenPercentage = computed(() =>
            props.overallTotalCards > 0 ? ((props.overallForgottenCount / props.overallTotalCards) * 100).toFixed(1) : 0
        );

        // Return all reactive data and functions that need to be exposed to the template
        return {
            overallPerfectPercentage,
            overallVaguePercentage,
            overallForgottenPercentage,
            // You don't need to explicitly return individual props if you define them in the 'props' option
            // They are automatically exposed to the template.
            // If you still want to explicitly pass them for clarity or further manipulation, you can:
            // overallPerfectCount: props.overallPerfectCount,
            // overallVagueCount: props.overallVagueCount,
            // overallForgottenCount: props.overallForgottenCount,
            // overallTotalCards: props.overallTotalCards,
        };
    }
};
</script>

<template>
    <section class="overall-summary-section">
        <h2>전체 학습 현황</h2>
        <div class="summary-stats">
            <div class="stat-item perfect">
                <span class="label">완벽한 기억:</span>
                <span class="value">{{ overallPerfectCount }}개</span>
            </div>
            <div class="stat-item vague">
                <span class="label">희미한 기억:</span>
                <span class="value">{{ overallVagueCount }}개</span>
            </div>
            <div class="stat-item forgotten">
                <span class="label">사라진 기억:</span>
                <span class="value">{{ overallForgottenCount }}개</span>
            </div>
            <div class="stat-item total">
                <span class="label">총 학습 카드:</span>
                <span class="value">{{ overallTotalCards }}개</span>
            </div>
        </div>

        <div class="progress-bar-container">
            <div
                v-if="overallPerfectPercentage > 0"
                class="progress-bar perfect"
                :style="{ width: overallPerfectPercentage + '%' }"
                title="완벽한 기억"
            ></div>
            <div
                v-if="overallVaguePercentage > 0"
                class="progress-bar vague"
                :style="{ width: overallVaguePercentage + '%' }"
                title="희미한 기억"
            ></div>
            <div
                v-if="overallForgottenPercentage > 0"
                class="progress-bar forgotten"
                :style="{ width: overallForgottenPercentage + '%' }"
                title="사라진 기억"
            ></div>
            <div v-if="overallTotalCards === 0" class="progress-bar no-data">학습 데이터 없음</div>
        </div>
        <p class="progress-labels">
            <span v-if="overallPerfectPercentage > 0" class="label perfect">완벽 {{ overallPerfectPercentage }}%</span>
            <span v-if="overallVaguePercentage > 0" class="label vague">희미 {{ overallVaguePercentage }}%</span>
            <span v-if="overallForgottenPercentage > 0" class="label forgotten">사라짐 {{ overallForgottenPercentage }}%</span>
        </p>
    </section>
</template>

<style scoped>
/* (Your existing styles remain unchanged) */
.overall-summary-section {
    width: 100%;
    max-width: 800px;
    background-color: #ffffff;
    border-radius: 10px;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    padding: 30px;
    margin-bottom: 40px;
    border: 1px solid #e0d0ff; /* Subtle border */
}

.overall-summary-section h2 {
    color: #5a2e87;
    font-size: 1.8rem;
    font-weight: 600;
    margin-bottom: 25px;
    padding-bottom: 10px;
}

.summary-stats {
    display: flex;
    justify-content: space-around;
    flex-wrap: wrap;
    margin-bottom: 25px;
    gap: 15px;
}

.stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px 15px;
    border-radius: 10px;
    background-color: #f9f9f9;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    min-width: 120px;
    text-align: center;
}

.stat-item .label {
    font-size: 0.95rem;
    color: #777;
    margin-bottom: 5px;
    font-weight: 500;
}

.stat-item .value {
    font-size: 1.5rem;
    font-weight: bold;
}

.stat-item.perfect .value { color: #28a745; }
.stat-item.vague .value { color: #ffc107; }
.stat-item.forgotten .value { color: #dc3545; }
.stat-item.total .value { color: #5a2e87; }


.progress-bar-container {
    width: 100%;
    height: 30px; /* Height of the bar */
    background-color: #e9ecef; /* Background for the empty part */
    border-radius: 15px;
    overflow: hidden;
    display: flex;
    margin-bottom: 15px;
    box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.1);
}

.progress-bar {
    height: 100%;
    transition: width 0.5s ease-in-out;
    flex-shrink: 0; /* Prevent shrinking */
}

.progress-bar.perfect { background-color: #28a745; }
.progress-bar.vague { background-color: #ffc107; }
.progress-bar.forgotten { background-color: #dc3545; }
.progress-bar.no-data {
    background-color: #a471ff;
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    color: white;
    font-weight: bold;
}

.progress-labels {
    display: flex;
    justify-content: space-around;
    font-size: 0.9rem;
    color: #555;
    margin-top: 10px;
}
.progress-labels .label {
    font-weight: 600;
}
.progress-labels .label.perfect { color: #28a745; }
.progress-labels .label.vague { color: #ffc107; }
.progress-labels .label.forgotten { color: #dc3545; }
</style>