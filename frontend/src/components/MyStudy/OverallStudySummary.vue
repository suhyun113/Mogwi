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
        <div class="summary-visual-row">
            <div class="mogwi-bubble-group">
                <img :src="mogwiImg" alt="모귀 캐릭터" class="mogwi-character-img" />
                <div class="speech-bubble" v-html="speechText"></div>
            </div>
            <div class="summary-main-content">
                <div v-if="isLoggedIn && overallTotalCards > 0" class="summary-tags">
                    <div class="summary-tag-group">
                        <div class="summary-tag perfect"><span class="tag-label">완벽한 기억</span></div>
                        <span class="tag-count perfect">{{ overallPerfectCount }}</span>
                    </div>
                    <div class="summary-tag-group">
                        <div class="summary-tag vague"><span class="tag-label">희미한 기억</span></div>
                        <span class="tag-count vague">{{ overallVagueCount }}</span>
                    </div>
                    <div class="summary-tag-group">
                        <div class="summary-tag forgotten"><span class="tag-label">사라진 기억</span></div>
                        <span class="tag-count forgotten">{{ overallForgottenCount }}</span>
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
            </div>
        </div>
    </section>
</template>

<script>
import { computed } from 'vue';
import mogwiHeart from '@/assets/mogwi-heart.png';
import mogwiSad from '@/assets/mogwi-sad.png';

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

        // 기준: 20개 이상이면 긍정, 미만이면 동기부여
        const isGood = computed(() => props.overallTotalCards >= 20);
        const mogwiImg = computed(() => isGood.value ? mogwiHeart : mogwiSad);
        const speechText = computed(() =>
            isGood.value
                ? `총 <span class='highlight-count'>${props.overallTotalCards}개</span>나 학습했어요!<br> 멋져요!`
                : `총 <span class='highlight-count'>${props.overallTotalCards}개</span>만 학습했어요...<br> 새 학습을 시작해봐요!`
        );

        return {
            overallPerfectPercentage,
            overallVaguePercentage,
            overallForgottenPercentage,
            mogwiImg,
            speechText,
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
    align-items: flex-end;
    justify-content: center;
    flex-wrap: wrap;
    margin-bottom: 40px !important;
    margin-top: 18px;
}

.summary-tag-group {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 13px;
}

.summary-tag {
    min-width: 80px;
    padding: 9px 15px 7px 15px;
    border-radius: 10px;
    font-size: 0.97rem;
    font-weight: 700;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0,0,0,0.07);
    margin: 0;
    letter-spacing: -0.5px;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
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

.tag-label {
    font-size: 0.97rem;
    font-weight: 700;
}
.tag-count {
    font-size: 1.08rem;
    font-weight: 800;
    letter-spacing: -1px;
}
.tag-count.perfect { color: #43a047; }
.tag-count.vague { color: #ffc107; }
.tag-count.forgotten { color: #f44336; }

.progress-bar-container {
    width: 88%;
    margin-left: auto;
    margin-right: auto;
    height: 30px;
    background-color: #e9ecef;
    border-radius: 15px;
    overflow: hidden;
    display: flex;
    margin-bottom: 0px;
    margin-top: 34px !important;
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

.summary-visual-row {
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    gap: 32px;
}
.mogwi-bubble-group {
    display: flex;
    flex-direction: column-reverse;
    align-items: center;
    min-width: 110px;
    margin-top: 10px;
    margin-left: 18px;
}
.mogwi-character-img {
    width: 90px;
    height: auto;
    margin-top: 8px;
    margin-bottom: 0;
    filter: drop-shadow(0 2px 8px rgba(164,113,255,0.10));
}
.speech-bubble {
    position: relative;
    background: #f3eaff;
    color: #5a2e87;
    border-radius: 18px;
    padding: 10px 18px;
    font-size: 1rem;
    font-weight: 600;
    box-shadow: 0 2px 8px 0 rgba(164,113,255,0.08);
    margin-bottom: 12px;
    margin-top: 0;
    text-align: center;
    max-width: 220px;
    z-index: 1;
}
.speech-bubble::after {
    content: '';
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    top: 100%;
    width: 0;
    height: 0;
    border: 10px solid transparent;
    border-top: 10px solid #f3eaff;
    border-bottom: 0;
}
.summary-main-content {
    flex: 1 1 0%;
    min-width: 0;
}
@media (max-width: 700px) {
    .summary-visual-row {
        flex-direction: column;
        gap: 18px;
    }
    .mogwi-bubble-group {
        flex-direction: row;
        align-items: flex-start;
        min-width: 0;
        margin-top: 0;
        margin-bottom: 8px;
    }
    .mogwi-character-img {
        width: 48px;
        margin-bottom: 0;
        margin-right: 10px;
    }
    .speech-bubble {
        font-size: 0.95rem;
        padding: 8px 12px;
        max-width: 120px;
    }
    .speech-bubble::after {
        left: 12px;
        border-width: 7px;
        border-top-width: 7px;
    }
}
@media (max-width: 500px) {
    .summary-visual-row {
        flex-direction: column;
        gap: 10px;
    }
    .mogwi-bubble-group {
        display: none;
    }
}
.highlight-count {
    color: #ff7e5f !important;
    font-weight: 900;
}
:deep(.speech-bubble .highlight-count) {
    color:rgb(127, 58, 245) !important;
    font-weight: 900;
}
</style>