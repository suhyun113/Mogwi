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
                    :key="tag.id"
                    :tagName="formatTagName(tag.tag_name)"
                    :backgroundColor="getColor(tag)" />
            </div>
            <div class="btn-wrapper">
                <template v-if="localProblem.authorId && localProblem.authorId == currentUserId">
                    <button
                        class="edit-btn"
                        @click.stop="handleEditClick"
                        :disabled="isSelectionMode || !isAuthenticated" >
                        수정
                    </button>
                </template>
                <button
                    class="solve-btn"
                    @click.stop="handleSolveClick"
                    :disabled="isSelectionMode || !isAuthenticated" >
                    {{ localProblem.studyStatus === 'completed' ? '다시 풀기' : '문제 풀기' }}
                </button>
            </div>
        </div>

        <div class="meta">
            <div class="meta-left">
                <div
                    v-if="isAuthenticated && localProblem.authorId !== currentUserId"
                    :class="['icon-wrapper', { 'disabled': isSelectionMode || !isAuthenticated }]" @click.stop="emitToggleLike"
                >
                    <img :src="localProblem.isLiked ? heartOn : heartOff" alt="like" class="icon" />
                    <span>{{ localProblem.totalLikes }}</span>
                </div>
                <div v-else class="icon-wrapper disabled"> <img :src="localProblem.isLiked ? heartOn : heartOff" alt="like" class="icon" />
                    <span>{{ localProblem.totalLikes }}</span>
                </div>

                <div
                    v-if="isAuthenticated && localProblem.authorId !== currentUserId"
                    :class="['icon-wrapper', { 'disabled': isSelectionMode || !isAuthenticated }]" @click.stop="emitToggleScrap"
                >
                    <img :src="localProblem.isScrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
                    <span>{{ localProblem.totalScraps }}</span>
                </div>
                <div v-else class="icon-wrapper disabled"> <img :src="localProblem.isScrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
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
        isAuthenticated: { // 이 prop은 MyStudy -> ProblemListSection -> ProblemListItem으로 계속 전달됩니다.
            type: Boolean,
            default: false
        },
        currentUserId: {
            type: [String, Number],
            default: ''
        },
        isSelectionMode: {
            type: Boolean,
            default: false
        },
        isSelected: {
            type: Boolean,
            default: false
        }
    },
    emits: ['auth-required', 'go-to-study', 'toggle-like', 'toggle-scrap', 'toggle-selection'],

    data() {
        return {
            // 로그인하지 않은 경우를 대비하여 problem 객체의 초기값들을 안전하게 설정
            localProblem: {
                id: this.problem.id || `default-${Math.random().toString(36).substr(2, 9)}`,
                title: this.problem.title || '문제 제목 (로그인 필요)',
                authorNickname: this.problem.authorNickname || '알 수 없음',
                studyStatus: this.problem.studyStatus || 'new',
                categories: this.problem.categories || [],
                isLiked: this.problem.isLiked || false,
                isScrapped: this.problem.isScrapped || false,
                totalLikes: this.problem.totalLikes || 0,
                totalScraps: this.problem.totalScraps || 0,
                perfectCount: this.problem.perfectCount || 0,
                vagueCount: this.problem.vagueCount || 0,
                forgottenCount: this.problem.forgottenCount || 0,
                cardCount: this.problem.cardCount || 0,
                authorId: this.problem.authorId || null,
            },
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
                'selectable': this.isSelectionMode && this.isAuthenticated, // 로그인해야 선택 가능
                'selected': this.isSelected // 선택되었을 때 빨간 테두리
            };
        }
    },
    watch: {
        problem: {
            handler(newVal) {
                // prop 'problem'이 변경될 때 localProblem 업데이트.
                // 로그인하지 않은 상태에서는 problem prop이 비어있을 수 있으므로 기본값 다시 설정.
                this.localProblem = {
                    id: newVal?.id || `default-${Math.random().toString(36).substr(2, 9)}`,
                    title: newVal?.title || '문제 제목 (로그인 필요)',
                    authorNickname: newVal?.authorNickname || '알 수 없음',
                    studyStatus: newVal?.studyStatus || 'new',
                    categories: newVal?.categories || [],
                    isLiked: !!newVal?.isLiked,
                    isScrapped: !!newVal?.isScrapped,
                    totalLikes: newVal?.totalLikes || 0,
                    totalScraps: newVal?.totalScraps || 0,
                    perfectCount: newVal?.perfectCount || 0,
                    vagueCount: newVal?.vagueCount || 0,
                    forgottenCount: newVal?.forgottenCount || 0,
                    cardCount: newVal?.cardCount || 0,
                    authorId: newVal?.authorId || null,
                };
            },
            deep: true,
            immediate: true
        },
        isAuthenticated: { // isAuthenticated prop 변화 감지
            handler(newVal) {
                // 로그인 상태가 변경될 때마다 로컬 데이터 갱신 (특히 problem이 없는 경우)
                if (!newVal) {
                    this.localProblem = {
                        id: this.problem.id || `default-${Math.random().toString(36).substr(2, 9)}`,
                        title: '문제 제목 (로그인 필요)',
                        authorNickname: '알 수 없음',
                        studyStatus: 'new',
                        categories: [],
                        isLiked: false,
                        isScrapped: false,
                        totalLikes: 0,
                        totalScraps: 0,
                        perfectCount: 0,
                        vagueCount: 0,
                        forgottenCount: 0,
                        cardCount: 0,
                        authorId: null,
                    };
                } else {
                    // 로그인 시에는 problem prop의 실제 데이터를 사용하도록 강제
                    this.localProblem = {
                        ...this.problem,
                        isLiked: !!this.problem.isLiked,
                        isScrapped: !!this.problem.isScrapped,
                    };
                }
            },
            immediate: true
        }
    },
    methods: {
        formatTagName(tagName) {
            // Remove existing '#' if any, then prepend a single '#'
            const cleanedTagName = tagName.startsWith('#') ? tagName.substring(1) : tagName;
            return `#${cleanedTagName}`;
        },
        getColor(tag) {
            return tag.color_code || '#ccc';
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
            if (!this.isAuthenticated) { // 로그인 안 했으면 클릭해도 선택 안 되게
                this.$emit('auth-required');
                return;
            }
            if (this.isSelectionMode) {
                this.$emit('toggle-selection', this.localProblem.id);
            }
        },
        emitToggleLike() {
            if (!this.isAuthenticated) {
                this.$emit('auth-required');
                return;
            }
            if (this.localProblem.authorId === this.currentUserId) {
                alert("본인이 작성한 문제는 좋아요/스크랩할 수 없습니다.");
                return;
            }
            if (!this.isSelectionMode) {
                this.$emit('toggle-like', this.localProblem.id);
            }
        },
        emitToggleScrap() {
            if (!this.isAuthenticated) {
                this.$emit('auth-required');
                return;
            }
            if (this.localProblem.authorId === this.currentUserId) {
                alert("본인이 작성한 문제는 좋아요/스크랩할 수 없습니다.");
                return;
            }
            if (!this.isSelectionMode) {
                this.$emit('toggle-scrap', this.localProblem.id);
            }
        },
        handleEditClick() {
            if (!this.isAuthenticated) {
                this.$emit('auth-required');
                return;
            }
            if (!this.isSelectionMode) {
                this.$router.push(`/edit/${this.localProblem.id}`);
            }
        },
        handleSolveClick() {
            if (!this.isAuthenticated) {
                this.$emit('auth-required');
                return;
            }
            if (!this.isSelectionMode) {
                this.$emit('go-to-study', this.localProblem.id);
            }
        }
    }
}
</script>

