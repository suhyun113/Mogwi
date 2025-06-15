<script>
import { computed } from 'vue';

export default {
    name: 'OverallStudySummary',
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
        },
        isLoggedIn: { // 새로 추가된 prop
            type: Boolean,
            default: false
        }
    },
    setup(props) {
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
            // props.isLoggedIn을 사용할 것이므로, setup에서 props를 직접 참조하는 것으로 충분합니다.
        };
    }
};
</script>

<template>
    <section class="overall-summary-section">
        <h2>전체 학습 현황</h2>
        <div v-if="isLoggedIn && overallTotalCards > 0" class="summary-stats">
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
        <div v-else-if="isLoggedIn && overallTotalCards === 0" class="no-study-data-message">
            <font-awesome-icon :icon="['fas', 'book-open']" class="no-data-icon" />
            <p>아직 학습한 문제가 없어요. 새로운 문제를 시작해보세요!</p>
        </div>
        <div v-else class="no-study-data-message">
            <font-awesome-icon :icon="['fas', 'user-lock']" class="no-data-icon" />
            <p>로그인하시면 학습 현황을 확인할 수 있습니다.</p>
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
            <div v-if="overallTotalCards === 0 || !isLoggedIn" class="progress-bar no-data">
                {{ isLoggedIn ? '학습 데이터 없음' : '로그인 필요' }}
            </div>
        </div>
        <p class="progress-labels">
            <span v-if="overallPerfectPercentage > 0" class="label perfect">완벽 {{ overallPerfectPercentage }}%</span>
            <span v-if="overallVaguePercentage > 0" class="label vague">희미 {{ overallVaguePercentage }}%</span>
            <span v-if="overallForgottenPercentage > 0" class="label forgotten">사라짐 {{ overallForgottenPercentage }}%</span>
            <span v-else-if="overallTotalCards === 0 && isLoggedIn" class="label no-data-label">새로운 학습을 시작해보세요!</span>
            <span v-else-if="!isLoggedIn" class="label no-data-label">로그인 후 확인 가능합니다</span>
        </p>
    </section>
</template>

<style scoped>
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
    font-size: 0.95rem;
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

.no-study-data-message {
    text-align: center;
    padding: 30px 20px;
    font-size: 1.1rem;
    color: #777;
    background-color: #f8f8f8;
    border-radius: 8px;
    margin-bottom: 25px;
    border: 1px solid #eee;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;
}

.no-study-data-message p {
    margin: 0;
    line-height: 1.5;
    color: #666;
}

.no-data-icon {
    font-size: 3rem;
    color: #ccc; /* 기본 회색 */
}

/* 로그인 필요 아이콘 색상 */
.no-study-data-message .fa-user-lock {
    color: #a471ff; /* 보라색 */
}

/* 학습 데이터 없음 아이콘 색상 */
.no-study-data-message .fa-book-open {
    color: #5cb85c; /* 초록색 (긍정적인 의미) */
}

.progress-labels .label.no-data-label {
    color: #a471ff;
    font-weight: 500;
    margin-left: auto; /* 오른쪽 정렬 */
    margin-right: auto;
}
</style>