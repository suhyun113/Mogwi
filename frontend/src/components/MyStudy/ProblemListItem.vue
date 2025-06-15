<template>
    <div :class="itemClass" @click="handleItemClick">
        <div class="title-row">
            <div class="title-left">
                <h3 class="problem-title">{{ localProblem.title }}</h3>
                <span class="author">작성자: {{ localProblem.authorNickname }}</span>
            </div>
            <div class="study-status">
                <span :class="getStatusClass(localProblem.studyStatus)">
                    {{ getStatusText(localProblem.studyStatus) }}
                </span>
            </div>
        </div>

        <div class="category-row">
            <div class="category-tags">
                <ProblemTag
                    v-for="tag in localProblem.categories"
                    :key="tag"
                    :tagName="tag"
                    :backgroundColor="getColor(tag)" />
            </div>
            <div class="btn-wrapper">
                <template v-if="isAuthenticated && localProblem.authorId == currentUserId">
                    <button
                        class="edit-btn"
                        @click.stop="handleEditClick"
                        :disabled="isSelectionMode"
                    >
                        수정
                    </button>
                </template>
                <button
                    class="solve-btn"
                    @click.stop="handleSolveClick"
                    :disabled="isSelectionMode"
                >
                    {{ localProblem.studyStatus === 'completed' ? '다시 풀기' : '문제 풀기' }}
                </button>
            </div>
        </div>

        <div class="meta">
            <div class="meta-left">
                <div
                    v-if="isAuthenticated && localProblem.authorId !== currentUserId"
                    :class="['icon-wrapper', { 'disabled': isSelectionMode }]"
                    @click.stop="emitToggleLike"
                >
                    <img :src="localProblem.isLiked ? heartOn : heartOff" alt="like" class="icon" />
                    <span>{{ localProblem.totalLikes }}</span>
                </div>
                <div v-else class="icon-wrapper disabled">
                    <img :src="localProblem.isLiked ? heartOn : heartOff" alt="like" class="icon" />
                    <span>{{ localProblem.totalLikes }}</span>
                </div>

                <div
                    v-if="isAuthenticated && localProblem.authorId !== currentUserId"
                    :class="['icon-wrapper', { 'disabled': isSelectionMode }]"
                    @click.stop="emitToggleScrap"
                >
                    <img :src="localProblem.isScrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
                    <span>{{ localProblem.totalScraps }}</span>
                </div>
                <div v-else class="icon-wrapper disabled">
                    <img :src="localProblem.isScrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
                    <span>{{ localProblem.totalScraps }}</span>
                </div>
            </div>

            <div class="meta-right-group">
                <div class="study-card-summary" v-if="localProblem.studyStatus !== 'new'">
                    <span class="perfect-count" :title="`${localProblem.perfectCount}개 - 완벽한 기억`">{{ localProblem.perfectCount }}</span> /
                    <span class="vague-count" :title="`${localProblem.vagueCount}개 - 희미한 기억`">{{ localProblem.vagueCount }}</span> /
                    <span class="forgotten-count" :title="`${localProblem.forgottenCount}개 - 사라진 기억`">{{ localProblem.forgottenCount }}</span>
                </div>
                <div class="card-count meta-card-count">
                    <img :src="cardIcon" alt="card icon" class="icon card-icon" />
                    {{ localProblem.cardCount }} 카드
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import heartOff from '@/assets/icons/like_outline.png'
import heartOn from '@/assets/icons/like_filled.png'
import scrapOff from '@/assets/icons/scrap_outline.png'
import scrapOn from '@/assets/icons/scrap_filled.png'
import cardIcon from '@/assets/icons/card.png'
import ProblemTag from './ProblemTag.vue'

