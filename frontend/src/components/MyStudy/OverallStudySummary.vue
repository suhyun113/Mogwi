<template>
    <h2 class="overall-summary-title">
        <template v-if="isLoggedIn">
            <span class="username-underline">{{ username }}</span>님의 전체 학습 현황
        </template>
        <template v-else>
            전체 학습 현황
        </template>
    </h2>
    <section class="overall-summary-section">
        <div v-if="isLoggedIn && overallTotalCards > 0" class="summary-tags">
            <div class="summary-tag perfect">완벽한 기억 {{ overallPerfectCount }}개</div>
            <div class="summary-tag vague">희미한 기억 {{ overallVagueCount }}개</div>
            <div class="summary-tag forgotten">사라진 기억 {{ overallForgottenCount }}개</div>
            <div class="summary-tag total">총 학습 카드 {{ overallTotalCards }}개</div>
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
        isLoggedIn: {
            type: Boolean,
            default: false
        },
        username: {
            type: String,
            default: '사용자'
        }
    },
    setup(props) {
        const overallPerfectPercentage = computed(() =>
            props.overallTotalCards > 0 ? ((props.overallPerfectCount / props.overallTotalCards) * 100).toFixed(1) : 0
        );
        const overallVaguePercentage = computed(() =>
            props.overallTotalCards > 0 ? ((props.overallVagueCount / props.overallTotalCards) * 100).toFixed(1) : 0
        );
        const overallForgottenPercentage = computed(() =>
            props.overallTotalCards > 0 ? ((props.overallForgottenCount / props.overallTotalCards) * 100).toFixed(1) : 0
        );

        return {
            overallPerfectPercentage,
            overallVaguePercentage,
            overallForgottenPercentage,
        };
    }
};
</script>

<style scoped>
.overall-summary-section {
    width: 100%;
    max-width: 800px;
    background-color: #ffffff;
    border-radius: 10px;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    padding: 30px;
    margin-top: 0;
    margin-bottom: 32px;
    border: 2.5px solid #e0d0ff; /* Thicker border */
}

.overall-summary-title {
    color: #5a2e87;
    font-size: 1.35rem;
    font-weight: 700;
    margin-bottom: 18px;
    margin-top: 0;
    padding-bottom: 0;
    text-align: left;
    background: none;
    filter: none;
    position: static;
}
.overall-summary-title::after {
    display: none;
}
.username-underline {
    text-decoration: underline;
    text-underline-offset: 3px;
    text-decoration-color: #a471ff;
    text-decoration-thickness: 2px;
    font-weight: 800;
    background: none;
    -webkit-background-clip: unset;
    -webkit-text-fill-color: unset;
    background-clip: unset;
    text-fill-color: unset;
    padding-right: 0;
}

.summary-tags {
    display: flex;
    flex-direction: row;
    gap: 16px;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    margin-bottom: 30px !important;
}

.summary-tag {
    min-width: 80px;
    padding: 9px 15px;
    border-radius: 10px;
    font-size: 0.98rem;
    font-weight: 700;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0,0,0,0.07);
    margin: 0;
    letter-spacing: -0.5px;
}
.summary-tag.perfect {
    background: #e6f7ea;
    color: #43a047;
}
.summary-tag.vague {
    background: #fffde7;
    color: #ffc107;
}
.summary-tag.forgotten {
    background: #fff0f0;
    color: #f44336;
}
.summary-tag.total {
    background: #f3eaff;
    color: #6a1b9a;
}

.progress-bar-container {
    width: 100%;
    height: 30px;
    background-color: #e9ecef;
    border-radius: 15px;
    overflow: hidden;
    display: flex;
    margin-bottom: 0px;
    margin-top: 22px !important;
    box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.1);
}

.progress-bar {
    height: 100%;
    transition: width 0.5s ease-in-out;
    flex-shrink: 0;
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
    margin-bottom: -10px;
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
    color: #ccc;
}

.no-study-data-message .fa-user-lock {
    color: #a471ff;
}

.no-study-data-message .fa-book-open {
    color: #5cb85c;
}

.progress-labels .label.no-data-label {
    color: #a471ff;
    font-weight: 500;
    margin-left: auto;
    margin-right: auto;
}

@media (max-width: 600px) {
    .summary-tags {
        flex-direction: column;
        gap: 12px;
    }
    .summary-tag {
        min-width: 60px;
        padding: 5px 6px;
        font-size: 0.92rem;
        border-radius: 10px;
    }
}
</style>