<style scoped>
/* 기존 스타일 유지 */
.problem-list-item {
    padding: 16px;
    border: 1px solid #e0d0ff;
    border-radius: 8px;
    background: #ffffff;
    width: 100%;
    box-sizing: border-box;
    margin-bottom: 15px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.problem-list-item:hover:not(.selectable) {
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

/* 삭제 모드일 때 (그리고 로그인했을 때) 스타일 */
.problem-list-item.selectable {
    cursor: pointer;
    border-color: #a471ff;
}

/* 선택된 문제 스타일 (빨간색 테두리) */
.problem-list-item.selected {
    border: 2px solid #e03c3c;
    box-shadow: 0 0 0 3px rgba(224, 60, 60, 0.3);
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
.icon-wrapper:hover:not(.disabled) {
    transform: translateY(-2px);
}
.icon-wrapper.disabled {
    opacity: 0.5;
    cursor: not-allowed;
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
    transition: background-color 0.2s ease, transform 0.1s ease, opacity 0.2s ease;
}
.edit-btn {
    background-color: #fff4c1;
    color: #343a40;
}
.edit-btn:hover:not(:disabled) {
    background-color: #ffe066;
    transform: translateY(-1px);
}
.edit-btn:disabled, .solve-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    background-color: #e0e0e0;
    color: #a0a0a0;
}
.solve-btn {
    background-color: #a471ff;
    color: white;
}
.solve-btn:hover:not(:disabled) {
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