export default {
    name: 'ProblemListItem',
    components: {
        ProblemTag
    },
    props: {
        problem: {
            type: Object,
            required: true
        },
        isAuthenticated: {
            type: Boolean,
            default: false
        },
        currentUserId: {
            type: [String, Number],
            default: ''
        },
        isSelectionMode: { // 새로 추가된 prop
            type: Boolean,
            default: false
        },
        isSelected: { // 새로 추가된 prop
            type: Boolean,
            default: false
        }
    },
    emits: ['auth-required', 'go-to-study', 'toggle-like', 'toggle-scrap', 'toggle-selection'], // 새 이벤트 추가

    data() {
        return {
            localProblem: { ...this.problem },
            heartOff,
            heartOn,
            scrapOff,
            scrapOn,
            cardIcon
        }
    },
    computed: {
        itemClass() {
            return {
                'problem-list-item': true,
                'selectable': this.isSelectionMode, // 삭제 모드일 때만 선택 가능하도록 cursor 변경
                'selected': this.isSelected // 선택되었을 때 빨간 테두리
            };
        }
    },
    watch: {
        problem: {
            handler(newVal) {
                this.localProblem = { ...newVal };
                this.localProblem.isLiked = !!newVal.isLiked;
                this.localProblem.isScrapped = !!newVal.isScrapped;
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        getColor(tag) {
            const trimmedTag = tag ? tag.trim() : '';
            const colors = {
                '#수학': '#ffd54f',
                '#AI': '#81c784',
                '#컴퓨터': '#64b5f6',
                '#과학': '#4dd0e1',
                '#역사': '#a1887f',
                '#기타': '#e0e0e0',
                '#프론트엔드': '#ba68c8',
                '#자료구조': '#f06292',
                '#전체': '#b0bec5'
            }
            return colors[trimmedTag] || '#ccc'
        },
        getStatusText(status) {
            switch (status) {
                case 'new':
                    return '진행 전';
                case 'ongoing':
                    return '진행 중';
                case 'completed':
                    return '완료';
                default:
                    return '';
            }
        },
        getStatusClass(status) {
            switch (status) {
                case 'new':
                    return 'status-new';
                case 'ongoing':
                    return 'status-ongoing';
                case 'completed':
                    return 'status-completed';
                default:
                    return '';
            }
        },
        handleItemClick() {
            if (this.isSelectionMode) {
                this.$emit('toggle-selection', this.localProblem.id); // 삭제 모드일 때만 선택 토글 이벤트 발생
            }
            // 그 외의 일반적인 클릭 동작 (예: 상세 페이지 이동)은 여기서 직접 처리하지 않음
            // 필요하다면, 여기에서 this.$router.push('/problem/${this.localProblem.id}') 같은 로직 추가 가능
        },
        emitToggleLike() {
            if (this.localProblem.authorId === this.currentUserId) {
                alert("본인이 작성한 문제는 좋아요/스크랩할 수 없습니다.");
                return;
            }
            // 삭제 모드일 때는 좋아요/스크랩 기능 비활성화
            if (!this.isSelectionMode) {
                this.$emit('toggle-like', this.localProblem.id);
            }
        },
        emitToggleScrap() {
            if (this.localProblem.authorId === this.currentUserId) {
                alert("본인이 작성한 문제는 좋아요/스크랩할 수 없습니다.");
                return;
            }
            // 삭제 모드일 때는 좋아요/스크랩 기능 비활성화
            if (!this.isSelectionMode) {
                this.$emit('toggle-scrap', this.localProblem.id);
            }
        },
        handleEditClick() {
            // 삭제 모드일 때는 수정 버튼 클릭을 막음
            if (!this.isSelectionMode) {
                this.$router.push(`/edit/${this.localProblem.id}`);
            }
        },
        handleSolveClick() {
            // 삭제 모드일 때는 풀이 버튼 클릭을 막음
            if (!this.isSelectionMode) {
                if (this.isAuthenticated) {
                    this.$emit('go-to-study', this.localProblem.id); // ID만 전달
                } else {
                    this.$emit('auth-required');
                }
            }
        }
    }
}
</script>

<style scoped>
.problem-list-item {
    padding: 16px;
    border: 1px solid #e0d0ff;
    border-radius: 8px;
    background: #ffffff;
    width: 100%;
    box-sizing: border-box;
    margin-bottom: 15px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease; /* border-color 추가 */
}

.problem-list-item:hover:not(.selectable) { /* 삭제 모드 아닐 때만 hover 효과 적용 */
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

/* 삭제 모드일 때 스타일 */
.problem-list-item.selectable {
    cursor: pointer; /* 선택 가능함을 알리는 커서 */
    border-color: #a471ff; /* 선택 가능 테두리 색상 */
}

/* 선택된 문제 스타일 (빨간색 테두리) */
.problem-list-item.selected {
    border: 2px solid #e03c3c; /* 빨간색 테두리 */
    box-shadow: 0 0 0 3px rgba(224, 60, 60, 0.3); /* 빨간색 하이라이트 */
}

.problem-list-item h3 {
    margin: 0;
    font-size: 18px;
    color: #5a2e87;
}
.title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    flex-wrap: wrap;
    gap: 8px;
}
.title-left {
    display: flex;
    align-items: baseline;
    gap: 12px;
}
.problem-title {
    font-size: 18px;
    font-weight: bold;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 70%;
}
.author {
    font-size: 13px;
    color: #888;
    white-space: nowrap;
    flex-shrink: 0;
}

.study-status {
    font-size: 13px;
    font-weight: bold;
    padding: 4px 8px;
    border-radius: 5px;
    white-space: nowrap;
    flex-shrink: 0;
}

.status-new {
    background-color: #e0e0e0;
    color: #616161;
}

.status-ongoing {
    background-color: #ffe082;
    color: #c66900;
}

.status-completed {
    background-color: #a5d6a7;
    color: #2e7d32;
}


.category-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    flex-wrap: wrap;
    gap: 8px;
}
.category-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    flex-grow: 1;
}

.meta {
    margin-top: 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    color: #666;
    flex-wrap: wrap;
}
.meta-left {
    display: flex;
    gap: 16px;
}
.icon-wrapper {
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    transition: transform 0.2s ease;
}
.icon-wrapper:hover:not(.disabled) { /* disabled 상태일 땐 hover 효과 없음 */
    transform: translateY(-2px);
}
.icon-wrapper.disabled {
    opacity: 0.5;
    cursor: not-allowed;
    /* pointer-events: none; 제거: 버튼 disabled 상태에 따라 자동으로 처리되도록 */
}
.icon {
    width: 20px;
    height: 20px;
}
.card-icon {
    width: 18px;
    height: 18px;
}
.btn-wrapper {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
}
.edit-btn, .solve-btn {
    padding: 6px 12px;
    border: none;
    border-radius: 6px;
    font-weight: bold;
    cursor: pointer;
    white-space: nowrap;
    font-size: 14px;
    transition: background-color 0.2s ease, transform 0.1s ease, opacity 0.2s ease; /* opacity 추가 */
}
.edit-btn {
    background-color: #fff4c1;
    color: #343a40;
}
.edit-btn:hover:not(:disabled) { /* disabled 상태일 땐 hover 효과 없음 */
    background-color: #ffe066;
    transform: translateY(-1px);
}
.edit-btn:disabled, .solve-btn:disabled {
    opacity: 0.6; /* 비활성화 상태에서 투명도 적용 */
    cursor: not-allowed;
    background-color: #e0e0e0; /* 비활성화 상태 배경색 */
    color: #a0a0a0; /* 비활성화 상태 텍스트 색상 */
}
.solve-btn {
    background-color: #a471ff;
    color: white;
}
.solve-btn:hover:not(:disabled) { /* disabled 상태일 땐 hover 효과 없음 */
    background-color: #854fe6;
    transform: translateY(-1px);
}

.study-card-summary {
    display: flex;
    align-items: center;
    gap: 4px;
    font-weight: bold;
    white-space: nowrap;
}

.perfect-count {
    color: #4CAF50;
}

.vague-count {
    color: #FFC107;
}

.forgotten-count {
    color: #F44336;
}

.meta-card-count {
    font-size: 13px;
    color: #666;
    display: flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;
    flex-shrink: 0;
}

.meta-right-group {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-left: auto;
    flex-wrap: wrap;
}

@media (max-width: 576px) {
    .title-row {
        flex-direction: column;
        align-items: flex-start;
        gap: 4px;
    }
    .problem-title {
        max-width: 100%;
    }
    .study-status {
        align-self: flex-end;
        margin-top: 4px;
    }
    .category-row {
        flex-direction: column;
        align-items: flex-start;
        gap: 10px;
    }
    .btn-wrapper {
        width: 100%;
        justify-content: flex-end;
    }
    .edit-btn, .solve-btn {
        flex-grow: 1;
    }
    .meta {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
    }
    .meta-right-group {
        margin-left: 0;
        width: 100%;
        justify-content: flex-end;
    }
    .study-card-summary {
        margin-left: 0;
    }
}
</style>